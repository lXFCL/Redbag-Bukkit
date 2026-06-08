package com.codex.minecraft.redbag;

final class PendingRedbag {
    private final double total;
    private final int count;
    private final String message;
    private final long createdAt;

    PendingRedbag(double total, int count, String message, long createdAt) {
        this.total = total;
        this.count = count;
        this.message = message;
        this.createdAt = createdAt;
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

    boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - createdAt > ttlMillis;
    }
}
