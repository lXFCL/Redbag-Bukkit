# Redbag

Minecraft 1.12.2 Bukkit/CatServer red packet plugin.

## Build

```powershell
.\build.ps1
```

The built jar will be placed in `target/Redbag-1.0.0.jar`.

## Test

```powershell
.\mvnw.cmd test
```

## Install

Put the jar into the server `plugins` folder, then restart or load the server.

## Commands

- `/redbag send <total> <count> [message]` - create a red packet.
- `/redbag grab <id>` - claim one share.
- `/redbag list` - list open red packets.
- `/redbag info <id>` - show red packet details.
- `/redbag reload` - reload config and data.

## Permissions

- `redbag.use` - use player commands.
- `redbag.reload` - reload plugin config.
