package com.ronlab.turfwars.util;

import com.ronlab.rga.api.RGASessionControl;
import com.ronlab.rga.api.event.ConcludeResult;
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

    public static ConcludeResult requestSessionConclude(String worldName, String reason, Map<UUID, ? extends Number> scores) {
        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin instanceof RGASessionControl sessionControl) {
            return sessionControl.requestSessionConclude(worldName, reason, scores);
        }
        return ConcludeResult.ERROR;
    }
}
