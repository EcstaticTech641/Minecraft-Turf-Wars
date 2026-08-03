package com.ronlab.turfwars.listeners;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.session.TurfWarsSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TurfWarsGameListener implements Listener {

    private final TurfWarsCompanion plugin;

    public TurfWarsGameListener(TurfWarsCompanion plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player killer)) return;

        TurfWarsSession session = plugin.getRgaEventListener().getSession(victim.getWorld().getName());
        if (session == null) return;

        if (session.getSpawnProtectedPlayers().contains(victim.getUniqueId())) {
            event.setCancelled(true);
            killer.sendMessage("§c" + victim.getName() + " currently has spawn protection!");
            killer.playSound(killer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            arrow.remove();
            return;
        }

        boolean victimIsGold = session.getGoldTeam().contains(victim.getUniqueId());
        boolean killerIsGold = session.getGoldTeam().contains(killer.getUniqueId());

        if (victimIsGold == killerIsGold) {
            event.setCancelled(true);
            killer.sendMessage("§cYou cannot hurt your teammates!");
            arrow.remove();
            return;
        }

        event.setDamage(1000.0);
        arrow.remove();

        session.handleKill(killer);
        session.handleDeathAndRespawn(victim);
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
                Location pushBack = event.getFrom().clone();
                pushBack.setZ(pushBack.getZ() - 1.0);
                event.setTo(pushBack);
                player.sendMessage("§cYou cannot enter gold team's territory!");
            }
        } else if (session.getGoldTeam().contains(player.getUniqueId())) {
            if (newZ < divideLine) {
                Location pushBack = event.getFrom().clone();
                pushBack.setZ(pushBack.getZ() + 1.0);
                event.setTo(pushBack);
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            session.getSpawnProtectedPlayers().remove(player.getUniqueId());
            session.getPlayers().remove(player.getUniqueId());
            session.getBlackTeam().remove(player.getUniqueId());
            session.getGoldTeam().remove(player.getUniqueId());
        }
    }
}
