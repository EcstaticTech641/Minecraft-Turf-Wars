package com.ronlab.turfwars.util;

import com.ronlab.rga.api.RGASessionControl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

public class RgaBridge {

    public static void setSpectator(Player player, boolean isSpectator) {
        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin instanceof RGASessionControl sessionControl) {
            sessionControl.setSpectator(player, isSpectator);
        }
    }

    public static void requestSessionConclude(String worldName, String reason, Map<UUID, ? extends Number> scores) {
        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin != null) {
            try {
                rgaPlugin.getClass()
                        .getMethod("requestSessionConclude", String.class, String.class, Map.class)
                        .invoke(rgaPlugin, worldName, reason, scores);
            } catch (Exception e) {
                Bukkit.getLogger().severe("[TurfWars] Failed to invoke requestSessionConclude on RGA: " + e.getMessage());
            }
        }
    }
}
