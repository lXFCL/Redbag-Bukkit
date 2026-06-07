package com.codex.minecraft.redbag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MoneyTest {
    @Test
    public void roundKeepsTwoDecimalPlaces() {
        assertEquals(1.24D, Money.round(1.235D), 0.0001D);
        assertEquals("1.24", Money.format(1.235D));
    }

    @Test
    public void equalShareSplitsRemainingAmount() {
        assertEquals(3.33D, Money.nextShare(10D, 3, false), 0.0001D);
    }

    @Test
    public void randomShareLeavesMinimumForRemainingPlayers() {
        for (int i = 0; i < 100; i++) {
            double share = Money.nextShare(1D, 10, true);
            assertTrue(share >= 0.01D);
            assertTrue(share <= 0.91D);
        }
    }
}
