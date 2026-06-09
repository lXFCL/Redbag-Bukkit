package com.codex.minecraft.redbag;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.java.JavaPlugin;

public final class RedbagPlugin extends JavaPlugin {
    private RedbagService redbagService;
    private RedbagStorage storage;
    private EconomyBridge economy;
    private RedbagDropGui dropGui;

    public RedbagPlugin() {
        super();
    }

    protected RedbagPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mergeDefaultConfig();
        this.storage = new RedbagStorage(this);
        reloadRuntime();
        this.dropGui = new RedbagDropGui(this);

        RedbagCommand command = new RedbagCommand(this, redbagService, dropGui);
        PluginCommand pluginCommand = getCommand("redbag");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(new RedbagChatListener(this), this);
        getServer().getPluginManager().registerEvents(dropGui, this);
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                redbagService.refundExpiredRedbags();
            }
        }, 20L * 60L, 20L * 60L);

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
        mergeDefaultConfig();
        reloadRuntime();
    }

    public RedbagService getRedbagService() {
        return redbagService;
    }

    RedbagDropGui getDropGui() {
        return dropGui;
    }

    private void reloadRuntime() {
        this.economy = new EconomyBridge(this);
        this.redbagService = new RedbagService(this, storage, economy);
        this.redbagService.load();
    }

    private void mergeDefaultConfig() {
        Reader reader = null;
        try {
            reader = new InputStreamReader(getResource("config.yml"), "UTF-8");
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            getConfig().setDefaults(defaults);
            getConfig().options().copyDefaults(true);
            saveConfig();
        } catch (Exception ex) {
            getLogger().warning("Failed to merge default config: " + ex.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
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
