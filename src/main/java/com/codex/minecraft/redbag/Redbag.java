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
    private final long createdAt;
    private final Map<UUID, Claim> claims;
    private double remaining;

    Redbag(int id, UUID ownerId, String ownerName, double total, int count, String message, long createdAt, double remaining, Map<UUID, Claim> claims) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.total = total;
        this.count = count;
        this.message = message;
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

    void refundRemaining() {
        remaining = 0D;
    }
}
