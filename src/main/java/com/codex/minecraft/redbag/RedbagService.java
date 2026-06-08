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
        return create(player, total, count, message, "", "");
    }

    CreateResult create(Player player, double total, int count, String message, String passphrase) {
        return create(player, total, count, message, passphrase, "");
    }

    CreateResult create(Player player, double total, int count, String message, String passphrase, String claimItemMaterial) {
        if (!economy.isReady()) {
            return CreateResult.error("economy-missing");
        }
        total = Money.round(total);
        double minTotal = plugin.getConfig().getDouble("settings.min-total", 1D);
        double maxTotal = plugin.getConfig().getDouble("settings.max-total", 1000000D);
        int minCount = plugin.getConfig().getInt("settings.min-count", 1);
        int maxCount = plugin.getConfig().getInt("settings.max-count", 100);
        int maxPassphraseLength = plugin.getConfig().getInt("settings.max-passphrase-length", 16);
        passphrase = Redbag.cleanPassphrase(passphrase);
        if (total < minTotal || total > maxTotal || count < minCount || count > maxCount || total < count * 0.01D) {
            return CreateResult.error("invalid-usage");
        }
        if (passphrase.length() > maxPassphraseLength) {
            return CreateResult.error("invalid-passphrase");
        }
        if (passphrase.length() > 0 && hasOpenPassphrase(passphrase)) {
            return CreateResult.error("duplicate-passphrase");
        }
        if (!economy.has(player, total)) {
            return CreateResult.error("insufficient-money");
        }
        if (!economy.withdraw(player, total)) {
            return CreateResult.error("insufficient-money");
        }

        Redbag redbag = new Redbag(nextId++, player.getUniqueId(), player.getName(), total, count, message, passphrase, claimItemMaterial, System.currentTimeMillis(), total, Collections.<java.util.UUID, Claim>emptyMap());
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
        if (redbag.requiresDropItem()) {
            return ClaimResult.error("drop-required");
        }

        double amount = Money.nextShare(redbag.getRemaining(), redbag.getRemainingCount(), plugin.getConfig().getBoolean("settings.random-amounts", true));
        return finishClaim(player, redbag, amount);
    }

    ClaimResult claimByItemUse(Player player, String materialName) {
        for (Redbag redbag : getOpenRedbags()) {
            if (redbag.matchesClaimItem(materialName) && !redbag.hasClaimed(player.getUniqueId())) {
                double speedWeight = getSpeedWeight(redbag);
                double bonusMultiplier = plugin.getConfig().getDouble("settings.drop-speed-bonus-multiplier", 1.5D);
                double amount = Money.nextShare(redbag.getRemaining(), redbag.getRemainingCount(), plugin.getConfig().getBoolean("settings.random-amounts", true), speedWeight, bonusMultiplier);
                return finishClaim(player, redbag, amount);
            }
        }
        return ClaimResult.error("no-item-redbag");
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

    boolean hasOpenPassphrase(String passphraseAnswer) {
        String answer = Redbag.cleanPassphrase(passphraseAnswer);
        if (answer.length() == 0) {
            return false;
        }
        for (Redbag redbag : getOpenRedbags()) {
            if (redbag.hasPassphrase() && redbag.matchesPassphrase(answer)) {
                return true;
            }
        }
        return false;
    }

    boolean hasClaimableDropItem(Player player, String materialName) {
        for (Redbag redbag : getOpenRedbags()) {
            if (redbag.matchesClaimItem(materialName) && !redbag.hasClaimed(player.getUniqueId())) {
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
        long expireMillis = plugin.getConfig().getLong("settings.expire-minutes", 5L) * 60L * 1000L;
        return System.currentTimeMillis() - redbag.getCreatedAt() > expireMillis;
    }

    void broadcastCreated(Redbag redbag) {
        if (!plugin.getConfig().getBoolean("settings.broadcast-created", true)) {
            return;
        }
        if (redbag.hasPassphrase()) {
            Bukkit.broadcastMessage(format(plugin.msg("created-code-broadcast"), redbag));
        } else {
            Bukkit.broadcastMessage(format(plugin.msg("created-broadcast"), redbag));
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

    void broadcastClaimed(Redbag redbag, Claim claim) {
        if (!plugin.getConfig().getBoolean("settings.broadcast-claimed", true)) {
            return;
        }
        Bukkit.broadcastMessage(format(plugin.msg("claimed-broadcast"), redbag)
                .replace("{claimer}", claim.getPlayerName())
                .replace("{amount}", Money.format(claim.getAmount()))
                .replace("{remaining-count}", String.valueOf(redbag.getRemainingCount()))
                .replace("{remaining}", Money.format(redbag.getRemaining())));
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

    private ClaimResult finishClaim(Player player, Redbag redbag, double amount) {
        if (!economy.deposit(player, amount)) {
            return ClaimResult.error("economy-missing");
        }
        Claim claim = redbag.addClaim(player.getUniqueId(), player.getName(), amount, System.currentTimeMillis());
        save();
        broadcastClaimed(redbag, claim);
        if (redbag.isFinished()) {
            broadcastLuckyKing(redbag);
        }
        return ClaimResult.success(redbag, claim);
    }

    private double getSpeedWeight(Redbag redbag) {
        long expireMillis = plugin.getConfig().getLong("settings.expire-minutes", 10L) * 60L * 1000L;
        if (expireMillis <= 0L) {
            return 0D;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - redbag.getCreatedAt());
        return Math.max(0D, Math.min(1D, 1D - (double) elapsed / (double) expireMillis));
    }

    private String rawMessage(String path, String fallback) {
        return plugin.color(plugin.getConfig().getString("messages." + path, fallback));
    }

    private String format(String text, Redbag redbag) {
        return text.replace("{player}", redbag.getOwnerName())
                .replace("{id}", String.valueOf(redbag.getId()))
                .replace("{passphrase}", redbag.getPassphrase())
                .replace("{claim-item}", getClaimItemDisplayName(redbag.getClaimItemMaterial()))
                .replace("{claim-item-material}", redbag.getClaimItemMaterial())
                .replace("{message}", redbag.getMessage())
                .replace("{expire}", String.valueOf(plugin.getConfig().getLong("settings.expire-minutes", 10L)));
    }

    private String getClaimItemDisplayName(String materialName) {
        String path = "drop-items." + materialName + ".name";
        String configured = plugin.getConfig().getString(path, "");
        return configured == null || configured.trim().length() == 0 ? materialName : configured;
    }

    private void sendTitle(Redbag redbag) {
        String mainKey = redbag.hasPassphrase() ? "title-code-main" : "title-main";
        String subtitleKey = redbag.hasPassphrase() ? "title-code-subtitle" : "title-subtitle";
        String title = format(rawMessage(mainKey, "&c&l红包来了！"), redbag);
        String subtitle = format(rawMessage(subtitleKey, "&f祝福语：{message}"), redbag);
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
