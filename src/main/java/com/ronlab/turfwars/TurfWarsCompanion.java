package com.ronlab.turfwars;

import com.ronlab.turfwars.listeners.RgaEventListener;
import com.ronlab.turfwars.listeners.TurfWarsGameListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TurfWarsCompanion extends JavaPlugin {

    private static TurfWarsCompanion instance;
    private RgaEventListener rgaEventListener;

    @Override
    public void onEnable() {
        instance = this;

        if (!Bukkit.getPluginManager().isPluginEnabled("RonlabGameAssistant")) {
            getLogger().severe("[TurfWars] RonlabGameAssistant plugin is NOT enabled. Disabling TurfWarsCompanion.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.rgaEventListener = new RgaEventListener(this);
        Bukkit.getPluginManager().registerEvents(rgaEventListener, this);
        Bukkit.getPluginManager().registerEvents(new TurfWarsGameListener(this), this);

        getLogger().info("TurfWarsCompanion has been enabled cleanly as an RGA Companion Plugin!");
    }

    @Override
    public void onDisable() {
        if (rgaEventListener != null) {
            rgaEventListener.getActiveSessions().values().forEach(session -> session.cleanup());
            rgaEventListener.getActiveSessions().clear();
        }
        getLogger().info("TurfWarsCompanion has been disabled.");
    }

    public static TurfWarsCompanion getInstance() {
        return instance;
    }

    public RgaEventListener getRgaEventListener() {
        return rgaEventListener;
    }
}
