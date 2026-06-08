package com.codex.minecraft.redbag;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

final class RedbagChatListener implements Listener {
    private final RedbagPlugin plugin;

    RedbagChatListener(RedbagPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final String message = event.getMessage();
        if (Redbag.cleanPassphrase(message).length() == 0) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!plugin.getRedbagService().hasClaimablePassphrase(player, message)) {
                    return;
                }
                RedbagService.ClaimResult result = plugin.getRedbagService().claimByPassphrase(player, message);
                if (result.isSuccess()) {
                    player.sendMessage(plugin.msg("grabbed")
                            .replace("{id}", String.valueOf(result.getRedbag().getId()))
                            .replace("{amount}", Money.format(result.getClaim().getAmount())));
                } else if (!"wrong-passphrase".equals(result.getMessageKey())) {
                    player.sendMessage(plugin.msg(result.getMessageKey()));
                }
            }
        });
    }
}
