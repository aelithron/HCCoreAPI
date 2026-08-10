package com.hackclub.hccoreapi;

import com.hackclub.hccoreapi.DataTypes.APIAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ManageCommand implements TabExecutor {
    private final HCCoreAPI plugin;
    public ManageCommand(HCCoreAPI plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hccoreapi.manage")) {
            sender.sendMessage(Component.text().color(NamedTextColor.RED).content("You don't have permission to do this!").append(Component.text().color(NamedTextColor.GRAY).content(" (hccoreapi.manage)")));
            return false;
        }
        if (args.length == 0) {
            sendHelpMsg(sender);
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadPlugin();
                sender.sendMessage(Component.text().color(NamedTextColor.GREEN).content("Reloaded the web API!"));
                return true;
            case "keys":
                break;
            default:
                sendHelpMsg(sender);
                return false;
        }
        if (args.length == 1) {
            sendHelpMsg(sender);
            return false;
        }
        if (args[0].equalsIgnoreCase("keys")) {
            switch (args[1].toLowerCase()) {
                case "list":
                    Map<String, Boolean> keys = plugin.keys.getKeyList();
                    if (keys.isEmpty()) {
                        sender.sendMessage(Component.text().color(NamedTextColor.RED).content("There aren't any API keys configured!"));
                        return false;
                    }
                    TextComponent.Builder list = Component.text().append(Component.text().color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).content("API Key List"));
                    for (String key : keys.keySet()) {
                        boolean enabled = keys.get(key);
                        if (enabled) {
                            list.appendNewline().resetStyle().append(Component.text().color(NamedTextColor.GREEN).content("- " + key)).append(Component.text().color(NamedTextColor.GRAY).content(" (Enabled)"));
                        } else {
                            list.appendNewline().resetStyle().append(Component.text().color(NamedTextColor.RED).content("- " + key)).append(Component.text().color(NamedTextColor.GRAY).content(" (Disabled)"));
                        }
                    }
                    sender.sendMessage(list.build());
                    return true;
                case "add":
                    if (args.length < 3) {
                        sendHelpMsg(sender);
                        return false;
                    }
                    APIAccess access = plugin.keys.createNewKey(args[2]);
                    if (access == null) {
                        sender.sendMessage(Component.text().color(NamedTextColor.RED).content("There was an error creating this key! Try using a different ID."));
                        return false;
                    }
                    TextComponent component = Component.text()
                            .append(Component.text().color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).content("Key \"" + access.id + "\" created successfully!"))
                            .appendNewline()
                            .appendNewline()
                            .resetStyle()
                            .append(Component.text().color(NamedTextColor.GREEN).content("API Key: ********").clickEvent(ClickEvent.copyToClipboard(access.key)))
                            .append(Component.text().color(NamedTextColor.GRAY).content(" (click to copy)").clickEvent(ClickEvent.copyToClipboard(access.key)))
                            .appendNewline()
                            .resetStyle()
                            .append(Component.text().color(NamedTextColor.GREEN).content("Rate Limit: 10 requests/minute"))
                            .appendNewline()
                            .resetStyle()
                            .append(Component.text().color(NamedTextColor.GREEN).content("Enabled: Yes"))
                            .build();
                    sender.sendMessage(component);
                    return true;
                case "remove":
                    if (args.length < 3) {
                        sendHelpMsg(sender);
                        return false;
                    }
                    boolean success = plugin.keys.deleteKey(args[2]);
                    if (!success) {
                        sender.sendMessage(Component.text().color(NamedTextColor.RED).content("There isn't any key by the ID \"" + args[2] + "\"."));
                        return false;
                    }
                    sender.sendMessage(Component.text().color(NamedTextColor.GREEN).content("Deleted key \"" + args[2] + "\" successfully."));
                    return true;
                case "disable":
                    if (args.length < 3) {
                        sendHelpMsg(sender);
                        return false;
                    }
                    boolean successDisable = plugin.keys.changeKeyStatus(args[2], false);
                    if (!successDisable) {
                        sender.sendMessage(Component.text().color(NamedTextColor.RED).content("There isn't any key by the ID \"" + args[2] + "\"."));
                        return false;
                    }
                    sender.sendMessage(Component.text().color(NamedTextColor.GREEN).content("Disabled key \"" + args[2] + "\" successfully."));
                    return true;
                case "enable":
                    if (args.length < 3) {
                        sendHelpMsg(sender);
                        return false;
                    }
                    boolean successEnable = plugin.keys.changeKeyStatus(args[2], true);
                    if (!successEnable) {
                        sender.sendMessage(Component.text().color(NamedTextColor.RED).content("There isn't any key by the ID \"" + args[2] + "\"."));
                        return false;
                    }
                    sender.sendMessage(Component.text().color(NamedTextColor.GREEN).content("Enabled key \"" + args[2] + "\" successfully."));
                    return true;
                case "ratelimit":
                    if (args.length < 4) {
                        sendHelpMsg(sender);
                        return false;
                    }
                    int newLimit;
                    try {
                        newLimit = Integer.parseInt(args[3]);
                    } catch (NumberFormatException ignored) {
                        sender.sendMessage(Component.text().color(NamedTextColor.RED).content("The number provided was invalid, please make sure to type a non-decimal number after the ID!"));
                        return false;
                    }
                    boolean successRateLimit = plugin.keys.changeKeyLimit(args[2], newLimit);
                    if (!successRateLimit) {
                        sender.sendMessage(Component.text().color(NamedTextColor.RED).content("There isn't any key by the ID \"" + args[2] + "\"."));
                        return false;
                    }
                    sender.sendMessage(Component.text().color(NamedTextColor.GREEN).content("Enabled key \"" + args[2] + "\" successfully."));
                    return true;
                default:
                    break;
            }
        }
        sendHelpMsg(sender);
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> finalOpts = new ArrayList<>();
        if (args.length == 1) {
            List<String> options = List.of("reload", "keys");
            Collections.sort(StringUtil.copyPartialMatches(args[0], options, finalOpts));
            return finalOpts;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("keys")) {
                List<String> options = List.of("list", "add", "remove");
                Collections.sort(StringUtil.copyPartialMatches(args[1], options, finalOpts));
                return finalOpts;
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("keys")) {
                switch (args[1].toLowerCase()) {
                    case "add":
                        return List.of("<id>");
                    case "remove":
                    case "enable":
                    case "disable":
                    case "ratelimit":
                        Set<String> ids = plugin.keys.getKeyList().keySet();
                        Collections.sort(StringUtil.copyPartialMatches(args[2], ids, finalOpts));
                        return finalOpts;
                }
            }
        }
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("keys") && args[1].equalsIgnoreCase("ratelimit")) {
                return List.of("<number>");
            }
        }
        return new ArrayList<>();
    }

    private void sendHelpMsg(CommandSender sender) {
        TextComponent helpMsg = Component.text()
                .append(Component.text().color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).content("HCCore Web API Help"))
                .appendNewline()
                .resetStyle()
                .append(Component.text().color(NamedTextColor.BLUE).content("/webapi reload"))
                .append(Component.text().color(NamedTextColor.WHITE).content(": Reloads the plugin, also resetting all rate limit caps."))
                .appendNewline()
                .resetStyle()
                .append(Component.text().color(NamedTextColor.BLUE).content("/webapi keys list"))
                .append(Component.text().color(NamedTextColor.WHITE).content(": Lists information about all API keys."))
                .appendNewline()
                .resetStyle()
                .append(Component.text().color(NamedTextColor.BLUE).content("/webapi keys add <id>"))
                .append(Component.text().color(NamedTextColor.WHITE).content(": Adds a new API key and sends it to you."))
                .appendNewline()
                .resetStyle()
                .append(Component.text().color(NamedTextColor.BLUE).content("/webapi keys remove <id>"))
                .append(Component.text().color(NamedTextColor.WHITE).content(": Removes an API key permanently."))
                .appendNewline()
                .resetStyle()
                .append(Component.text().color(NamedTextColor.BLUE).content("/webapi keys disable <id>"))
                .append(Component.text().color(NamedTextColor.WHITE).content(": Temporarily disables an API key."))
                .appendNewline()
                .resetStyle()
                .append(Component.text().color(NamedTextColor.BLUE).content("/webapi keys enable <id>"))
                .append(Component.text().color(NamedTextColor.WHITE).content(": Re-enables a previously disabled API key."))
                .appendNewline()
                .resetStyle()
                .append(Component.text().color(NamedTextColor.BLUE).content("/webapi keys ratelimit <id> <number>"))
                .append(Component.text().color(NamedTextColor.WHITE).content(": Edits the rate limit of a key."))
                .build();
        sender.sendMessage(helpMsg);
    }
}
