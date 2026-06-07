# Redbag

Minecraft 1.12.2 Bukkit/CatServer red packet plugin.

## Build

```powershell
.\build.ps1
```

The built jar will be placed in `target/Redbag-<version>.jar`.

## Test

```powershell
.\mvnw.cmd test
```

## Install

Put the jar into the server `plugins` folder, then restart or load the server.

## Commands

- `/redbag send <total> <count> [message]` - create a red packet.
- `/redbag code <total> <count> <passphrase> [message]` - create a passphrase red packet.
- `/redbag grab <id> [passphrase]` - claim one share.
- `/redbag list` - list open red packets.
- `/redbag info <id>` - show red packet details.
- `/redbag reload` - reload config and data.

## Gameplay

- Normal red packet: split one total amount into multiple shares.
- Passphrase red packet: players must enter the correct passphrase when claiming.
- Lucky king: when a red packet is fully claimed, the server broadcasts the player who claimed the largest share.

## Permissions

- `redbag.use` - use player commands.
- `redbag.reload` - reload plugin config.
