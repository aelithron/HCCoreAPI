package com.hackclub.hccoreapi;

import com.hackclub.hccore.HCCorePlugin;
import com.hackclub.hccore.PlayerData;
import com.hackclub.hccoreapi.DataTypes.APIAccess;
import com.hackclub.hccoreapi.DataTypes.APIError;
import com.hackclub.hccoreapi.DataTypes.Nickname;
import com.hackclub.hccoreapi.DataTypes.PlayerInfo;
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
    private KeyManager keys;
    private RateLimitManager limits;
    private HCCorePlugin hccore;
    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        int port = getConfig().getInt("port", 7600);
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

        app = Javalin.create(config -> {
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
                    ctx.json(new APIError("forbidden", "The provided API key is invalid or has been disabled! Check that your key exists, is correctly entered, that you haven't been told about it being disabled, and that your Authorization header is properly formatted (\"Authorization\"=\"Bearer <your_key>\")."));
                    return;
                }
                if (limits.isRateLimited(access)) {
                    ctx.status(422);
                    ctx.json(new APIError("rate_limited", "The provided API key has exceeded its rate limit of " + access.rateLimit + " requests per minute. Please retry in one minute."));
                }
                limits.countRateLimit(access);
                List<PlayerInfo> list = new ArrayList<>();
                if (ctx.queryParam("uuid") != null) {
                    OfflinePlayer player = getServer().getOfflinePlayer(UUID.fromString(Objects.requireNonNull(ctx.queryParam("uuid"))));
                    if (!player.hasPlayedBefore()) {
                        ctx.status(404);
                        ctx.json(new APIError("unknown_uuid", "This player hasn't played on the server before!"));
                        return;
                    }
                    PlayerData data = hccore.getDataManager().getData(player);
                    list.add(new PlayerInfo(player.getUniqueId(), data.getSlackId(), new Nickname(data.getUsableName(), data.getNameColor().asHexString())));
                } else if (ctx.queryParam("slack") != null) {
                    List<PlayerData> data = hccore.getDataManager().findDataMany(pData -> Objects.equals(pData.getSlackId(), ctx.queryParam("slack")));
                    if (data == null) {
                        ctx.status(404);
                        ctx.json(new APIError("unknown_slack_id", "This Slack ID isn't linked to any Minecraft player!"));
                        return;
                    }
                    for (PlayerData pData : data) {
                        list.add(new PlayerInfo(pData.offlinePlayer.getUniqueId(), pData.getSlackId(), new Nickname(pData.getUsableName(), pData.getNameColor().asHexString())));
                    }
                } else if (ctx.queryParam("nick") != null) {
                    PlayerData data = hccore.getDataManager().findData(pData -> Objects.equals(pData.getUsableName(), ctx.queryParam("nick")));
                    if (data == null) {
                        ctx.status(404);
                        ctx.json(new APIError("unknown_nick", "This nickname isn't used by any Minecraft player!"));
                        return;
                    }
                    list.add(new PlayerInfo(data.offlinePlayer.getUniqueId(), data.getSlackId(), new Nickname(data.getUsableName(), data.getNameColor().asHexString())));
                } else {
                    ctx.status(400);
                    ctx.json(new APIError("no_search", "You didn't include anything to search by! Include either \"?uuid=<a player's UUID>\", \"?slack=<a slack id>\", or \"?nick=<an HTML-encoded nickname>\" at the end of your URL."));
                    return;
                }
                ctx.status(200);
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
