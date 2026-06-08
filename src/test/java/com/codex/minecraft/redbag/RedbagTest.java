package com.codex.minecraft.redbag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

public class RedbagTest {
    @Test
    public void passphraseIsTrimmedAndMatchedExactly() {
        Redbag redbag = new Redbag(1, UUID.randomUUID(), "Alice", 10D, 2, "hello", "  lucky  ", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap());

        assertTrue(redbag.hasPassphrase());
        assertTrue(redbag.matchesPassphrase("lucky"));
        assertTrue(redbag.matchesPassphrase(" lucky "));
        assertFalse(redbag.matchesPassphrase("LUCKY"));
        assertFalse(redbag.matchesPassphrase(""));
    }

    @Test
    public void emptyPassphraseDoesNotBlockClaim() {
        Redbag redbag = new Redbag(1, UUID.randomUUID(), "Alice", 10D, 2, "hello", "", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap());

        assertFalse(redbag.hasPassphrase());
        assertTrue(redbag.matchesPassphrase(""));
        assertTrue(redbag.matchesPassphrase("anything"));
    }

    @Test
    public void dropItemRedbagMatchesConfiguredMaterial() {
        Redbag redbag = new Redbag(1, UUID.randomUUID(), "Alice", 10D, 2, "hello", "", "dirt", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap());

        assertTrue(redbag.requiresDropItem());
        assertEquals("DIRT", redbag.getClaimItemMaterial());
        assertTrue(redbag.matchesClaimItem("DIRT"));
        assertTrue(redbag.matchesClaimItem("dirt"));
        assertFalse(redbag.matchesClaimItem("STONE"));
    }

    @Test
    public void cleanPassphraseTrimsChatInput() {
        assertEquals("lucky", Redbag.cleanPassphrase(" lucky "));
        assertEquals("", Redbag.cleanPassphrase(null));
    }

    @Test
    public void bestClaimReturnsHighestAmount() {
        Redbag redbag = new Redbag(1, UUID.randomUUID(), "Alice", 10D, 3, "hello", "", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap());

        redbag.addClaim(UUID.randomUUID(), "Bob", 1.25D, System.currentTimeMillis());
        redbag.addClaim(UUID.randomUUID(), "Carol", 3.50D, System.currentTimeMillis());
        redbag.addClaim(UUID.randomUUID(), "Dave", 2.00D, System.currentTimeMillis());

        assertEquals("Carol", redbag.getBestClaim().getPlayerName());
        assertEquals(3.50D, redbag.getBestClaim().getAmount(), 0.0001D);
    }
}
