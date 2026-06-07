package com.codex.minecraft.redbag;

import java.util.UUID;

final class Claim {
    private final UUID playerId;
    private final String playerName;
    private final double amount;
    private final long claimedAt;

    Claim(UUID playerId, String playerName, double amount, long claimedAt) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.amount = amount;
        this.claimedAt = claimedAt;
    }

    UUID getPlayerId() {
        return playerId;
    }

    String getPlayerName() {
        return playerName;
    }

    double getAmount() {
        return amount;
    }

    long getClaimedAt() {
        return claimedAt;
    }
}
