package com.codex.minecraft.redbag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

final class RedbagStorage {
    private final RedbagPlugin plugin;
    private final File file;

    RedbagStorage(RedbagPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "redbags.yml");
    }

    LoadedRedbags load() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        int nextId = config.getInt("next-id", 1);
        Map<Integer, Redbag> redbags = new LinkedHashMap<Integer, Redbag>();
        ConfigurationSection section = config.getConfigurationSection("redbags");
        if (section == null) {
            return new LoadedRedbags(nextId, redbags);
        }

        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection node = section.getConfigurationSection(key);
                if (node == null) {
                    continue;
                }
                int id = Integer.parseInt(key);
                UUID ownerId = UUID.fromString(node.getString("owner-id"));
                String ownerName = node.getString("owner-name", "Unknown");
                double total = node.getDouble("total");
                int count = node.getInt("count");
                String message = node.getString("message", "恭喜发财");
                String passphrase = node.getString("passphrase", "");
                long createdAt = node.getLong("created-at");
                double remaining = node.getDouble("remaining");
                Map<UUID, Claim> claims = loadClaims(node.getConfigurationSection("claims"));
                redbags.put(id, new Redbag(id, ownerId, ownerName, total, count, message, passphrase, createdAt, remaining, claims));
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load redbag " + key + ": " + ex.getMessage());
            }
        }
        return new LoadedRedbags(nextId, redbags);
    }

    void save(int nextId, Map<Integer, Redbag> redbags) {
        FileConfiguration config = new YamlConfiguration();
        config.set("next-id", nextId);
        for (Redbag redbag : redbags.values()) {
            String base = "redbags." + redbag.getId() + ".";
            config.set(base + "owner-id", redbag.getOwnerId().toString());
            config.set(base + "owner-name", redbag.getOwnerName());
            config.set(base + "total", redbag.getTotal());
            config.set(base + "count", redbag.getCount());
            config.set(base + "message", redbag.getMessage());
            config.set(base + "passphrase", redbag.getPassphrase());
            config.set(base + "created-at", redbag.getCreatedAt());
            config.set(base + "remaining", redbag.getRemaining());
            for (Claim claim : redbag.getClaims().values()) {
                String claimBase = base + "claims." + claim.getPlayerId() + ".";
                config.set(claimBase + "name", claim.getPlayerName());
                config.set(claimBase + "amount", claim.getAmount());
                config.set(claimBase + "claimed-at", claim.getClaimedAt());
            }
        }

        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save redbags.yml: " + ex.getMessage());
        }
    }

    private Map<UUID, Claim> loadClaims(ConfigurationSection section) {
        Map<UUID, Claim> claims = new LinkedHashMap<UUID, Claim>();
        if (section == null) {
            return claims;
        }
        for (String key : section.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            String name = section.getString(key + ".name", "Unknown");
            double amount = section.getDouble(key + ".amount");
            long claimedAt = section.getLong(key + ".claimed-at");
            claims.put(playerId, new Claim(playerId, name, amount, claimedAt));
        }
        return claims;
    }

    static final class LoadedRedbags {
        private final int nextId;
        private final Map<Integer, Redbag> redbags;

        LoadedRedbags(int nextId, Map<Integer, Redbag> redbags) {
            this.nextId = nextId;
            this.redbags = redbags;
        }

        int getNextId() {
            return nextId;
        }

        Map<Integer, Redbag> getRedbags() {
            return redbags;
        }
    }
}
