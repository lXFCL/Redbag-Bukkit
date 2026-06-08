package com.codex.minecraft.redbag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class Redbag {
    private final int id;
    private final UUID ownerId;
    private final String ownerName;
    private final double total;
    private final int count;
    private final String message;
    private final String passphrase;
    private final long createdAt;
    private final Map<UUID, Claim> claims;
    private double remaining;

    Redbag(int id, UUID ownerId, String ownerName, double total, int count, String message, String passphrase, long createdAt, double remaining, Map<UUID, Claim> claims) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.total = total;
        this.count = count;
        this.message = message;
        this.passphrase = cleanPassphrase(passphrase);
        this.createdAt = createdAt;
        this.remaining = remaining;
        this.claims = new LinkedHashMap<UUID, Claim>(claims);
    }

    int getId() {
        return id;
    }

    UUID getOwnerId() {
        return ownerId;
    }

    String getOwnerName() {
        return ownerName;
    }

    double getTotal() {
        return total;
    }

    int getCount() {
        return count;
    }

    String getMessage() {
        return message;
    }

    String getPassphrase() {
        return passphrase;
    }

    boolean hasPassphrase() {
        return passphrase.length() > 0;
    }

    boolean matchesPassphrase(String answer) {
        if (!hasPassphrase()) {
            return true;
        }
        return passphrase.equals(cleanPassphrase(answer));
    }

    long getCreatedAt() {
        return createdAt;
    }

    double getRemaining() {
        return remaining;
    }

    int getRemainingCount() {
        return count - claims.size();
    }

    Map<UUID, Claim> getClaims() {
        return claims;
    }

    boolean hasClaimed(UUID playerId) {
        return claims.containsKey(playerId);
    }

    boolean isFinished() {
        return getRemainingCount() <= 0 || remaining <= 0.0001D;
    }

    Claim addClaim(UUID playerId, String playerName, double amount, long claimedAt) {
        remaining = Math.max(0D, Money.round(remaining - amount));
        Claim claim = new Claim(playerId, playerName, amount, claimedAt);
        claims.put(playerId, claim);
        return claim;
    }

    Claim getBestClaim() {
        Claim best = null;
        for (Claim claim : claims.values()) {
            if (best == null || claim.getAmount() > best.getAmount()) {
                best = claim;
            }
        }
        return best;
    }

    void refundRemaining() {
        remaining = 0D;
    }

    static String cleanPassphrase(String text) {
        return text == null ? "" : text.trim();
    }
}
