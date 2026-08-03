package com.ronlab.turfwars.listeners;

import com.ronlab.rga.api.event.MinigameConcludeEvent;
import com.ronlab.rga.api.event.MinigameStartEvent;
import com.ronlab.rga.api.model.MinigameId;
import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.session.TurfWarsSession;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RgaEventListener implements Listener {

    private static final MinigameId GAME_ID = MinigameId.of("ronlab", "turfwars");
    private final TurfWarsCompanion plugin;
    private final Map<String, TurfWarsSession> activeSessions = new ConcurrentHashMap<>();

    public RgaEventListener(TurfWarsCompanion plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMinigameStart(MinigameStartEvent event) {
        if (!isTurfWarsGame(event.getMinigameId())) {
            return;
        }

        String worldName = event.getWorldName();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().severe("[TurfWars] MinigameStartEvent fired for missing world: " + worldName);
            return;
        }

        TurfWarsSession session = new TurfWarsSession(plugin, world, event.getPlayerUuids());
        activeSessions.put(worldName, session);
        session.startSession();

        plugin.getLogger().info("[TurfWars] Started session in world: " + worldName + " with " + event.getPlayerUuids().size() + " players.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMinigameConclude(MinigameConcludeEvent event) {
        if (!isTurfWarsGame(event.getMinigameId())) {
            return;
        }

        String worldName = event.getWorldName();
        TurfWarsSession session = activeSessions.remove(worldName);

        if (session != null) {
            session.cleanup();
            plugin.getLogger().info("[TurfWars] Cleaned up session for world: " + worldName);
        }
    }

    private boolean isTurfWarsGame(String minigameIdStr) {
        if (minigameIdStr == null) return false;
        if (minigameIdStr.equalsIgnoreCase(GAME_ID.asString())) return true;
        if (minigameIdStr.equalsIgnoreCase(GAME_ID.key())) return true;
        return minigameIdStr.equalsIgnoreCase("turf_wars");
    }

    public TurfWarsSession getSession(String worldName) {
        return activeSessions.get(worldName);
    }

    public Map<String, TurfWarsSession> getActiveSessions() {
        return activeSessions;
    }
}
