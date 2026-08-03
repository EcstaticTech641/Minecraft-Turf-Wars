package com.ronlab.turfwars.commands;

import com.ronlab.turfwars.TurfWarsCompanion;
import com.ronlab.turfwars.session.TurfWarsSession;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TurfWarsTestCommand implements BasicCommand {

    private final TurfWarsCompanion plugin;

    public TurfWarsTestCommand(TurfWarsCompanion plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be executed by a player in-game.");
            return;
        }

        if (!player.hasPermission("turfwars.admin") && !player.isOp()) {
            player.sendMessage("§cYou do not have permission to execute developer test commands.");
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("forcewin")) {
            TurfWarsSession session = plugin.getRgaEventListener().getSession(player.getWorld().getName());
            if (session == null) {
                player.sendMessage("§cNo active TurfWars session found in your current world.");
                return;
            }

            player.sendMessage("§a[QA] Forcing session conclusion for world: " + player.getWorld().getName());
            session.forceWin("ADMIN FORCE WIN (QA Override)");
            return;
        }

        player.sendMessage("§eTurfWars Developer Commands:");
        player.sendMessage("§e/turfwars forcewin §7- Force conclude active TurfWars session");
    }
}
