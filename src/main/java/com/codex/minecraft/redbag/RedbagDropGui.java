package com.codex.minecraft.redbag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class RedbagDropGui implements Listener {
    private final RedbagPlugin plugin;
    private final Map<UUID, PendingRedbag> pending = new HashMap<UUID, PendingRedbag>();
    private final Map<UUID, Long> recentClaims = new HashMap<UUID, Long>();

    RedbagDropGui(RedbagPlugin plugin) {
        this.plugin = plugin;
    }

    void open(Player player, double total, int count, String message) {
        pending.put(player.getUniqueId(), new PendingRedbag(total, count, message, System.currentTimeMillis()));
        RedbagGuiHolder holder = new RedbagGuiHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, getSize(), plugin.color(plugin.getConfig().getString("drop-gui.title", "&c选择领取物品")));
        holder.setInventory(inventory);
        for (RedbagDropItem dropItem : getDropItems()) {
            if (dropItem.getSlot() < 0 || dropItem.getSlot() >= inventory.getSize()) {
                continue;
            }
            ItemStack stack = new ItemStack(dropItem.getMaterial(), 1);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.color(plugin.getConfig().getString("drop-gui.item-name", "&e{item}")
                        .replace("{item}", dropItem.getDisplayName())));
                List<String> lore = new ArrayList<String>();
                for (String line : plugin.getConfig().getStringList("drop-gui.item-lore")) {
                    lore.add(plugin.color(line
                            .replace("{item}", dropItem.getDisplayName())
                            .replace("{total}", Money.format(total))
                            .replace("{count}", String.valueOf(count))));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inventory.setItem(dropItem.getSlot(), stack);
        }
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!isRedbagGui(event.getInventory(), player)) {
            return;
        }
        event.setCancelled(true);
        RedbagDropItem selected = getDropItemBySlot(event.getRawSlot());
        if (selected == null) {
            return;
        }
        PendingRedbag request = pending.remove(player.getUniqueId());
        if (request == null || request.isExpired(plugin.getConfig().getLong("settings.gui-pending-seconds", 60L) * 1000L)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("gui-expired"));
            return;
        }
        RedbagService.CreateResult result = plugin.getRedbagService().create(player, request.getTotal(), request.getCount(), request.getMessage(), "", selected.getMaterialName());
        if (!result.isSuccess()) {
            player.closeInventory();
            player.sendMessage(plugin.msg(result.getMessageKey()).replace("{total}", Money.format(request.getTotal())));
            return;
        }
        Redbag redbag = result.getRedbag();
        player.closeInventory();
        player.sendMessage(plugin.msg("created")
                .replace("{id}", String.valueOf(redbag.getId()))
                .replace("{total}", Money.format(redbag.getTotal()))
                .replace("{count}", String.valueOf(redbag.getCount())));
        player.sendMessage(plugin.msg("drop-created-tip")
                .replace("{claim-item}", selected.getDisplayName())
                .replace("{expire}", String.valueOf(plugin.getConfig().getLong("settings.expire-minutes", 10L))));
        plugin.getRedbagService().broadcastCreated(redbag);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (isRedbagGui(event.getInventory(), (Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (claimByHeldItem(event.getPlayer(), event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        claimByHeldItem(event.getPlayer(), event.getPlayer().getItemInHand());
    }

    private boolean claimByHeldItem(Player player, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long lastClaim = recentClaims.get(player.getUniqueId());
        if (lastClaim != null && now - lastClaim.longValue() < 250L) {
            return false;
        }
        if (!plugin.getRedbagService().hasClaimableDropItem(player, stack.getType().name())) {
            return false;
        }
        RedbagService.ClaimResult result = plugin.getRedbagService().claimByItemUse(player, stack.getType().name());
        if (result.isSuccess()) {
            recentClaims.put(player.getUniqueId(), now);
            player.sendMessage(plugin.msg("grabbed")
                    .replace("{id}", String.valueOf(result.getRedbag().getId()))
                    .replace("{amount}", Money.format(result.getClaim().getAmount())));
            return true;
        } else if (!"no-item-redbag".equals(result.getMessageKey())) {
            recentClaims.put(player.getUniqueId(), now);
            player.sendMessage(plugin.msg(result.getMessageKey()));
            return true;
        }
        return false;
    }

    private int getSize() {
        int size = plugin.getConfig().getInt("drop-gui.size", 27);
        if (size < 9) {
            size = 9;
        }
        if (size > 54) {
            size = 54;
        }
        return ((size + 8) / 9) * 9;
    }

    private boolean isRedbagGui(Inventory inventory, Player player) {
        if (inventory == null || !(inventory.getHolder() instanceof RedbagGuiHolder)) {
            return false;
        }
        RedbagGuiHolder holder = (RedbagGuiHolder) inventory.getHolder();
        return holder.getPlayerId().equals(player.getUniqueId());
    }

    private RedbagDropItem getDropItemBySlot(int slot) {
        for (RedbagDropItem dropItem : getDropItems()) {
            if (dropItem.getSlot() == slot) {
                return dropItem;
            }
        }
        return null;
    }

    private List<RedbagDropItem> getDropItems() {
        List<RedbagDropItem> result = new ArrayList<RedbagDropItem>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("drop-items");
        if (section == null) {
            return result;
        }
        int fallbackSlot = 0;
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) {
                itemSection = new org.bukkit.configuration.MemoryConfiguration();
                itemSection.set("material", key);
                itemSection.set("name", key);
            }
            RedbagDropItem item = RedbagDropItem.fromConfig(key, itemSection, fallbackSlot);
            fallbackSlot++;
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    Map<String, String> getDropItemNames() {
        Map<String, String> names = new LinkedHashMap<String, String>();
        for (RedbagDropItem item : getDropItems()) {
            names.put(item.getMaterialName(), item.getDisplayName());
        }
        return names;
    }
}
