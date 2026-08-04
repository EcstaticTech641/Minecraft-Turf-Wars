package com.ronlab.turfwars.listeners;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.session.TurfWarsSession;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class TurfWarsDeathListener implements Listener {

    private final TurfWarsCompanion plugin;

    public TurfWarsDeathListener(TurfWarsCompanion plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player death inside an active Turf Wars session.
     *
     * <p>Priority HIGH: fires after rga-core CorePlayerDeathListener (LOWEST), which yields
     * full authority to companion plugins for active sessions. Suppresses vanilla death
     * messages, item drops, and XP spawns. Awards a kill credit to the attacker when a
     * distinct Player delivered the final blow. Schedules a 1-tick auto-respawn to
     * immediately route the player into the PlayerRespawnEvent pipeline.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(victim.getWorld().getName());
        if (session == null) return;

        // Suppress vanilla death message, item drops, and XP orb spawns
        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Award kill credit only when a distinct player delivered the final blow
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            session.handleKill(killer);
        }

        // Schedule 1-tick auto-respawn to route the victim into PlayerRespawnEvent
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline() && victim.isDead()) {
                victim.spigot().respawn();
            }
        }, 1L);
    }

    /**
     * Handles post-death respawn routing inside an active Turf Wars session.
     *
     * <p>Priority HIGHEST: must fire after rga-core MinigameWorldListener (also HIGHEST),
     * which is registered first and sets the respawn location to the world-spawn fallback.
     * By registering at the same priority after rga-core, this handler correctly overrides
     * that fallback with the player's team spawn coordinates. Using HIGH would allow
     * MinigameWorldListener to silently reset the location after this handler runs.
     *
     * <p>A 1-tick delayed task after the respawn event restores GameMode.SURVIVAL, issues
     * the combat kit, and activates 3-second spawn protection.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session == null) return;

        // Resolve team spawn coordinates
        boolean isBlack = session.getBlackTeam().contains(player.getUniqueId());
        Location teamSpawn = isBlack
                ? new Location(session.getWorld(), TurfWarsSession.BLACK_X, TurfWarsSession.BLACK_Y, TurfWarsSession.BLACK_Z, 0f, 0f)
                : new Location(session.getWorld(), TurfWarsSession.GOLD_X, TurfWarsSession.GOLD_Y, TurfWarsSession.GOLD_Z, 180f, 0f);

        event.setRespawnLocation(teamSpawn);

        // Restore combat state on the tick following respawn completion
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Restore GameMode.SURVIVAL — this is the earliest safe point to set game mode
            // post-respawn and is the point at which block placement is re-enabled.
            player.setGameMode(GameMode.SURVIVAL);
            session.equipCombatKit(player);

            session.getSpawnProtectedPlayers().add(player.getUniqueId());
            player.sendMessage("§bYou have spawn protection for 3 seconds!");

            // Expire spawn protection after 3 seconds (60 ticks)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (session.getSpawnProtectedPlayers().remove(player.getUniqueId())) {
                    if (player.isOnline()) {
                        player.sendMessage("§cYour spawn protection has worn off!");
                    }
                }
            }, 60L);
        }, 1L);
    }
}
