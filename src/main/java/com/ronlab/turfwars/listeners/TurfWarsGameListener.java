package com.ronlab.turfwars.listeners;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.session.TurfWarsSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player killer)) return;

        TurfWarsSession session = plugin.getRgaEventListener().getSession(victim.getWorld().getName());
        if (session == null) return;

        // Cancel vanilla damage event to bypass vanilla death screens & item drops
        event.setCancelled(true);

        if (session.getSpawnProtectedPlayers().contains(victim.getUniqueId())) {
            killer.sendMessage("§c" + victim.getName() + " currently has spawn protection!");
            killer.playSound(killer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            arrow.remove();
            return;
        }

        boolean victimIsGold = session.getGoldTeam().contains(victim.getUniqueId());
        boolean killerIsGold = session.getGoldTeam().contains(killer.getUniqueId());

        if (victimIsGold == killerIsGold) {
            killer.sendMessage("§cYou cannot hurt your teammates!");
            arrow.remove();
            return;
        }

        arrow.remove();
        session.handleKill(killer);
        session.handleDeathAndRespawn(victim);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event instanceof EntityDamageByEntityEvent) return; // Processed in onArrowHit

        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session == null) return;

        event.setCancelled(true);

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            session.handleDeathAndRespawn(player);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session == null) return;

        double blockZ = event.getBlock().getLocation().getZ();
        double divideLine = session.getDivideLine();

        if (session.getBlackTeam().contains(player.getUniqueId())) {
            if (blockZ > divideLine) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot build in enemy territory!");
            }
        } else if (session.getGoldTeam().contains(player.getUniqueId())) {
            if (blockZ < divideLine) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot build in enemy territory!");
            }
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
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            event.setCancelled(true);
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
