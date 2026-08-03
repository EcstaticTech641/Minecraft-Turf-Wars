package com.ronlab.turfwars.session;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryManager {

    public static void giveCombatKit(Player player, Material teamBlock) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().addItem(new ItemStack(Material.BOW, 1));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 7));
        player.getInventory().addItem(new ItemStack(teamBlock, 7));
    }

    public static void giveReplenishItems(Player player, Material teamBlock) {
        player.getInventory().addItem(new ItemStack(teamBlock, 1));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
    }

    public static void resetHealthAndHunger(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
    }

    public static void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }
}
