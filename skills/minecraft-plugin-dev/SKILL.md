---
name: minecraft-plugin-dev
description: Build, modify, and test Minecraft Bukkit/Spigot/Paper/CatServer plugins, especially legacy 1.12.2 Java plugins. Use when Codex needs to create plugin.yml, JavaPlugin entrypoints, command executors, config/data YAML, local server-jar compilation, Vault economy hooks, or package a plugin jar for a Minecraft server.
---

# Minecraft Plugin Dev

Use this workflow for Bukkit-style Minecraft plugins.

## Workflow

1. Inspect the server version and available jars before choosing APIs.
2. Prefer Java 8 bytecode for Minecraft 1.12.2 compatibility: `-source 8 -target 8`.
3. Keep external dependencies `provided` when the server already supplies them.
4. Include `plugin.yml` with `name`, `main`, `version`, commands, permissions, and `depend`/`softdepend`.
5. For CatServer 1.12.2, compile against the local universal server jar when Maven dependencies are unavailable.
6. Avoid NMS unless the task requires version-specific internals.
7. Save config defaults with `saveDefaultConfig()` and store mutable runtime data in separate YAML files.
8. Test by compiling, inspecting jar contents, and when feasible loading the jar on the target server.

## Economy Plugins

Use Vault for economy integration when available. If the Vault API jar is not available as a compile dependency, use reflection against `net.milkbowl.vault.economy.Economy` and set `softdepend: [Vault]` in `plugin.yml`.

## Legacy Compatibility

For 1.12.2:

- Do not use modern Bukkit APIs without checking signatures in the server jar.
- Use `Bukkit.getOfflinePlayer(UUID)` for offline players.
- Avoid Java 9+ library APIs in plugin code even when the local compiler is newer.
- Keep text color handling with `ChatColor.translateAlternateColorCodes('&', text)`.

## Build Pattern Without Maven

When Maven is missing, create a small build script that:

1. Deletes and recreates `target/classes`.
2. Finds all `src/main/java/**/*.java` files.
3. Runs `javac -encoding UTF-8 -source 8 -target 8 -cp <server.jar> -d target/classes`.
4. Copies `src/main/resources/*` into `target/classes`.
5. Runs `jar cf target/<PluginName>-<version>.jar -C target/classes .`.
