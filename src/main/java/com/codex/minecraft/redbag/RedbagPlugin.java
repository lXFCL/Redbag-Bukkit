package com.codex.minecraft.redbag;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.java.JavaPlugin;

public final class RedbagPlugin extends JavaPlugin {
    private RedbagService redbagService;
    private RedbagStorage storage;
    private EconomyBridge economy;

    public RedbagPlugin() {
        super();
    }

    protected RedbagPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.storage = new RedbagStorage(this);
        reloadRuntime();

        RedbagCommand command = new RedbagCommand(this, redbagService);
        PluginCommand pluginCommand = getCommand("redbag");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("Redbag enabled. Economy ready: " + economy.isReady());
    }

    @Override
    public void onDisable() {
        if (redbagService != null) {
            redbagService.save();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        reloadRuntime();
    }

    public RedbagService getRedbagService() {
        return redbagService;
    }

    private void reloadRuntime() {
        this.economy = new EconomyBridge(this);
        this.redbagService = new RedbagService(this, storage, economy);
        this.redbagService.load();
    }

    public String msg(String path) {
        String prefix = getConfig().getString("messages.prefix", "&c[Redbag]&r ");
        String body = getConfig().getString("messages." + path, path);
        return color(prefix + body);
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
