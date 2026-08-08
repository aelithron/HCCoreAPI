package com.hackclub.hccoreapi;

import io.javalin.Javalin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class HCCoreAPI extends JavaPlugin {
    private Javalin app;
    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        int port = getConfig().getInt("port", 7600);
        app = Javalin.create(config -> {
            config.routes.get("/player", ctx -> {

            });
        });
        getLogger().log(Level.INFO, "Starting HCCore-API server (on port " + port + ")...");
        app.start(port);
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Shutting down HCCore-API server...");
        app.stop();
    }
}
