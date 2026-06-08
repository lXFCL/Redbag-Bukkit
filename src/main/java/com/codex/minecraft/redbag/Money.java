package com.codex.minecraft.redbag;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

final class Money {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");

    private Money() {
    }

    static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    static String format(double value) {
        synchronized (FORMAT) {
            return FORMAT.format(round(value));
        }
    }

    static double nextShare(double remaining, int remainingCount, boolean randomAmounts) {
        return nextShare(remaining, remainingCount, randomAmounts, 0D, 0D);
    }

    static double nextShare(double remaining, int remainingCount, boolean randomAmounts, double speedWeight, double speedBonusMultiplier) {
        if (remainingCount <= 1) {
            return round(remaining);
        }
        if (!randomAmounts) {
            return round(remaining / remainingCount);
        }

        double min = 0.01D;
        speedWeight = Math.max(0D, Math.min(1D, speedWeight));
        speedBonusMultiplier = Math.max(0D, speedBonusMultiplier);
        double max = Math.max(min, remaining / remainingCount * 2D * (1D + speedWeight * speedBonusMultiplier));
        double amount = ThreadLocalRandom.current().nextDouble(min, max);
        double rounded = round(amount);
        double reserve = (remainingCount - 1) * min;
        return Math.max(min, Math.min(rounded, round(remaining - reserve)));
    }
}
