package com.ronlab.turfwars.session;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.util.RgaBridge;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TurfWarsSession {

    private final TurfWarsCompanion plugin;
    private final World world;
    private final List<UUID> players;
    private final int initialPlayerCount;

    private double divideLine = 38.5;
    private static final int FLOOR_Y = -61;
    private static final int MIN_X = -43;
    private static final int MAX_X = -1;
    private static final int MIN_Z = 7;
    private static final int MAX_Z = 70;

    public static final double BLACK_X = -21, BLACK_Y = -59, BLACK_Z = 0;
    public static final double GOLD_X = -21, GOLD_Y = -59, GOLD_Z = 77;

    private final List<UUID> blackTeam = new CopyOnWriteArrayList<>();
    private final List<UUID> goldTeam = new CopyOnWriteArrayList<>();

    private final Map<UUID, Location> deathLocations = new HashMap<>();
    private final Set<UUID> spawnProtectedPlayers = new HashSet<>();

    private final Set<BukkitTask> activeTasks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<UUID> teleporting = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private long sessionStartTimeMs;
    private Scoreboard scoreboard;
    private Objective objective;
    private BukkitTask mechanicsTask;
    private boolean concluded = false;

    public TurfWarsSession(TurfWarsCompanion plugin, World world, List<UUID> playerUuids) {
        this.plugin = plugin;
        this.world = world;
        this.players = new ArrayList<>(playerUuids);
        this.initialPlayerCount = playerUuids.size();
        setupScoreboard();
    }

    private void setupScoreboard() {
        if (Bukkit.getScoreboardManager() != null) {
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            this.objective = scoreboard.registerNewObjective("turfwars", "dummy", "§b§lTURF WARS");
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    public void startSession() {
        this.sessionStartTimeMs = System.currentTimeMillis();
        blackTeam.clear();
        goldTeam.clear();

        for (int i = 0; i < players.size(); i++) {
            UUID uuid = players.get(i);
            if (i % 2 == 0) {
                blackTeam.add(uuid);
            } else {
                goldTeam.add(uuid);
            }
        }

        this.divideLine = 38.5;
        updateTerritoryBlocks();
        updateScoreboard();

        // Enforce team spawns & give kits at 10L (0.5s)
        BukkitTask initTask10 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (concluded) return;
            teleportAllToSpawns();
            equipAllCombatKits();
        }, 10L);
        activeTasks.add(initTask10);

        // Re-enforce team spawns at 20L (1.0s) - kit distribution is idempotent and won't duplicate items
        BukkitTask initTask20 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (concluded) return;
            teleportAllToSpawns();
        }, 20L);
        activeTasks.add(initTask20);

        // Start replenishing items task every 5 seconds (100 ticks)
        this.mechanicsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (concluded) return;
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.getWorld().equals(world)) {
                    Material teamBlock = blackTeam.contains(uuid) ? Material.BLACK_WOOL : Material.YELLOW_WOOL;
                    InventoryManager.giveReplenishItems(p, teamBlock);
                }
            }
        }, 0L, 100L);
        activeTasks.add(mechanicsTask);
    }

    public void teleportPlayerToSpawn(Player p) {
        if (concluded || p == null || !p.isOnline()) return;

        Location blackSpawn = new Location(world, BLACK_X, BLACK_Y, BLACK_Z, 0f, 0f);
        Location goldSpawn = new Location(world, GOLD_X, GOLD_Y, GOLD_Z, 180f, 0f);

        boolean isBlack = blackTeam.contains(p.getUniqueId());
        Location spawn = isBlack ? blackSpawn : goldSpawn;

        teleporting.add(p.getUniqueId());
        try {
            p.teleport(spawn);
            p.setScoreboard(scoreboard);
        } finally {
            Bukkit.getScheduler().runTaskLater(plugin, () -> teleporting.remove(p.getUniqueId()), 1L);
        }
    }

    public void teleportAllToSpawns() {
        if (concluded) return;
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                teleportPlayerToSpawn(p);
            }
        }
    }

    public void equipCombatKit(Player p) {
        if (concluded || p == null || !p.isOnline()) return;

        // Idempotency check: skip item distribution if player already has team wool kit
        if (p.getInventory().contains(Material.BLACK_WOOL) || p.getInventory().contains(Material.YELLOW_WOOL)) {
            return;
        }

        boolean isBlack = blackTeam.contains(p.getUniqueId());
        Material teamBlock = isBlack ? Material.BLACK_WOOL : Material.YELLOW_WOOL;

        InventoryManager.clearInventory(p);
        InventoryManager.resetHealthAndHunger(p);
        InventoryManager.giveCombatKit(p, teamBlock);

        String teamName = isBlack ? "§8BLACK" : "§6GOLD";
        p.sendMessage("§fThe game has started! You are on the " + teamName + " §fteam.");
    }

    public void equipAllCombatKits() {
        if (concluded) return;
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                equipCombatKit(p);
            }
        }
    }

    public boolean isTeleporting(UUID playerUuid) {
        return teleporting.contains(playerUuid);
    }

    public void updateScoreboard() {
        if (scoreboard == null || objective == null) return;

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        double totalLength = MAX_Z - MIN_Z;
        double blackControl = divideLine - MIN_Z;

        int blackPercent = (int) Math.max(0, Math.min(100, (blackControl / totalLength) * 100));
        int goldPercent = 100 - blackPercent;

        objective.getScore(" ").setScore(6);
        objective.getScore("§fMap Control:").setScore(5);
        objective.getScore("§8Black: §7" + blackPercent + "%").setScore(4);
        objective.getScore("§6Gold: §e" + goldPercent + "%").setScore(3);
        objective.getScore("  ").setScore(2);
        objective.getScore("§fPlayers: §a" + players.size()).setScore(1);
        objective.getScore(" ").setScore(0);
    }

    public void handleKill(Player killer) {
        if (concluded) return;

        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);

        double moveStep;
        if (players.size() > 8) {
            moveStep = 1.0;
        } else if (players.size() > 5) {
            moveStep = 2.0;
        } else {
            moveStep = 3.0;
        }

        if (blackTeam.contains(killer.getUniqueId())) {
            divideLine += moveStep;
        } else {
            divideLine -= moveStep;
        }

        updateTerritoryBlocks();
        updateScoreboard();
        checkWinCondition();
    }

    public void handleDeathAndRespawn(Player victim) {
        if (concluded || victim == null || !victim.isOnline()) return;

        deathLocations.put(victim.getUniqueId(), victim.getLocation());

        // Use local spectator mode (do NOT invoke RGA setSpectator API to avoid Hub teleportation)
        victim.setGameMode(GameMode.SPECTATOR);

        // 5-second spectator delay before respawn
        BukkitTask respawnTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (concluded || !victim.isOnline()) return;

            victim.setGameMode(GameMode.SURVIVAL);
            teleportPlayerToSpawn(victim);

            boolean isBlack = blackTeam.contains(victim.getUniqueId());
            Material teamBlock = isBlack ? Material.BLACK_WOOL : Material.YELLOW_WOOL;

            InventoryManager.clearInventory(victim);
            InventoryManager.resetHealthAndHunger(victim);
            InventoryManager.giveCombatKit(victim, teamBlock);
            victim.sendMessage("§aYou have respawned!");

            spawnProtectedPlayers.add(victim.getUniqueId());
            victim.sendMessage("§bYou have spawn protection for 3 seconds!");

            // 3-second spawn invincibility
            BukkitTask protectionTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (spawnProtectedPlayers.remove(victim.getUniqueId())) {
                    if (victim.isOnline()) {
                        victim.sendMessage("§cYour spawn protection has worn off!");
                    }
                }
            }, 60L);
            activeTasks.add(protectionTask);

        }, 100L); // 5-second spectator timer
        activeTasks.add(respawnTask);
    }

    public void updateTerritoryBlocks() {
        if (world == null) return;

        // 1. Update floor concrete blocks across X: [-43..-1] and Z: [7..70] at Y = FLOOR_Y
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                boolean isBlackTerritory = (z < divideLine);
                org.bukkit.block.Block block = world.getBlockAt(x, FLOOR_Y, z);
                Material targetFloor = isBlackTerritory ? Material.BLACK_CONCRETE : Material.YELLOW_CONCRETE;
                if (block.getType() != targetFloor) {
                    block.setType(targetFloor);
                }
            }
        }

        // 2. Clear invalid wool blocks in enemy territory (Y: FLOOR_Y + 1 to FLOOR_Y + 8)
        int maxWoolY = FLOOR_Y + 8;
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                boolean isBlackTerritory = (z < divideLine);
                for (int y = FLOOR_Y + 1; y <= maxWoolY; y++) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type == Material.AIR) continue;

                    if (isBlackTerritory && type == Material.YELLOW_WOOL) {
                        block.setType(Material.AIR);
                    } else if (!isBlackTerritory && type == Material.BLACK_WOOL) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

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

    public void forceWin(String reason) {
        if (concluded) return;
        List<UUID> winners = blackTeam.isEmpty() ? goldTeam : blackTeam;
        concludeGame(reason, winners);
    }

    private void concludeGame(String winMessage, List<UUID> winningTeam) {
        if (concluded) return;
        concluded = true;

        Map<UUID, Number> scores = new HashMap<>();
        for (UUID uuid : players) {
            if (winningTeam.contains(uuid)) {
                scores.put(uuid, 100);
            } else {
                scores.put(uuid, 0);
            }
        }

        RgaBridge.requestSessionConclude(world.getName(), winMessage, scores);
    }

    public void cleanup() {
        concluded = true;
        activeTasks.forEach(task -> {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        });
        activeTasks.clear();
        mechanicsTask = null;

        if (scoreboard != null) {
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        }

        spawnProtectedPlayers.clear();
        deathLocations.clear();
    }

    // Getters & Query Methods
    public World getWorld() { return world; }
    public double getDivideLine() { return divideLine; }
    public List<UUID> getBlackTeam() { return blackTeam; }
    public List<UUID> getGoldTeam() { return goldTeam; }
    public List<UUID> getPlayers() { return players; }
    public Set<UUID> getSpawnProtectedPlayers() { return spawnProtectedPlayers; }
    public int getInitialPlayerCount() { return initialPlayerCount; }
    public long getSessionStartTimeMs() { return sessionStartTimeMs; }
}
