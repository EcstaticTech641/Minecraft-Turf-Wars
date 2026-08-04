package com.ronlab.turfwars;

import com.ronlab.turfwars.commands.TurfWarsTestCommand;
import com.ronlab.turfwars.listeners.RgaEventListener;
import com.ronlab.turfwars.listeners.TurfWarsBlockListener;
import com.ronlab.turfwars.listeners.TurfWarsDeathListener;
import com.ronlab.turfwars.listeners.TurfWarsGameListener;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TurfWarsCompanion extends JavaPlugin {

    private static TurfWarsCompanion instance;
    private RgaEventListener rgaEventListener;

    @Override
    public void onEnable() {
        instance = this;

        this.rgaEventListener = new RgaEventListener(this);
        Bukkit.getPluginManager().registerEvents(rgaEventListener, this);
        Bukkit.getPluginManager().registerEvents(new TurfWarsGameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TurfWarsBlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TurfWarsDeathListener(this), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                "turfwars",
                "Turf Wars developer QA test command",
                List.of("tw"),
                new TurfWarsTestCommand(this)
            );
        });

        getLogger().info("TurfWarsCompanion has been enabled cleanly as an RGA Companion Plugin!");
    }

    @Override
    public void onDisable() {
        if (rgaEventListener != null) {
            rgaEventListener.getActiveSessions().values().forEach(session -> session.cleanup());
            rgaEventListener.getActiveSessions().clear();
        }
        instance = null;
        getLogger().info("TurfWarsCompanion has been disabled.");
    }

    public static TurfWarsCompanion getInstance() {
        return instance;
    }

    public RgaEventListener getRgaEventListener() {
        return rgaEventListener;
    }
}
