package com.codex.minecraft.redbag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.bukkit.command.PluginCommand;
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
}
