package com.codex.minecraft.redbag;

import java.lang.reflect.Method;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

final class EconomyBridge {
    private final RedbagPlugin plugin;
    private Object economy;
    private Method has;
    private Method withdrawPlayer;
    private Method depositPlayer;
    private boolean warned;

    EconomyBridge(RedbagPlugin plugin) {
        this.plugin = plugin;
        refresh();
    }

    boolean isReady() {
        if (economy == null) {
            refresh();
        }
        return economy != null && has != null && withdrawPlayer != null && depositPlayer != null;
    }

    boolean has(Player player, double amount) {
        if (!isReady()) {
            return false;
        }
        try {
            return (Boolean) has.invoke(economy, player, amount);
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean withdraw(Player player, double amount) {
        return callTransaction(withdrawPlayer, player, amount);
    }

    boolean deposit(OfflinePlayer player, double amount) {
        return callTransaction(depositPlayer, player, amount);
    }

    private boolean callTransaction(Method method, OfflinePlayer player, double amount) {
        if (!isReady()) {
            return false;
        }
        try {
            Object response = method.invoke(economy, player, amount);
            Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
            return (Boolean) transactionSuccess.invoke(response);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void refresh() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = plugin.getServer().getServicesManager().getRegistration(economyClass);
            if (provider == null) {
                return;
            }
            Object service = provider.getProvider();
            this.has = service.getClass().getMethod("has", OfflinePlayer.class, double.class);
            this.withdrawPlayer = service.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            this.depositPlayer = service.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
            this.economy = service;
        } catch (Exception ex) {
            if (!warned) {
                plugin.getLogger().warning("Vault economy is not available: " + ex.getMessage());
                warned = true;
            }
        }
    }
}
