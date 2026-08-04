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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(victim.getWorld().getName());
        if (session == null) return;

        // Clear drops and XP to prevent arena clutter
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            session.handleKill(killer);
        }

        // Auto-respawn player on next tick to bypass death screen
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline() && victim.isDead()) {
                victim.spigot().respawn();
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session == null) return;

        boolean isBlack = session.getBlackTeam().contains(player.getUniqueId());
        Location spawn = isBlack ?
                new Location(session.getWorld(), TurfWarsSession.BLACK_X, TurfWarsSession.BLACK_Y, TurfWarsSession.BLACK_Z, 0f, 0f) :
                new Location(session.getWorld(), TurfWarsSession.GOLD_X, TurfWarsSession.GOLD_Y, TurfWarsSession.GOLD_Z, 180f, 0f);

        event.setRespawnLocation(spawn);

        // Schedule post-respawn state setup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.setGameMode(GameMode.SURVIVAL);
            session.equipCombatKit(player);
            session.getSpawnProtectedPlayers().add(player.getUniqueId());

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
