package com.hackclub.hccoreapi;

import com.hackclub.hccore.HCCorePlugin;
import com.hackclub.hccore.PlayerData;
import com.hackclub.hccoreapi.DataTypes.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HCCoreAPI extends JavaPlugin {
    private Javalin app;
    KeyManager keys;
    RateLimitManager limits;
    Logger reqLogger;
    private HCCorePlugin hccore;
    private FileHandler logHandler;
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
        initReqLogger();
        app = getApp();
        getLogger().log(Level.INFO, "Starting HCCore-API server (on port " + port + ")...");
        app.start(port);
    }

    @Override
    public void onDisable() {
        limits.resetAllLimits();
        getLogger().log(Level.INFO, "Shutting down HCCore-API server...");
        app.stop();
        logHandler.close();
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
            config.registerPlugin(new OpenApiPlugin(openApi -> {
                openApi.withDefinitionConfiguration((ver, def) -> {
                    def.openApiVersion("3.0.3");
                    def.withBearerAuth();
                    def.info(info -> {
                        info.title("HCCore Web API");
                        info.description("Powerful web API that allows getting data from the Hack Club Minecraft server and HCCore plugin (https://github.com/hackclub/HCCore)." +
                                " You can find the plugin's source code and an installation guide at https://github.com/aelithron/HCCoreAPI.");
                        info.contact("HCCoreAPI", "https://github.com/aelithron/HCCoreAPI", "nova@novatea.dev");
                        info.license("MIT", "https://github.com/aelithron/HCCoreAPI/blob/main/LICENSE", "MIT");
                        info.termsOfService("https://hackclub.com/conduct");
                    });
                    def.server(server -> {
                        server.url(getConfig().getString("address", "https://api.mc.hackclub.com"));
                        server.description("Production");
                    });
                });
            }));
            config.registerPlugin(new SwaggerPlugin(swagger -> {
                swagger.withUiPath("/");
            }));
            config.registerPlugin(new ReDocPlugin());
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.maxAge = 86400;
                    rule.anyHost();
                    for (String header : List.of("Content-Type", "Rate-Limit-Remaining", "Rate-Limit-Reset", "Retry-After", "Lookup-Type")) {
                        rule.exposeHeader(header);
                    }
                });
            });
            config.bundledPlugins.enableHttpAllowedMethodsOnRoutes();
            config.routes.get("/player", this::playerInfo);
            config.routes.get("/health", this::health);
            config.routes.after(ctx -> {
                if (!ctx.path().startsWith("/player") && !ctx.path().startsWith("/health")) {
                    return;
                }
                String keyID = null;
                String path = ctx.path();
                boolean valid = false;
                int rateLimit = 0;
                int rateLimitUsed = 0;
                if (ctx.queryString() != null) {
                   path = path + "?" + ctx.queryString();
                }
                if (ctx.header("Authorization") != null) {
                    String apiKey = Objects.requireNonNull(ctx.header("Authorization")).split("Bearer ")[1];
                    APIAccess access = keys.getAccessByKey(apiKey);
                    if (access != null) {
                        keyID = access.id;
                        rateLimit = access.rateLimit;
                        RateLimitInfo info = limits.getLimitInfo(access);
                        if (info != null) {
                          rateLimitUsed = info.getCount();
                        }
                        if (access.enabled && access.validate()) {
                            valid = true;
                        }
                    }
                }
                reqLogger.log(Level.INFO, String.format("%s %s - %d <auth: %s - valid: %b> <ratelimit: %d/%d>", ctx.method(), path, ctx.status().getCode(), keyID, valid, rateLimitUsed, rateLimit));
            });
            config.routes.error(404, ctx -> {
               if (ctx.result() != null) {
                   return;
               }
               ctx.json(new APIError("no-route", "The requested route " + ctx.path() + " doesn't exist!"));
            });
        });
    }

    @OpenApi(
            path = "/player",
            methods = {HttpMethod.GET},
            tags = {"Players"},
            security = {@OpenApiSecurity(name = "BearerAuth")},
            summary = "Get a player's data from their UUID, Slack ID, or HCCore nickname.",
            description = "This lets you get a player's data from their UUID, Slack ID, or HCCore nickname. You must specify one parameter out of uuid, slack, or nick (any more than one will be ignored).",
            queryParams = {
                    @OpenApiParam(name = "uuid", type = UUID.class),
                    @OpenApiParam(name = "slack"),
                    @OpenApiParam(name = "nick")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = {@OpenApiContent(from = PlayerInfo[].class, example = """
                            [
                              {
                                "uuid": "eb7ea62d-b7aa-4d6e-b68a-d7e948780f03",
                                "slack": "U08RJ1PEM7X",
                                "nick": {
                                  "name": "Nova",
                                  "color": "#E6E6FA"
                                }
                              }
                            ]
                            """)}, description = "Information about the player."),
                    @OpenApiResponse(status = "429", content = {@OpenApiContent(from = APIError.class, example = """
                            {
                              "error": "rate_limited",
                              "message": "The provided API key has exceeded its rate limit of 10 request(s) per minute. Please retry in 35 second(s)."
                            }
                            """)}, description = "Your key is rate limited."),
                    @OpenApiResponse(status = "401", content = {@OpenApiContent(from = APIError.class, example = """
                            {
                              "error": "unauthorized",
                              "message": "No API key was found in your request, or it was malformed! Make sure you are sending an Authorization header in your request, with the word Bearer followed by a valid API key."
                            }
                            """)}, description = "Authorization is missing or malformed."),
                    @OpenApiResponse(status = "403", content = {@OpenApiContent(from = APIError.class, example = """
                            {
                              "error": "forbidden",
                              "message": "The provided API key is invalid or has been disabled! Check that your key exists, is correctly entered, that you haven't been told about it being disabled, and that your Authorization header is properly formatted."
                            }
                            """)}, description = "API key provided in authorization is invalid/disabled."),
                    @OpenApiResponse(status = "400", content = {@OpenApiContent(from = APIError.class, exampleObjects = {
                            @OpenApiExampleProperty(name = "Error format for missing lookup parameter", objects = {
                                    @OpenApiExampleProperty(name = "error", value = "no_param"),
                                    @OpenApiExampleProperty(name = "message", value = "You didn't include anything to use in the lookup! Include either \"?uuid=<a player's UUID>\", \"?slack=<a slack id>\", or \"?nick=<an HTML-encoded nickname>\" at the end of your URL.")
                            }),
                            @OpenApiExampleProperty(name = "Error format for invalid UUID", objects = {
                                    @OpenApiExampleProperty(name = "error", value = "invalid_uuid"),
                                    @OpenApiExampleProperty(name = "message", value = "This UUID is malformed! Make sure you are sending a valid, hyphenated UUID, such as eb7ea62d-b7aa-4d6e-b68a-d7e948780f03.")
                            })
                    })}, description = "A mistake was made in writing the request."),
                    @OpenApiResponse(status = "404", content = {@OpenApiContent(from = APIError.class, exampleObjects = {
                            @OpenApiExampleProperty(name = "Error format for unknown UUID", objects = {
                                    @OpenApiExampleProperty(name = "error", value = "unknown_uuid"),
                                    @OpenApiExampleProperty(name = "message", value = "This player hasn't played on the server before!")
                            }),
                            @OpenApiExampleProperty(name = "Error format for unknown Slack ID", objects = {
                                    @OpenApiExampleProperty(name = "error", value = "unknown_slack_id"),
                                    @OpenApiExampleProperty(name = "message", value = "This Slack ID isn't linked to any Minecraft player!")
                            }),
                            @OpenApiExampleProperty(name = "Error format for unknown HCCore Nickname", objects = {
                                    @OpenApiExampleProperty(name = "error", value = "unknown_nick"),
                                    @OpenApiExampleProperty(name = "message", value = "This nickname isn't used by any Minecraft player!")
                            })
                    })}, description = "The provided search doesn't match any player(s)."),
            }
    )
    private void playerInfo(Context ctx) {
        String apiKey = ctx.header("Authorization");
        if (apiKey == null || apiKey.split("Bearer ").length < 2) {
            ctx.status(401);
            ctx.json(new APIError("unauthorized", "No API key was found in your request, or it was malformed! Make sure you are sending an Authorization header in your request, with the word Bearer followed by a valid API key."));
            return;
        }
        APIAccess access = keys.getAccessByKey(apiKey.split("Bearer ")[1]);
        if (access == null || !access.validate()) {
            ctx.status(403);
            ctx.json(new APIError("forbidden", """
            The provided API key is invalid or has been disabled! Check that your key exists, is correctly entered, that you haven't been told about it being disabled, and that your Authorization header is properly formatted ("Authorization": "Bearer <your_key>")."
            """));
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
            ctx.json(new APIError("no_param", """
            You didn't include anything to use in the lookup! Include either "?uuid=<a player's UUID>", "?slack=<a Slack ID>", or "?nick=<an HCCore nickname>" at the end of your URL."));
            """));
            return;
        }
        ctx.status(200);
        ctx.header("Lookup-Type", lookupType);
        ctx.json(list);
    }

    @OpenApi(
            path = "/health",
            methods = {HttpMethod.GET},
            tags = {"System"},
            summary = "Check the API's health and version info.",
            description = "Check the API's health, what version it is on, and verify your authentication.",
            responses = {@OpenApiResponse(status = "200", content = {@OpenApiContent(from = APIHealth.class, example = """
                    {
                      "status": "ok",
                      "version": "v1.0.0",
                      "authorized": true
                    }
                    """)}, description = "Information about the API's health.")}
    )
    private void health(Context ctx) {
        Boolean authStatus = null;
        if (ctx.header("Authorization") != null) {
            String apiKey = Objects.requireNonNull(ctx.header("Authorization")).split("Bearer ")[1];
            APIAccess access = keys.getAccessByKey(apiKey);
            authStatus = (access == null || access.validate());
        }
        ctx.status(200);
        ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
        ctx.header("Expires", "0");
        ctx.json(new APIHealth("ok", getPluginMeta().getVersion(), authStatus));
    }

    private void initReqLogger() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File log = new File(getDataFolder(), "requests.log");
        try {
            logHandler = new FileHandler(log.getAbsolutePath(), true);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error setting up the request logger: " + e.getMessage());
            throw new RuntimeException(e);
        }
        logHandler.setFormatter(new ReqLogFormatter());
        reqLogger = Logger.getLogger("HCCore API");
        reqLogger.addHandler(logHandler);
        reqLogger.setUseParentHandlers(false);
    }
}
