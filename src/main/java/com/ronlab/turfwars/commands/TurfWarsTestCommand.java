package com.ronlab.turfwars.commands;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.session.TurfWarsSession;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TurfWarsTestCommand implements CommandExecutor {

    private final TurfWarsCompanion plugin;

    public TurfWarsTestCommand(TurfWarsCompanion plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be executed by a player in-game.");
            return true;
        }

        if (!player.hasPermission("turfwars.admin") && !player.isOp()) {
            player.sendMessage("§cYou do not have permission to execute developer test commands.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("forcewin")) {
            TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
            if (session == null) {
                player.sendMessage("§cNo active TurfWars session found in your current world.");
                return true;
            }

            player.sendMessage("§a[QA] Forcing session conclusion for world: " + player.getWorld().getName());
            session.forceWin("ADMIN FORCE WIN (QA Override)");
            return true;
        }

        player.sendMessage("§eTurfWars Developer Commands:");
        player.sendMessage("§e/turfwars forcewin §7- Force conclude active TurfWars session");
        return true;
    }
}
