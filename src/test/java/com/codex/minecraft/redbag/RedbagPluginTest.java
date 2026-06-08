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
        assertEquals(16, plugin.getConfig().getInt("settings.max-passphrase-length"));

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
    }
}
