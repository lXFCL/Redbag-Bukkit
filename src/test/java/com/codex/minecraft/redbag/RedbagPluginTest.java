package com.codex.minecraft.redbag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

public class RedbagPluginTest {
    private ServerMock server;
    private RedbagPlugin plugin;

    @Before
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(RedbagPlugin.class);
    }

    @After
    public void tearDown() {
        MockBukkit.unload();
    }

    @Test
    public void pluginLoadsDefaultConfigAndRegistersCommand() {
        assertNotNull(server);
        assertNotNull(plugin.getRedbagService());
        assertEquals(1D, plugin.getConfig().getDouble("settings.min-total"), 0.0001D);
        assertEquals(10, plugin.getConfig().getInt("settings.expire-minutes"));
        assertTrue(plugin.getConfig().getBoolean("settings.broadcast-claimed"));
        assertEquals(1.5D, plugin.getConfig().getDouble("settings.drop-speed-bonus-multiplier"), 0.0001D);
        assertEquals(16, plugin.getConfig().getInt("settings.max-passphrase-length"));
        assertEquals("&c&l红包来了！", plugin.getConfig().getString("messages.title-main"));
        assertEquals(10, plugin.getConfig().getConfigurationSection("drop-items").getKeys(false).size());
        assertEquals("泥土", plugin.getConfig().getString("drop-items.DIRT.name"));
        assertTrue(plugin.getConfig().getString("messages.created-broadcast").contains("{claim-item}"));
        assertTrue(plugin.getConfig().getString("messages.created-broadcast").contains("左键空气"));
        assertTrue(plugin.getConfig().getString("messages.drop-created-tip").contains("左键"));
        assertTrue(plugin.getConfig().getString("messages.created-broadcast").contains("祝福语: "));
        assertTrue(plugin.getConfig().getString("messages.created-code-broadcast").contains("祝福语: "));
        assertTrue(plugin.getConfig().getString("messages.claimed-broadcast").contains("{remaining-count}"));

        PluginCommand command = plugin.getCommand("redbag");
        assertNotNull(command);
        assertEquals("Send and claim red packets.", command.getDescription());
    }

    @Test
    public void chatPassphraseMatchesOpenCodeRedbag() {
        Player player = server.addPlayer();
        Map<Integer, Redbag> redbags = new LinkedHashMap<Integer, Redbag>();
        redbags.put(1, new Redbag(1, UUID.randomUUID(), "Alice", 10D, 2, "hello", "lucky", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap()));
        plugin.getRedbagService().replaceRedbagsForTest(2, redbags);

        assertTrue(plugin.getRedbagService().hasClaimablePassphrase(player, " lucky "));
        assertFalse(plugin.getRedbagService().hasClaimablePassphrase(player, "wrong"));
        assertTrue(plugin.getRedbagService().hasOpenPassphrase("lucky"));
    }

    @Test
    public void chatPassphraseIgnoresAlreadyClaimedPlayer() {
        Player player = server.addPlayer();
        Map<UUID, Claim> claims = new LinkedHashMap<UUID, Claim>();
        claims.put(player.getUniqueId(), new Claim(player.getUniqueId(), "Bob", 5D, System.currentTimeMillis()));
        Map<Integer, Redbag> redbags = new LinkedHashMap<Integer, Redbag>();
        redbags.put(1, new Redbag(1, UUID.randomUUID(), "Alice", 10D, 2, "hello", "lucky", System.currentTimeMillis(), 5D, claims));
        plugin.getRedbagService().replaceRedbagsForTest(2, redbags);

        assertFalse(plugin.getRedbagService().hasClaimablePassphrase(player, "lucky"));
        assertTrue(plugin.getRedbagService().hasOpenPassphrase("lucky"));
    }

    @Test
    public void openPassphraseIgnoresFinishedCodeRedbag() {
        Map<Integer, Redbag> redbags = new LinkedHashMap<Integer, Redbag>();
        redbags.put(1, new Redbag(1, UUID.randomUUID(), "Alice", 10D, 1, "hello", "lucky", System.currentTimeMillis(), 0D, Collections.<UUID, Claim>emptyMap()));
        plugin.getRedbagService().replaceRedbagsForTest(2, redbags);

        assertFalse(plugin.getRedbagService().hasOpenPassphrase("lucky"));
    }

    @Test
    public void blockingDropRedbagOnlyMatchesOpenDropRedbag() {
        Player alice = server.addPlayer();
        Player bob = server.addPlayer();
        Map<Integer, Redbag> redbags = new LinkedHashMap<Integer, Redbag>();
        redbags.put(1, new Redbag(1, alice.getUniqueId(), "Alice", 10D, 2, "hello", "", "DIRT", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap()));
        plugin.getRedbagService().replaceRedbagsForTest(2, redbags);
        assertTrue(plugin.getRedbagService().hasBlockingDropRedbag(alice));
        assertFalse(plugin.getRedbagService().hasBlockingDropRedbag(bob));
        assertFalse(plugin.getRedbagService().hasBlockingCodeRedbag(alice));

        redbags.clear();
        redbags.put(1, new Redbag(1, alice.getUniqueId(), "Alice", 10D, 1, "hello", "", "DIRT", System.currentTimeMillis(), 0D, Collections.<UUID, Claim>emptyMap()));
        plugin.getRedbagService().replaceRedbagsForTest(2, redbags);
        assertFalse(plugin.getRedbagService().hasBlockingDropRedbag(alice));

        redbags.clear();
        redbags.put(1, new Redbag(1, alice.getUniqueId(), "Alice", 10D, 2, "hello", "", "DIRT", System.currentTimeMillis() - 11L * 60L * 1000L, 10D, Collections.<UUID, Claim>emptyMap()));
        plugin.getRedbagService().replaceRedbagsForTest(2, redbags);
        assertFalse(plugin.getRedbagService().hasBlockingDropRedbag(alice));
    }

    @Test
    public void blockingCodeRedbagOnlyMatchesOpenCodeRedbag() {
        Player alice = server.addPlayer();
        Player bob = server.addPlayer();
        Map<Integer, Redbag> redbags = new LinkedHashMap<Integer, Redbag>();
        redbags.put(1, new Redbag(1, alice.getUniqueId(), "Alice", 10D, 2, "hello", "lucky", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap()));
        plugin.getRedbagService().replaceRedbagsForTest(2, redbags);
        assertTrue(plugin.getRedbagService().hasBlockingCodeRedbag(alice));
        assertFalse(plugin.getRedbagService().hasBlockingCodeRedbag(bob));
        assertFalse(plugin.getRedbagService().hasBlockingDropRedbag(alice));

        redbags.clear();
        redbags.put(1, new Redbag(1, alice.getUniqueId(), "Alice", 10D, 2, "hello", "lucky", System.currentTimeMillis() - 11L * 60L * 1000L, 10D, Collections.<UUID, Claim>emptyMap()));
        plugin.getRedbagService().replaceRedbagsForTest(2, redbags);
        assertFalse(plugin.getRedbagService().hasBlockingCodeRedbag(alice));
    }

    @Test
    public void samePlayerCanHaveDropAndCodeRedbagAtTheSameTime() {
        Player alice = server.addPlayer();
        Map<Integer, Redbag> redbags = new LinkedHashMap<Integer, Redbag>();
        redbags.put(1, new Redbag(1, alice.getUniqueId(), "Alice", 10D, 2, "drop", "", "DIRT", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap()));
        redbags.put(2, new Redbag(2, alice.getUniqueId(), "Alice", 10D, 2, "code", "lucky", System.currentTimeMillis(), 10D, Collections.<UUID, Claim>emptyMap()));
        plugin.getRedbagService().replaceRedbagsForTest(3, redbags);

        assertTrue(plugin.getRedbagService().hasBlockingDropRedbag(alice));
        assertTrue(plugin.getRedbagService().hasBlockingCodeRedbag(alice));
    }
}
