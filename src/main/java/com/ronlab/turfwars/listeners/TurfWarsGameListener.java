package com.ronlab.turfwars.listeners;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.session.TurfWarsSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class TurfWarsGameListener implements Listener {

    private final TurfWarsCompanion plugin;
    private final java.util.Set<java.util.UUID> bypassGuard = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public TurfWarsGameListener(TurfWarsCompanion plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session == null) return;

        // Ignore teleports initiated by TurfWarsSession or listener bypass guard
        if (session.isTeleporting(player.getUniqueId()) || bypassGuard.contains(player.getUniqueId())) {
            return;
        }

        long elapsedMs = System.currentTimeMillis() - session.getSessionStartTimeMs();
        if (elapsedMs <= 3000) {
            org.bukkit.Location to = event.getTo();
            if (to == null) return;

            double x = to.getX();
            double z = to.getZ();

            if (x < -43.0 || x > -1.0 || z < 7.0 || z > 70.0) {
                plugin.getLogger().warning("[TurfWars] Intercepted external overwrite teleport for " + player.getName());
                event.setCancelled(true);

                bypassGuard.add(player.getUniqueId());
                try {
                    session.teleportPlayerToSpawn(player);
                } finally {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> bypassGuard.remove(player.getUniqueId()), 1L);
                }
            }
        }
    }

    /*
     * PROJECT MANAGEMENT NOTE: August 4, 2026.
     * Environmental damage (fall, fire, drowning) is intentionally cancelled.
     * The core gameplay loop uses arrows, wool, borders, and kits.
     * This decision restricts future map designs to arrow-only lethal damage.
     * Accepted by Project Management.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // Arrow damage (EntityDamageByEntityEvent) passes through untouched — vanilla
        // processes it, reduces HP to zero, and fires PlayerDeathEvent, which is
        // handled by TurfWarsDeathListener.onPlayerDeath.
        if (event instanceof EntityDamageByEntityEvent) return;

        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session == null) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            // Void damage passes through naturally — vanilla zeroes the player's HP,
            // firing PlayerDeathEvent. TurfWarsDeathListener handles the respawn routing.
            // No kill credit is awarded (killer is null for environmental deaths).
            return;
        }

        // Cancel all other environmental damage (fall, fire, drowning, suffocation).
        // Only arrows are lethal in Turf Wars. See PM note above.
        event.setCancelled(true);
    }

    /**
     * Enforces spawn protection and prevents friendly fire for arrow damage.
     *
     * <p>Priority HIGH: runs before the HIGHEST environmental-damage handler above so
     * arrow events are intercepted here and never reach general cancellation logic.
     * The strict early-return chain prevents NPE / ClassCastException on non-player
     * projectiles (e.g. Skeletons, dispensers) that also fire EntityDamageByEntityEvent.
     *
     * <p>Two guards are applied in this order:
     * <ol>
     *   <li>Spawn protection — cancels damage and notifies attacker when the victim is
     *       still inside their post-respawn protection window.</li>
     *   <li>Friendly fire — cancels damage and notifies attacker when both players
     *       share the same team.</li>
     * </ol>
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Guard 1: victim must be a Player
        if (!(event.getEntity() instanceof Player victim)) return;

        // Guard 2: damager must be an Arrow
        if (!(event.getDamager() instanceof Arrow arrow)) return;

        // Guard 3: shooter must be a Player (rules out skeletons, dispensers, etc.)
        if (!(arrow.getShooter() instanceof Player attacker)) return;

        // Guard 4: an active Turf Wars session must exist in the victim's world
        TurfWarsSession session = plugin.getRgaEventListener().getSession(victim.getWorld().getName());
        if (session == null) return;

        // Spawn protection check — takes priority over team check
        if (session.getSpawnProtectedPlayers().contains(victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage("§cTarget has spawn protection!");
            return;
        }

        // Friendly fire check — cancel damage between teammates
        String attackerTeam = session.getTeam(attacker);
        String victimTeam   = session.getTeam(victim);
        if (attackerTeam.equals(victimTeam)) {
            event.setCancelled(true);
            attacker.sendMessage("§cYou cannot damage your teammates!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session == null) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

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

    @EventHandler
    public void onArrowHitGround(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        TurfWarsSession session = plugin.getRgaEventListener().getSession(shooter.getWorld().getName());
        if (session == null) return;

        if (event.getHitBlock() != null) {
            Block hitBlock = event.getHitBlock();
            Material blockType = hitBlock.getType();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (blockType == Material.BLACK_WOOL || blockType == Material.YELLOW_WOOL) {
                    hitBlock.setType(Material.AIR);
                }
                if (!arrow.isDead()) {
                    arrow.remove();
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            session.getSpawnProtectedPlayers().remove(player.getUniqueId());
            session.getPlayers().remove(player.getUniqueId());
            session.getBlackTeam().remove(player.getUniqueId());
            session.getGoldTeam().remove(player.getUniqueId());
            session.checkWinCondition();
        }
    }
}
