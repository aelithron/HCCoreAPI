package com.hackclub.hccoreapi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ManageCommand implements TabExecutor {
    private final HCCoreAPI plugin;
    public ManageCommand(HCCoreAPI plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelpMsg(sender);
            return false;
        }
        switch (args[0]) {
            case "reload":
                plugin.reloadPlugin();
                sender.sendMessage(Component.text().color(NamedTextColor.GREEN).content("Reloaded the web API!"));
                break;
            default:
                sendHelpMsg(sender);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return List.of("reload");
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
                .build();
        sender.sendMessage(helpMsg);
    }
}
