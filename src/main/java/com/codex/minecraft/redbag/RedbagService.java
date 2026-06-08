package com.codex.minecraft.redbag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

final class RedbagService {
    private final RedbagPlugin plugin;
    private final RedbagStorage storage;
    private final EconomyBridge economy;
    private Map<Integer, Redbag> redbags = new LinkedHashMap<Integer, Redbag>();
    private int nextId = 1;

    RedbagService(RedbagPlugin plugin, RedbagStorage storage, EconomyBridge economy) {
        this.plugin = plugin;
        this.storage = storage;
        this.economy = economy;
    }

    void replaceRedbagsForTest(int nextId, Map<Integer, Redbag> redbags) {
        this.nextId = nextId;
        this.redbags = new LinkedHashMap<Integer, Redbag>(redbags);
    }

    void load() {
        RedbagStorage.LoadedRedbags loaded = storage.load();
        this.nextId = loaded.getNextId();
        this.redbags = loaded.getRedbags();
        refundExpiredRedbags();
    }

    void save() {
        storage.save(nextId, redbags);
    }

    CreateResult create(Player player, double total, int count, String message) {
        return create(player, total, count, message, "");
    }

    CreateResult create(Player player, double total, int count, String message, String passphrase) {
        if (!economy.isReady()) {
            return CreateResult.error("economy-missing");
        }
        total = Money.round(total);
        double minTotal = plugin.getConfig().getDouble("settings.min-total", 1D);
        double maxTotal = plugin.getConfig().getDouble("settings.max-total", 1000000D);
        int minCount = plugin.getConfig().getInt("settings.min-count", 1);
        int maxCount = plugin.getConfig().getInt("settings.max-count", 100);
        int maxPassphraseLength = plugin.getConfig().getInt("settings.max-passphrase-length", 16);
        if (total < minTotal || total > maxTotal || count < minCount || count > maxCount || total < count * 0.01D) {
            return CreateResult.error("invalid-usage");
        }
        if (passphrase != null && passphrase.trim().length() > maxPassphraseLength) {
            return CreateResult.error("invalid-passphrase");
        }
        if (!economy.has(player, total)) {
            return CreateResult.error("insufficient-money");
        }
        if (!economy.withdraw(player, total)) {
            return CreateResult.error("insufficient-money");
        }

        Redbag redbag = new Redbag(nextId++, player.getUniqueId(), player.getName(), total, count, message, passphrase, System.currentTimeMillis(), total, Collections.<java.util.UUID, Claim>emptyMap());
        redbags.put(redbag.getId(), redbag);
        save();
        return CreateResult.success(redbag);
    }

    ClaimResult claim(Player player, int id) {
        return claim(player, id, "");
    }

    ClaimResult claim(Player player, int id, String passphraseAnswer) {
        Redbag redbag = redbags.get(id);
        if (redbag == null) {
            return ClaimResult.error("not-found");
        }
        if (isExpired(redbag)) {
            refund(redbag);
            save();
            return ClaimResult.error("closed");
        }
        if (redbag.isFinished()) {
            return ClaimResult.error("closed");
        }
        if (redbag.hasClaimed(player.getUniqueId())) {
            return ClaimResult.error("already-grabbed");
        }
        if (!redbag.matchesPassphrase(passphraseAnswer)) {
            return ClaimResult.error("wrong-passphrase");
        }

        double amount = Money.nextShare(redbag.getRemaining(), redbag.getRemainingCount(), plugin.getConfig().getBoolean("settings.random-amounts", true));
        if (!economy.deposit(player, amount)) {
            return ClaimResult.error("economy-missing");
        }
        Claim claim = redbag.addClaim(player.getUniqueId(), player.getName(), amount, System.currentTimeMillis());
        save();
        if (redbag.isFinished()) {
            broadcastLuckyKing(redbag);
        }
        return ClaimResult.success(redbag, claim);
    }

    ClaimResult claimByPassphrase(Player player, String passphraseAnswer) {
        String answer = Redbag.cleanPassphrase(passphraseAnswer);
        if (answer.length() == 0) {
            return ClaimResult.error("wrong-passphrase");
        }
        Redbag found = null;
        for (Redbag redbag : getOpenRedbags()) {
            if (redbag.hasPassphrase() && redbag.matchesPassphrase(answer) && !redbag.hasClaimed(player.getUniqueId())) {
                if (found != null) {
                    return ClaimResult.error("ambiguous-passphrase");
                }
                found = redbag;
            }
        }
        if (found == null) {
            return ClaimResult.error("wrong-passphrase");
        }
        return claim(player, found.getId(), answer);
    }

    boolean hasClaimablePassphrase(Player player, String passphraseAnswer) {
        String answer = Redbag.cleanPassphrase(passphraseAnswer);
        if (answer.length() == 0) {
            return false;
        }
        for (Redbag redbag : getOpenRedbags()) {
            if (redbag.hasPassphrase() && redbag.matchesPassphrase(answer) && !redbag.hasClaimed(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    Redbag get(int id) {
        Redbag redbag = redbags.get(id);
        if (redbag != null && isExpired(redbag)) {
            refund(redbag);
            save();
        }
        return redbag;
    }

    List<Redbag> getOpenRedbags() {
        refundExpiredRedbags();
        List<Redbag> open = new ArrayList<Redbag>();
        for (Redbag redbag : redbags.values()) {
            if (!redbag.isFinished() && !isExpired(redbag)) {
                open.add(redbag);
            }
        }
        return open;
    }

    boolean isExpired(Redbag redbag) {
        long expireMillis = plugin.getConfig().getLong("settings.expire-minutes", 60L) * 60L * 1000L;
        return System.currentTimeMillis() - redbag.getCreatedAt() > expireMillis;
    }

    void broadcastCreated(Redbag redbag) {
        if (!plugin.getConfig().getBoolean("settings.broadcast-created", true)) {
            return;
        }
        String messageKey = redbag.hasPassphrase() ? "created-code-broadcast" : "created-broadcast";
        String text = plugin.msg(messageKey)
                .replace("{player}", redbag.getOwnerName())
                .replace("{id}", String.valueOf(redbag.getId()))
                .replace("{passphrase}", redbag.getPassphrase())
                .replace("{message}", redbag.getMessage());
        String hover = plugin.msg("click-hover").replace("{id}", String.valueOf(redbag.getId()));
        String command = "/redbag grab " + redbag.getId();
        if (redbag.hasPassphrase()) {
            command = command + " " + redbag.getPassphrase();
        }
        if (!sendClickableBroadcast(text, command, hover)) {
            Bukkit.broadcastMessage(text);
        }
        sendTitle(redbag);
    }

    void broadcastLuckyKing(Redbag redbag) {
        if (!plugin.getConfig().getBoolean("settings.broadcast-lucky-king", true)) {
            return;
        }
        Claim best = redbag.getBestClaim();
        if (best == null) {
            return;
        }
        String text = plugin.msg("lucky-king-broadcast")
                .replace("{id}", String.valueOf(redbag.getId()))
                .replace("{player}", best.getPlayerName())
                .replace("{amount}", Money.format(best.getAmount()));
        Bukkit.broadcastMessage(text);
    }

    private void refundExpiredRedbags() {
        boolean changed = false;
        for (Redbag redbag : redbags.values()) {
            if (isExpired(redbag) && refund(redbag)) {
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    private boolean refund(Redbag redbag) {
        if (redbag.getRemaining() <= 0.0001D || !economy.isReady()) {
            return false;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(redbag.getOwnerId());
        double amount = redbag.getRemaining();
        if (economy.deposit(owner, amount)) {
            redbag.refundRemaining();
            return true;
        }
        return false;
    }

    private boolean sendClickableBroadcast(String text, String command, String hover) {
        try {
            Class<?> componentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> clickEventClass = Class.forName("net.md_5.bungee.api.chat.ClickEvent");
            Class<?> clickActionClass = Class.forName("net.md_5.bungee.api.chat.ClickEvent$Action");
            Class<?> hoverEventClass = Class.forName("net.md_5.bungee.api.chat.HoverEvent");
            Class<?> hoverActionClass = Class.forName("net.md_5.bungee.api.chat.HoverEvent$Action");
            Object component = componentClass.getConstructor(String.class).newInstance(text);
            Object clickAction = enumValue(clickActionClass, "RUN_COMMAND");
            Object clickEvent = clickEventClass.getConstructor(clickActionClass, String.class).newInstance(clickAction, command);
            componentClass.getMethod("setClickEvent", clickEventClass).invoke(component, clickEvent);
            Object hoverAction = enumValue(hoverActionClass, "SHOW_TEXT");
            Object hoverText = componentClass.getConstructor(String.class).newInstance(hover);
            Object hoverArray = java.lang.reflect.Array.newInstance(baseComponentClass, 1);
            java.lang.reflect.Array.set(hoverArray, 0, hoverText);
            Object hoverEvent = hoverEventClass.getConstructor(hoverActionClass, hoverArray.getClass()).newInstance(hoverAction, hoverArray);
            componentClass.getMethod("setHoverEvent", hoverEventClass).invoke(component, hoverEvent);
            Object messageArray = java.lang.reflect.Array.newInstance(baseComponentClass, 1);
            java.lang.reflect.Array.set(messageArray, 0, component);
            for (Player player : Bukkit.getOnlinePlayers()) {
                Object spigot = player.getClass().getMethod("spigot").invoke(player);
                spigot.getClass().getMethod("sendMessage", messageArray.getClass()).invoke(spigot, messageArray);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Object enumValue(Class<?> enumClass, String name) {
        Object[] constants = enumClass.getEnumConstants();
        for (Object constant : constants) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(name);
    }

    private void sendTitle(Redbag redbag) {
        String title = plugin.color(plugin.getConfig().getString("messages.title-main", "&c&l红包来了"));
        String subtitle = plugin.color(plugin.getConfig().getString("messages.title-subtitle", "&e{player} 发了一个红包，点击聊天消息领取"))
                .replace("{player}", redbag.getOwnerName())
                .replace("{id}", String.valueOf(redbag.getId()))
                .replace("{message}", redbag.getMessage())
                .replace("{passphrase}", redbag.getPassphrase());
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class)
                        .invoke(player, title, subtitle, 10, 50, 10);
            } catch (Throwable ignored) {
                player.sendMessage(subtitle);
            }
        }
    }

    static final class CreateResult {
        private final boolean success;
        private final String messageKey;
        private final Redbag redbag;

        private CreateResult(boolean success, String messageKey, Redbag redbag) {
            this.success = success;
            this.messageKey = messageKey;
            this.redbag = redbag;
        }

        static CreateResult success(Redbag redbag) {
            return new CreateResult(true, null, redbag);
        }

        static CreateResult error(String messageKey) {
            return new CreateResult(false, messageKey, null);
        }

        boolean isSuccess() {
            return success;
        }

        String getMessageKey() {
            return messageKey;
        }

        Redbag getRedbag() {
            return redbag;
        }
    }

    static final class ClaimResult {
        private final boolean success;
        private final String messageKey;
        private final Redbag redbag;
        private final Claim claim;

        private ClaimResult(boolean success, String messageKey, Redbag redbag, Claim claim) {
            this.success = success;
            this.messageKey = messageKey;
            this.redbag = redbag;
            this.claim = claim;
        }

        static ClaimResult success(Redbag redbag, Claim claim) {
            return new ClaimResult(true, null, redbag, claim);
        }

        static ClaimResult error(String messageKey) {
            return new ClaimResult(false, messageKey, null, null);
        }

        boolean isSuccess() {
            return success;
        }

        String getMessageKey() {
            return messageKey;
        }

        Redbag getRedbag() {
            return redbag;
        }

        Claim getClaim() {
            return claim;
        }
    }
}
