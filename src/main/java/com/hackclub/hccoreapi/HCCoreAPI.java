package com.hackclub.hccoreapi;

import com.hackclub.hccore.HCCorePlugin;
import com.hackclub.hccore.PlayerData;
import com.hackclub.hccoreapi.DataTypes.*;
import io.javalin.Javalin;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class HCCoreAPI extends JavaPlugin {
    private Javalin app;
    KeyManager keys;
    RateLimitManager limits;
    private HCCorePlugin hccore;
    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        int port = getConfig().getInt("port", 7600);
        getCommand("webapi").setExecutor(new ManageCommand(this));
        getCommand("webapi").setTabCompleter(new ManageCommand(this));
        hccore = (HCCorePlugin) getServer().getPluginManager().getPlugin("HCCore");
        try {
            Objects.requireNonNull(Objects.requireNonNull(hccore).getDataManager());
        } catch (NullPointerException e) {
            getLogger().log(Level.SEVERE, "Error initializing HCCore for the API!" + System.lineSeparator() + "This plugin will disable now, please check on HCCore and make sure it is installed." + System.lineSeparator() + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        keys = new KeyManager(this);
        limits = new RateLimitManager();
        app = getApp();
        getLogger().log(Level.INFO, "Starting HCCore-API server (on port " + port + ")...");
        app.start(port);
    }

    @Override
    public void onDisable() {
        limits.resetAllLimits();
        getLogger().log(Level.INFO, "Shutting down HCCore-API server...");
        app.stop();
    }

    public void reloadPlugin() {
        getLogger().log(Level.INFO, "Shutting down HCCore-API server (reload)...");
        app.stop();
        reloadConfig();
        limits.resetAllLimits();
        keys = new KeyManager(this);
        limits = new RateLimitManager();
        app = getApp();
        int port = getConfig().getInt("port", 7600);
        getLogger().log(Level.INFO, "Starting HCCore-API server (reload) (on port " + port + ")...");
        app.start(port);
    }
    private Javalin getApp() {
        return Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.startup.showOldJavalinVersionWarning = false;
            config.routes.get("/player", ctx -> {
                String apiKey = ctx.header("Authorization");
                if (apiKey == null || apiKey.split("Bearer ").length < 2) {
                    ctx.status(401);
                    ctx.json(new APIError("unauthorized", "No API key was found in your request, or it was malformed! Make sure you are sending an \"Authorization\" header in your request, with the word Bearer followed by a valid API key."));
                    return;
                }
                APIAccess access = keys.getAccessByKey(apiKey.split("Bearer ")[1]);
                if (access == null || !access.validate()) {
                    ctx.status(403);
                    ctx.json(new APIError("forbidden", "The provided API key is invalid or has been disabled! Check that your key exists, is correctly entered, that you haven't been told about it being disabled, and that your Authorization header is properly formatted (\"Authorization\": \"Bearer <your_key>\")."));
                    return;
                }
                if (limits.isRateLimited(access)) {
                    long expiryUntil = Math.max(0L, (Math.round(limits.getLimitInfo(access).getExpiry() - System.currentTimeMillis()) / 1000L));
                    ctx.status(429);
                    ctx.json(new APIError("rate_limited", "The provided API key has exceeded its rate limit of " + access.rateLimit + " request(s) per minute. Please retry in " + expiryUntil + " second(s)."));
                    ctx.header("Retry-After", String.valueOf(expiryUntil));
                    return;
                }
                limits.countRateLimit(access);
                RateLimitInfo limitInfo = limits.getLimitInfo(access);
                ctx.header("Rate-Limit-Remaining", String.valueOf(access.rateLimit - limitInfo.getCount()));
                ctx.header("Rate-Limit-Reset", String.valueOf(Math.max(0L, (Math.round(limitInfo.getExpiry() - System.currentTimeMillis()) / 1000L))));
                String lookupType;
                List<PlayerInfo> list = new ArrayList<>();
                if (ctx.queryParam("uuid") != null) {
                    UUID playerUUID;
                    try {
                        playerUUID = UUID.fromString(Objects.requireNonNull(ctx.queryParam("uuid")));
                    } catch (IllegalArgumentException ignored) {
                        ctx.status(400);
                        ctx.json(new APIError("invalid_uuid", "This UUID is malformed! Make sure you are sending a valid, hyphenated UUID, such as eb7ea62d-b7aa-4d6e-b68a-d7e948780f03."));
                        return;
                    }
                    OfflinePlayer player = getServer().getOfflinePlayer(playerUUID);
                    if (!player.hasPlayedBefore()) {
                        ctx.status(404);
                        ctx.json(new APIError("unknown_uuid", "This player hasn't played on the server before!"));
                        return;
                    }
                    PlayerData data = hccore.getDataManager().getData(player);
                    list.add(new PlayerInfo(player.getUniqueId(), data.getSlackId(), new Nickname(data.getUsableName(), data.getNameColor().asHexString())));
                    lookupType = "uuid";
                } else if (ctx.queryParam("slack") != null) {
                    List<PlayerData> data = hccore.getDataManager().findDataMany(pData -> Objects.equals(pData.getSlackId(), ctx.queryParam("slack")));
                    if (data == null || data.isEmpty()) {
                        ctx.status(404);
                        ctx.json(new APIError("unknown_slack_id", "This Slack ID isn't linked to any Minecraft player!"));
                        return;
                    }
                    for (PlayerData pData : data) {
                        list.add(new PlayerInfo(pData.offlinePlayer.getUniqueId(), pData.getSlackId(), new Nickname(pData.getUsableName(), pData.getNameColor().asHexString())));
                    }
                    lookupType = "slack";
                } else if (ctx.queryParam("nick") != null) {
                    List<PlayerData> data = hccore.getDataManager().findDataMany(pData -> Objects.equals(pData.getUsableName(), ctx.queryParam("nick")));
                    if (data == null || data.isEmpty()) {
                        ctx.status(404);
                        ctx.json(new APIError("unknown_nick", "This nickname isn't used by any Minecraft player!"));
                        return;
                    }
                    for (PlayerData pData : data) {
                        list.add(new PlayerInfo(pData.offlinePlayer.getUniqueId(), pData.getSlackId(), new Nickname(pData.getUsableName(), pData.getNameColor().asHexString())));
                    }
                    lookupType = "nick";
                } else {
                    ctx.status(400);
                    ctx.json(new APIError("no_param", "You didn't include anything to use in the lookup! Include either \"?uuid=<a player's UUID>\", \"?slack=<a slack id>\", or \"?nick=<an HTML-encoded nickname>\" at the end of your URL."));
                    return;
                }
                ctx.status(200);
                ctx.header("Lookup-Type", lookupType);
                ctx.json(list);
            });
            config.routes.get("/health", ctx -> {
                Boolean authStatus = null;
                if (ctx.header("Authorization") != null) {
                    String apiKey = Objects.requireNonNull(ctx.header("Authorization")).split("Bearer ")[1];
                    APIAccess access = keys.getAccessByKey(apiKey);
                    authStatus = (access == null || access.validate());
                }
                ctx.status(200);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Expires", "0");
                ctx.json(new APIHealth(getPluginMeta().getVersion(), authStatus));
            });
        });
    }
}
