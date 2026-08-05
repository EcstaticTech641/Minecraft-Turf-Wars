# Turf Wars Companion Plugin - Developer & Operator User Guide 🎮

> **Companion Plugin Micro-Kernel (CPMK) Specification Document**  
> **Target Environment:** Java 25 | PaperMC 26.2 | RGA Core 1.13.0-SNAPSHOT  
> **Plugin Identifier:** `TurfWarsCompanion` (`com.ronlab:rga-turfwars:1.0.0-SNAPSHOT`)

---

## 📌 1. Architectural Baseline & CPMK Pillars

`rga-turfwars` is built on the canonical **Companion Plugin Micro-Kernel (CPMK)** standard powered by `RonlabGameAssistant` (`rga-core`). The companion operates as a self-contained minigame module that communicates strictly through the decoupled `com.ronlab:rga-api` event bus.

### The 5 CPMK Pillars in Turf Wars:

1. **Core Gameplay Function Retention:**  
   Preserves 100% of the native tug-of-war minigame mechanics, custom game loops, territory shift algorithms, and team scoreboards.
2. **Ronlab Integration Standard:**  
   Listens exclusively for `MinigameStartEvent` and `MinigameConcludeEvent`. Specified in `paper-plugin.yml` with `api-version: '26.2'` and hard server dependency on `RonlabGameAssistant` (`required: true`, `join-classpath: true`).
3. **Baseline Structure & Rules Provision:**  
   - Employs PaperMC's `objective.numberFormat(NumberFormat.blank())` across sidebar lines to suppress margin numbers.
   - Performs scoreboard assignment (`player.setScoreboard()`) strictly during post-teleport spawn phases (10L/20L ticks) to prevent chunk-loading hangs.
   - Performs complete teardown via `player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard())` and task cancellation upon session conclusion.
4. **Companion-Type Agnostic Design:**  
   Remains completely decoupled from `rga-core` internal classes, referencing only `rga-api` models (`MinigameId`, `MinigameStartEvent`, `MinigameConcludeEvent`).
5. **Feature Implementation & Modification Specs:**  
   Fully documents local administrative commands (`/turfwars forcewin`), permission nodes (`turfwars.admin`), spatial vector configurations (`arena.yml`), and **Solo QA Developer Mode** (`initialPlayerCount == 1`).

---

## ⚔️ 2. Minigame Mechanics & Ruleset

Turf Wars is a competitive, team-based tug-of-war combat minigame played on a rectangular arena.

### Team Allocation & Spawns
- **Black Team** (`BLACK_WOOL` / `BLACK_CONCRETE`):
  - Spawn Vector: `(X: -21.0, Y: -59.0, Z: 0.0, Yaw: 0.0, Pitch: 0.0)`
- **Gold Team** (`YELLOW_WOOL` / `YELLOW_CONCRETE`):
  - Spawn Vector: `(X: -21.0, Y: -59.0, Z: 77.0, Yaw: 180.0, Pitch: 0.0)`

### Combat Rules
- **One-Shot Bow Combat:** Any direct arrow hit or melee kill instantly eliminates an opponent.
- **Kits & Inventory:** Players receive a Bow, 1 Arrow, dynamic wool building blocks, and leather armor matching their team color.
- **Wool Replenishment:** A background task runs every 100 ticks (5 seconds), granting additional team wool and ensuring players maintain at least 1 arrow.

### Dynamic Turf Advancement
- The arena floor consists of concrete blocks spanning `X: [-43, -1]` and `Z: [7, 70]` at `Y: -61`.
- The territory division line starts at `Z = 38.5`.
- **Kill Shifts:** Eliminating an opponent advances your team's divide line forward into enemy territory:
  - **Large Games (>8 players):** `1.0` block per kill.
  - **Medium Games (6-8 players):** `2.0` blocks per kill.
  - **Small Games (<=5 players):** `3.0` blocks per kill.
- **Territory Update & Clearing:**
  - Floor concrete recalculates immediately (`BLACK_CONCRETE` for $Z < \text{divideLine}$, `YELLOW_CONCRETE` for $Z \ge \text{divideLine}$).
  - Enemy wool blocks placed in newly captured territory (`Y: -60` to `Y: -53`) are automatically cleared to prevent blocking advancing teams.

### Match Conclusion
- **Black Win Condition:** Division line reaches or exceeds `Z = 70` ($Z \ge \text{MAX\_Z}$).
- **Gold Win Condition:** Division line reaches or drops below `Z = 7` ($Z \le \text{MIN\_Z}$).
- **Time Limit:** 600 seconds (10 minutes).

---

## 🧪 3. Solo QA Developer Mode (`initialPlayerCount == 1`)

To facilitate single-developer testing without requiring multi-account setups, Turf Wars implements **Solo QA Developer Mode**.

```java
// CPMK Solo-Developer QA Guard in TurfWarsSession.java
if (initialPlayerCount == 1) {
    plugin.getLogger().info("[CPM] Single-player testing mode detected; suppressing automatic 0-opponent win condition.");
    return;
}
```

### Key QA Mode Features:
1. **Win Condition Freeze:** Automatic zero-opponent win checks are suppressed when a session starts with only 1 player.
2. **Persistent Environment:** Allows single developers to continuously test arena boundaries, spawn vectors, wool building, arrow replenishment, floor concrete shifting, and scoreboard rendering.
3. **Manual Conclude Testing:** Developers can execute `/turfwars forcewin` at any time to verify post-match cleanup and `RgaBridge.requestSessionConclude()` execution.

---

## 💻 4. Administrative Commands & Permission Nodes

| Command | Aliases | Description | Permission Node | Default |
| :--- | :--- | :--- | :--- | :--- |
| `/turfwars forcewin` | `/tw forcewin` | Programmatically triggers session conclusion for QA testing. | `turfwars.admin` | OP / Admin |
| `/turfwars` | `/tw` | Displays developer test command usage overview. | `turfwars.admin` | OP / Admin |

---

## 📡 5. CPMK Event Bus Integration & Scoreboard Lifecycle

### Event Bus Handlers (`RgaEventListener.java`)
- **`MinigameStartEvent`:**
  - Validates `minigameId` matches `ronlab:turfwars`, `turfwars`, or `turf_wars`.
  - Instantiates `TurfWarsSession` for the target world.
  - Schedules 10L and 20L post-teleport tasks for team spawn positioning and combat kit equipping.
- **`MinigameConcludeEvent`:**
  - Retrieves active session for target world.
  - Invokes `session.cleanup()`.
  - Restores player scoreboards to `Bukkit.getScoreboardManager().getMainScoreboard()`.
  - Cancels all running session timers and tasks.

### Scoreboard Formatting (`TurfWarsScoreboard.java`)
- Scoreboard objective uses `NumberFormat.blank()` to suppress PaperMC sidebar margin numbers.
- Dynamic line updates every 20 ticks (1 second) detailing:
  - Match Time Remaining (`MM:SS`)
  - Territory Control Percentage (`Black %` vs `Gold %`)
  - Team Kill Counters (`Black Kills` vs `Gold Kills`)

---

## ⚙️ 6. Configuration & Vector Reference (`config.yml` & `arena.yml`)

### `config.yml`
Controls match duration, replenish intervals, scoreboard refresh rates, and QA mode flags.

### `arena.yml`
Defines spatial boundaries and team vectors:
- **Floor Y:** `-61`
- **Fall Threshold Y:** `-65`
- **Min X / Max X:** `-43` / `-1`
- **Min Z / Max Z:** `7` / `70`
- **Black Spawn:** `(-21.0, -59.0, 0.0, 0.0, 0.0)`
- **Gold Spawn:** `(-21.0, -59.0, 77.0, 180.0, 0.0)`
