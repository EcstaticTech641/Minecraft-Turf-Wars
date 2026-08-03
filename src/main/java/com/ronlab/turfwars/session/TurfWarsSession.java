package com.ronlab.turfwars.session;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.util.RgaBridge;
import org.bukkit.Bukkit;
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

                String teamName = blackTeam.contains(uuid) ? "§8BLACK" : "§6GOLD";
                p.sendMessage("§fThe game has started! You are on the " + teamName + " §fteam.");
            }
        }

        this.divideLine = 38.5;
        updateTerritoryBlocks();
        updateScoreboard();

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
        if (concluded) return;

        deathLocations.put(victim.getUniqueId(), victim.getLocation());

        // Delegate spectator state to RGA Session Control
        RgaBridge.setSpectator(victim, true);

        // 5-second spectator delay before respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (concluded || !victim.isOnline()) return;

            RgaBridge.setSpectator(victim, false);

            Location respawnLoc;
            Material teamBlock;

            if (blackTeam.contains(victim.getUniqueId())) {
                respawnLoc = new Location(world, BLACK_X, BLACK_Y, BLACK_Z, 0f, 0f);
                teamBlock = Material.BLACK_WOOL;
            } else {
                respawnLoc = new Location(world, GOLD_X, GOLD_Y, GOLD_Z, 180f, 0f);
                teamBlock = Material.YELLOW_WOOL;
            }

            victim.teleport(respawnLoc);
            InventoryManager.resetHealthAndHunger(victim);
            InventoryManager.giveCombatKit(victim, teamBlock);
            victim.sendMessage("§aYou have respawned!");

            spawnProtectedPlayers.add(victim.getUniqueId());
            victim.sendMessage("§bYou have spawn protection for 3 seconds!");

            // 3-second spawn invincibility
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (spawnProtectedPlayers.remove(victim.getUniqueId())) {
                    if (victim.isOnline()) {
                        victim.sendMessage("§cYour spawn protection has worn off!");
                    }
                }
            }, 60L);

        }, 100L); // 5-second spectator timer
    }

    public void updateTerritoryBlocks() {
        if (world == null) return;

        int MAX_Y = FLOOR_Y + 20;
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                boolean isBlackTerritory = (z < divideLine);
                Location floorLoc = new Location(world, x, FLOOR_Y, z);

                if (isBlackTerritory) {
                    floorLoc.getBlock().setType(Material.BLACK_CONCRETE);
                } else {
                    floorLoc.getBlock().setType(Material.YELLOW_CONCRETE);
                }

                for (int y = FLOOR_Y + 1; y <= MAX_Y; y++) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type == Material.AIR) continue;

                    if (isBlackTerritory) {
                        if (type == Material.YELLOW_WOOL) {
                            block.setType(Material.AIR);
                        }
                    } else {
                        if (type == Material.BLACK_WOOL) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    private void checkWinCondition() {
        // CPMK Solo-Developer QA Guard
        if (initialPlayerCount == 1) {
            plugin.getLogger().info("[CPM] Single-player testing mode detected; suppressing automatic 0-opponent win condition.");
            return;
        }

        if (divideLine >= MAX_Z) {
            concludeGame("BLACK TEAM WINS!", blackTeam);
        } else if (divideLine <= MIN_Z) {
            concludeGame("GOLD TEAM WINS!", goldTeam);
        }
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
        if (mechanicsTask != null) {
            mechanicsTask.cancel();
            mechanicsTask = null;
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
}
