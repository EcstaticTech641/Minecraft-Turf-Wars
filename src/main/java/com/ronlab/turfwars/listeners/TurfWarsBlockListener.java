package com.ronlab.turfwars.listeners;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.session.TurfWarsSession;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Dedicated block placement and break guard for Turf Wars sessions.
 *
 * <p>Extracted from TurfWarsGameListener to provide a focused, single-responsibility
 * listener for the block interaction ruleset. Block placement is gated by session
 * membership, GameMode.SURVIVAL state, and territory boundary enforcement. Block
 * breaking is unconditionally cancelled for all active session players.
 */
public class TurfWarsBlockListener implements Listener {

    private final TurfWarsCompanion plugin;

    public TurfWarsBlockListener(TurfWarsCompanion plugin) {
        this.plugin = plugin;
    }

    /**
     * Enforces territory boundary rules for block placement.
     *
     * <p>Priority NORMAL: cancels placement if:
     * <ul>
     *   <li>No active session exists for the player's world.</li>
     *   <li>The player is not in GameMode.SURVIVAL (covers the 1-tick post-respawn window
     *       where GameMode has not yet been restored).</li>
     *   <li>The block's Z coordinate crosses into enemy territory relative to the
     *       current divide line.</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session == null) return;

        // Deny placement if the player's GameMode has not yet been restored to SURVIVAL.
        // This guards the brief window between respawn and the 1-tick SURVIVAL restore task.
        if (player.getGameMode() != GameMode.SURVIVAL) {
            event.setCancelled(true);
            return;
        }

        double blockZ = event.getBlock().getLocation().getZ();
        double divideLine = session.getDivideLine();

        if (session.getBlackTeam().contains(player.getUniqueId())) {
            // Black Team: territory is Z < divideLine (low Z side)
            if (blockZ > divideLine) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot build in enemy territory!");
            }
        } else if (session.getGoldTeam().contains(player.getUniqueId())) {
            // Gold Team: territory is Z > divideLine (high Z side)
            if (blockZ < divideLine) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot build in enemy territory!");
            }
        }
    }

    /**
     * Unconditionally cancels all block break attempts inside an active Turf Wars session.
     *
     * <p>Turf Wars has no block-break mechanic. Wool blocks are removed by the territory
     * sweep in TurfWarsSession.updateTerritoryBlocks() when they cross the divide line,
     * not by manual player mining.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
        if (session != null) {
            event.setCancelled(true);
        }
    }
}
