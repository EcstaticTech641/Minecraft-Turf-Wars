# Technical Audit & Spatial Report: Team Selection & World Mapping

DOCUMENT ID: RGA-REPORT-2026-TW-SPATIAL  
SUBJECT: Historical Audit, Spatial Coordinate Matrix, and RGA Team Auto-Balancing Architecture for `rga-turfwars`  
SPECIFICATION: ASD-STE100 (Simplified Technical English)  
DATE: August 3, 2026  

---

## 1. Historical Audit Findings (`M:\projects\minecraft-turf-wars\`)

### 1.1 Team Selection & Auto-Balancing Mechanics
- **Legacy Implementation**: The original plugin (`minecraft-turf-wars`) contained **no team selection GUI, item selectors, or team commands**.
- **Assignment Algorithm**: Players were assigned to teams automatically inside `Game.java` (lines 92–117) during match initiation:
  ```java
  for (int i = 0; i < players.size(); i++) {
      UUID uuid = players.get(i);
      Player p = Bukkit.getPlayer(uuid);
      Material teamBlock = (i % 2 == 0) ? Material.BLACK_WOOL : Material.YELLOW_WOOL;

      if (i % 2 == 0) {
          getBlackTeam().add(uuid);
          if (p != null) p.teleport(blackSpawn);
      } else {
          getGoldTeam().add(uuid);
          if (p != null) p.teleport(goldSpawn);
      }
  }
  ```
- **Evaluation**: Team assignment relied on simple round-robin index partitioning (`i % 2 == 0` $\rightarrow$ Black Team, `i % 2 != 0` $\rightarrow$ Gold Team).

---

### 1.2 Spatial Coordinates & Arena Mapping
The legacy code in `Game.java` and `LobbyManager.java` defined fixed spatial bounds and spawn vectors:

| Element | $X$ Coordinate | $Y$ Coordinate | $Z$ Coordinate | Yaw | Pitch | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Black Team Spawn** | `-21.0` | `-59.0` | `0.0` | `0.0°` (North) | `0.0°` | Located behind Black end boundary ($Z=7$) |
| **Gold Team Spawn** | `-21.0` | `-59.0` | `77.0` | `180.0°` (South) | `0.0°` | Located behind Gold end boundary ($Z=70$) |
| **Lobby Spawn** | `2.0` | `-60.0` | `-21.0` | `0.0°` | `0.0°` | Legacy staging lobby location |
| **Territory Floor Y** | — | `-61.0` | — | — | — | Solid concrete floor level |
| **Arena Min Bound** | `-43.0` | `-61.0` | `7.0` | — | — | West/Southwest arena boundary |
| **Arena Max Bound** | `-1.0` | `-41.0` | `70.0` | — | — | East/Northeast arena boundary |
| **Initial Divide Line**| — | — | `38.5` | — | — | Midpoint line ($Z = 38.5$) |

#### Root Cause: Solo Player Teleportation Bug
When RGA provisions procedural minigame worlds (`minigame_<id>_<hash>`), Paper defaults player teleports to the world's default spawn point before `MinigameStartEvent` fires. If `MinigameStartEvent` setup is delayed or interrupted by an improperly placed `initialPlayerCount == 1` guard in `startSession()`, players remain stranded at the default world spawn outside the active platform ($X \in [-43, -1], Z \in [7, 70]$).

---

### 1.3 World Infrastructure Audit
- **`/lobby/` World**: Staging lobby for manual queueing via `/turfwars join`. Discarded in RGA integration because RGA Central Hub handles party queueing.
- **`/template_world/`**: Dedicated Turf Wars arena world containing the concrete platform ($Y=-61$, $X \in [-43, -1]$, $Z \in [7, 70]$). Moved to RGA template directory (`templates/turfwars/`).

---

## 2. RGA Ecosystem Integration Strategy

### 2.1 RGA-Native Auto-Balancing Architecture
To maintain zero UI friction and align with RGA Party routing:
1. `MinigameStartEvent` supplies the list of participating player UUIDs (`event.getPlayerUuids()`).
2. Companion plugin splits the list round-robin upon match initiation:
   - Index $i = 0, 2, 4 \dots \rightarrow$ **Black Team** (`BLACK_WOOL`).
   - Index $i = 1, 3, 5 \dots \rightarrow$ **Gold Team** (`YELLOW_WOOL`).
3. Odd player counts assign the extra player to Black Team.

---

### 2.2 Immediate Teleportation Protocol
To eliminate default world spawn stranded bugs:
1. `startSession()` executes immediately on `MinigameStartEvent`.
2. For each assigned player, `startSession()` calls `player.teleport(blackSpawn)` or `player.teleport(goldSpawn)` on tick 0.
3. Overrides default Paper world spawn before players render world terrain.

---

### 2.3 Solo QA Testing Behavior (Win-Check Fix)
To support single-developer testing passes:
1. The `initialPlayerCount == 1` guard must reside **strictly inside `checkWinCondition()`**:
   ```java
   public void checkWinCondition() {
       if (concluded) return;

       // CPMK Solo-Developer QA Guard
       if (initialPlayerCount == 1) {
           plugin.getLogger().info("[CPM] Single-player testing mode detected; suppressing automatic 0-opponent win condition.");
           return;
       }

       if (blackTeam.isEmpty() && !goldTeam.isEmpty()) {
           concludeGame("GOLD TEAM WINS (Opponents Disconnected)!", goldTeam);
           return;
       } else if (goldTeam.isEmpty() && !blackTeam.isEmpty()) {
           concludeGame("BLACK TEAM WINS (Opponents Disconnected)!", blackTeam);
           return;
       }

       if (divideLine >= MAX_Z) {
           concludeGame("BLACK TEAM WINS!", blackTeam);
       } else if (divideLine <= MIN_Z) {
           concludeGame("GOLD TEAM WINS!", goldTeam);
       }
   }
   ```
2. **Behavior**: `startSession()` completes fully for 1-player sessions (kits given, spawn teleported, mechanics active). Automatic win check is suppressed, allowing developer to test boundaries, wool placement, and execute `/turfwars forcewin` to conclude.

---

## 3. Code Adjustments for Companion Classes

### 3.1 `TurfWarsSession.java` Adjustments
```java
public void startSession() {
    blackTeam.clear();
    goldTeam.clear();

    Location blackSpawn = new Location(world, BLACK_X, BLACK_Y, BLACK_Z, 0f, 0f);
    Location goldSpawn = new Location(world, GOLD_X, GOLD_Y, GOLD_Z, 180f, 0f);

    for (int i = 0; i < players.size(); i++) {
        UUID uuid = players.get(i);
        Player p = Bukkit.getPlayer(uuid);
        Material teamBlock = (i % 2 == 0) ? Material.BLACK_WOOL : Material.YELLOW_WOOL;

        if (i % 2 == 0) {
            blackTeam.add(uuid);
            if (p != null) p.teleport(blackSpawn);
        } else {
            goldTeam.add(uuid);
            if (p != null) p.teleport(goldSpawn);
        }

        if (p != null) {
            p.setScoreboard(scoreboard);
            InventoryManager.clearInventory(p);
            InventoryManager.resetHealthAndHunger(p);
            InventoryManager.giveCombatKit(p, teamBlock);
            p.sendMessage("§fThe game has started!");
        }
    }

    this.divideLine = 38.5;
    updateTerritoryBlocks();
    updateScoreboard();
}
```

### 3.2 `TurfWarsGameListener.java` Adjustments
```java
@EventHandler
public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
    if (session == null) return;

    double newZ = event.getTo().getZ();
    double divideLine = session.getDivideLine();

    if (session.getBlackTeam().contains(player.getUniqueId())) {
        if (newZ > divideLine) {
            event.setCancelled(true);
            player.setVelocity(new Vector(0, 0.15, -0.4));
            player.sendMessage("§cYou cannot enter gold team's territory!");
        }
    } else if (session.getGoldTeam().contains(player.getUniqueId())) {
        if (newZ < divideLine) {
            event.setCancelled(true);
            player.setVelocity(new Vector(0, 0.15, 0.4));
            player.sendMessage("§cYou cannot enter black team's territory!");
        }
    }
}
```

---

## 4. Server Setup Guide: Template World Configuration

To deploy the `turfwars` template world into RGA:

1. **Template Folder Path**:
   Place the template world directory at `templates/turfwars/` within the RGA root server path (`M:\projects\ronlabgameassistant\templates\turfwars\`).

2. **World Properties & Metadata**:
   - Ensure `level.dat` and region files match platform bounds:
     - Platform Floor: $Y = -61$, $X \in [-43, -1]$, $Z \in [7, 70]$.
     - Black Spawn: $X = -21.0, Y = -59.0, Z = 0.0, \text{Yaw} = 0.0^\circ$.
     - Gold Spawn: $X = -21.0, Y = -59.0, Z = 77.0, \text{Yaw} = 180.0^\circ$.

3. **`minigames.yml` Entry**:
   ```yaml
   turfwars:
     display-name: "Turf Wars"
     template-world: "turfwars"
     min-players: 1
     max-players: 16
     allow-spectators: true
     gamerules:
       doDaylightCycle: false
       doWeatherCycle: false
       keepInventory: true
       doImmediateRespawn: true
   ```

4. **Verification**:
   Execute `/party start turfwars` in RGA Hub. Verify team auto-balancing and immediate spawn teleportation.
