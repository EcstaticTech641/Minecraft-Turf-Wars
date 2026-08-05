# rga-turfwars 👾

![Language](https://img.shields.io/badge/Language-Java_25-orange.svg)
![Platform](https://img.shields.io/badge/Platform-Paper_26.2-blue.svg)
![Framework](https://img.shields.io/badge/Framework-RGA_CPMK-purple.svg)

`rga-turfwars` is the official companion minigame plugin for **Turf Wars** inside the **Ronlab Game Assistant (RGA)** ecosystem.

---

## 📖 About the Minigame

Turf Wars is a fast-paced, team-based tug-of-war combat game. Two teams (**Black Team** vs **Gold Team**) battle in a rectangular arena where the ground itself represents their territory score.

- **One-Shot Bow Combat:** Every bow hit or melee strike results in an instant kill and advances your team's territory line forward.
- **Dynamic Turf Advancement:** Eliminating opponents shifts the territory division line ($Z$-axis) into enemy territory.
- **Building & Wool Replenishment:** Players receive wool blocks matching their team color to build defenses, automatically replenished every 5 seconds.
- **RGA Integration:** World instantiation, party routing, spectator handling, and match teardowns are managed via the decoupled `com.ronlab:rga-api` event bus.

---

## 🏛️ CPMK Architecture & Core Directives

`rga-turfwars` adheres strictly to the 5 CPMK Pillars established in `rga-core`:

1. **Core Gameplay Function Retention:** Preserves 100% of native Turf Wars rules, game loops, dynamic step sizing, and scoreboards.
2. **Ronlab Integration Standard:** Listens for `MinigameStartEvent` and `MinigameConcludeEvent` from `com.ronlab.rga.api.event.*`. Declares `api-version: '26.2'` and server dependency on `RonlabGameAssistant` (`required: true`, `join-classpath: true`).
3. **Baseline Structure & Rules Provision:** Applies PaperMC's `objective.numberFormat(NumberFormat.blank())` to suppress margin numbers, assigns scoreboards during post-teleport spawn phases (10L/20L), and restores `getMainScoreboard()` upon session teardown.
4. **Companion-Type Agnostic Design:** Operates independently of `rga-core` internal classes, communicating exclusively through the public `rga-api` event bus.
5. **Feature Implementation & Modification Specs:** Fully documents local administrative commands, configuration files (`config.yml`, `arena.yml`), and Solo QA Developer Mode.

---

## 🧪 Solo QA Developer Mode (`initialPlayerCount == 1`)

When a match is launched with a single player (`initialPlayerCount == 1`), Turf Wars enters **Solo QA Developer Mode**:
- **Win Condition Freeze:** Automatic zero-opponent victory triggers are frozen.
- **Continuous QA:** Single developers can continuously test arena boundaries, spawn positions, wool replenishment, and line calculations.
- **Force Win Test:** Programmatic conclusion can be tested anytime via `/turfwars forcewin`.

---

## 🛠️ Commands & Permissions

- **Command:** `/turfwars forcewin` (Alias: `/tw forcewin`)
  - **Permission:** `turfwars.admin` (or Server OP)
  - **Description:** Programmatically concludes the active Turf Wars session in the player's world for QA testing.

---

## 📁 Configuration & Resources

- `src/main/resources/paper-plugin.yml`: PaperMC descriptor specifying Java 25, Paper 26.2, and `RonlabGameAssistant` dependency.
- `src/main/resources/config.yml`: General match configuration, durations, replenish intervals, and QA flags.
- `src/main/resources/arena.yml`: Spatial boundary specs, spawn vectors, floor coordinates (`Y: -61`), fall thresholds (`Y: -65`), and material mapping.
- `USER_GUIDE.md`: Detailed developer and operator specification guide.

---

## ⚙️ Compilation & Deployment

### 1. Build via Maven:
```bash
mvn clean package
```
The compiled jar will be generated at `target/rga-turfwars-1.0.0-SNAPSHOT.jar`.

### 2. Deployment & Setup:
1. Copy `rga-turfwars-1.0.0-SNAPSHOT.jar` to your Paper server's `plugins/` directory alongside `RonlabGameAssistant.jar`.
2. Copy the template world directory to `plugins/RonlabGameAssistant/templates/minigame_turfwars/`.
3. Register the minigame in `plugins/RonlabGameAssistant/config.yml`:
   ```yaml
   minigames:
     turfwars:
       display-name: "Turf Wars"
       min-players: 2
       max-players: 16
       template-world: "minigame_turfwars"
       allow-spectators: true
   ```
