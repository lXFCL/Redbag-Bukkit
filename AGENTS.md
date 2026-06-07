# AGENTS.md

## Project

This is the `Redbag` Minecraft plugin project.

- Main plugin class: `com.codex.minecraft.redbag.RedbagPlugin`
- Plugin descriptor: `src/main/resources/plugin.yml`
- Maven project version: `pom.xml` / `<version>`
- Plugin runtime version: `src/main/resources/plugin.yml` / `version`
- Current compile API: Bukkit `1.12.2-R0.1-SNAPSHOT`
- Target local test server directory: `D:\catserver\1.21 pokemon`
- GitHub repository: `https://github.com/lXFCL/Redbag-Bukkit.git`

Be careful not to assume the compile API version and the local test server version are the same. If a change depends on 1.21-only APIs, verify the project dependencies and server compatibility before editing.

This is a Bukkit plugin and should stay broadly compatible across server versions whenever possible. Prefer the oldest stable Bukkit API surface already used by the project, avoid version-specific NMS/CraftBukkit classes, and do not add hard dependencies on one server version unless the user explicitly requests it.

## Build And Test

Use the Maven wrapper for automated tests:

```powershell
.\mvnw.cmd test
```

The existing manual build script is:

```powershell
.\build.ps1
```

Before compiling after plugin changes, stop the local test server gracefully if it is running. Do not compile over a running server that may have the plugin jar loaded.

After compiling a plugin jar for runtime testing, copy the built jar into:

```text
D:\catserver\1.21 pokemon\plugins
```

Before copying the newly built jar, delete previous `Redbag` plugin jars from that `plugins` directory so the server cannot load an older version by mistake. Only remove old jars for this plugin; do not delete unrelated plugins.

Do not delete or reset the server directory. Treat it as user-managed runtime state.

When the user asks to modify the plugin, the agent is responsible for verifying the change. If the local test server is not running when runtime verification is needed, start it from `D:\catserver\1.21 pokemon` using that directory's existing launch script or server jar. After copying the updated jar, start the server again if needed and inspect logs/behavior to confirm the modification works.

After each completed update, commit the finished project changes and push them to the GitHub repository unless the user explicitly says not to push. Do not push generated files, local server files, caches, or IDE metadata.

## Versioning Rule

Every completed code/build change must increment both:

- `pom.xml` `<version>`
- `src/main/resources/plugin.yml` `version`

Use semantic versioning based on the size of the current change:

- If changed lines are more than 200 lines, bump the minor version.
  - Example: `1.0.0` -> `1.1.0`
- If changed lines are 200 lines or fewer, bump the patch version.
  - Example: `1.0.0` -> `1.0.1`

Changed lines means added plus deleted lines for this task. Ignore generated build output, dependency caches, IDE metadata, and test reports when deciding the bump.

Before finishing, verify the two version fields match.

## Coding Guidelines

- Keep Java source compatible with the configured compiler target unless the project is explicitly migrated.
- Prefer Bukkit/Paper API calls over server internals.
- Keep feature implementations version-tolerant for Bukkit servers across versions. Use reflection or capability checks only when truly necessary, and keep fallbacks explicit.
- Keep command behavior and message keys compatible with `config.yml`.
- Add or update tests when changing command parsing, redbag allocation, storage, permissions, economy integration, or plugin lifecycle behavior.
- Do not commit generated files from `target`, test server folders, local caches, or IDE folders.

## Final Verification

For normal code changes, run:

```powershell
.\mvnw.cmd test
```

For runtime-facing changes, also build the jar and place it in the configured test server `plugins` directory when appropriate.

Runtime verification workflow for plugin changes:

1. Stop the test server gracefully if it is running.
2. Run automated tests.
3. Build the plugin jar.
4. Delete previous `Redbag` jars from `D:\catserver\1.21 pokemon\plugins`.
5. Copy the newly built jar into that `plugins` directory.
6. Start the test server if it is not running.
7. Check logs and exercise the changed behavior before reporting completion.
