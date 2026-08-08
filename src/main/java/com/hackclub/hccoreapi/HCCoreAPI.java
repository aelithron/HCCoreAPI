package com.hackclub.hccoreapi;

import com.hackclub.hccoreapi.DataTypes.Nickname;
import com.hackclub.hccoreapi.DataTypes.PlayerInfo;
import io.javalin.Javalin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
                List<PlayerInfo> list = new ArrayList<>();
                list.add(new PlayerInfo(UUID.fromString("eb7ea62d-b7aa-4d6e-b68a-d7e948780f03"), "U08RJ1PEM7X", new Nickname("Nova", "#E6E6FA")));
                ctx.json(list);
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
