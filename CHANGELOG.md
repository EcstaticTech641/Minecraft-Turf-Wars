# Changelog

All notable changes to the `rga-turfwars` companion plugin will be documented in this file.

The format is based on [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-SNAPSHOT] - 2026-08-03

### Added
- CPMK migration from legacy standalone `minecraft-turf-wars` codebase to `rga-turfwars` companion plugin.
- Direct integration with `com.ronlab:rga-api:1.13.0-SNAPSHOT` for event-driven minigame lifecycles (`MinigameStartEvent`, `MinigameConcludeEvent`).
- Native Paper death and respawn event listener (`TurfWarsDeathListener.java`) for in-arena player deaths and respawns.
- Re-entrancy guard mechanism (`teleporting` in `TurfWarsSession` and `bypassGuard` in `TurfWarsGameListener`) with `try/finally` cleanup to prevent infinite `PlayerTeleportEvent` recursion.
- Multi-phase spawn enforcement at 10L (0.5s) and 20L (1.0s) to guarantee chunk loading completion during Paper dimension pipeline initialization.
- Solo-developer QA guard and developer override command `/turfwars forcewin` with `turfwars.admin` permission.
- Event protection listeners for item dropping, item pickup, offhand swapping, inventory clicking, environmental damage, and hunger decay.

### Changed
- Converted build toolchain from Gradle to Maven (targeting Java 25 and Paper 26.2).
- Optimized territory block updates (`updateTerritoryBlocks()`) from a 57,792-block 3D volume scan to target $Z$-row updates ($X: [-43 \dots -1]$ at $Y = \text{FLOOR\_Y}$).
- Replaced RGA core `setSpectator` API call during mid-game 5-second spectator respawns with local `GameMode.SPECTATOR` / `SURVIVAL` state toggles, preventing unintended mid-match player ejection to the server Hub world.
- Separated spatial teleportation (`teleportPlayerToSpawn`) from inventory kit distribution (`equipCombatKit`), adding an explicit inventory check (`contains(Material.BLACK_WOOL)`) to guarantee idempotent kit delivery.
- Replaced reflection in `RgaBridge.java` with typed `rga-api` calls (`RGA.getInstance()`).
- Aligned version strings across `pom.xml` and `paper-plugin.yml`.

### Fixed
- Fixed server-crashing `StackOverflowError` caused by recursive `PlayerTeleportEvent` calls during spawn correction.
- Fixed player dimension loading race conditions where Paper's connection pipeline left players at default world spawn instead of in-arena team spawns.
- Fixed mid-match Hub world ejection by isolating minigame spectator state toggles from RGA core's session conclusion spectator pipeline.
- Fixed uncancelled Bukkit task memory leaks on player respawns by tracking active tasks in `Set<BukkitTask>` and cancelling them on session cleanup.
- Fixed orphaned custom scoreboards by restoring `Bukkit.getScoreboardManager().getMainScoreboard()` upon match conclusion.
- Fixed player disconnect handling to trigger victory checks when players quit mid-match.
- Fixed movement pushback rubberbanding by applying smooth velocity vectors away from territory divide lines.
- Fixed death event conflicts by cancelling `EntityDamageByEntityEvent` during instant bow kills before initiating the internal respawn pipeline.

### Removed
- Removed legacy world lifecycle managers (`ArenaManager.java`, `LobbyManager.java`).
- Removed legacy command executors and hardcoded world loading directory copy routines.
