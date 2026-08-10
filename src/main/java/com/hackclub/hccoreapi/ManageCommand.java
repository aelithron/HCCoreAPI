package com.hackclub.hccoreapi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageCommand implements TabExecutor {
    private final HCCoreAPI plugin;
    public ManageCommand(HCCoreAPI plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hccoreapi.manage")) {
            sender.sendMessage(Component.text().color(NamedTextColor.RED).content("You don't have permission to do this!").color(NamedTextColor.GRAY).content(" (hccoreapi.manage)"));
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
                case "add":
                    if (args.length < 3) {
                        sendHelpMsg(sender);
                        return false;
                    }

                    return true;
                default:
                    sendHelpMsg(sender);
                    return false;
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
                List<String> options = List.of("list", "add");
                Collections.sort(StringUtil.copyPartialMatches(args[0], options, finalOpts));
                return finalOpts;
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("keys")) {
                if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
                    return List.of("<id>");
                }
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
                .build();
        sender.sendMessage(helpMsg);
    }
}
