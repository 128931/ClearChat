package me.onetwoeight.clearchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static org.bukkit.Bukkit.broadcastMessage;
import static org.bukkit.Bukkit.getPlayer;
import static org.bukkit.ChatColor.*;

public final class ChatListener implements CommandExecutor {

    private final Main plugin;

    public ChatListener(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (sender.hasPermission("cc.global") && args.length == 0) {
            for (int i = 0; i < 1000; i++)
                broadcastMessage("");
            broadcastMessage(translateAlternateColorCodes('&', plugin.getConfig().getString("Prefix") + plugin.getConfig().getString("Global").replace("%sender%", sender.getName())));
        } else if (sender.hasPermission("cc.player") && args.length == 1) {
            for (int i = 0; i < 1000; i++)
                if (getPlayer(args[0]) != null && getPlayer(args[0]).isOnline())
                    getPlayer(args[0]).sendMessage("");
                else {
                    sender.sendMessage(RED + "Could not find specified player" + RESET);
                    break;
                }
            if (getPlayer(args[0]) != null && getPlayer(args[0]).isOnline())
                getPlayer(args[0]).sendMessage(translateAlternateColorCodes('&', plugin.getConfig().getString("Prefix") + plugin.getConfig().getString("Player").replace("%sender%", sender.getName())));
        } else if (!sender.hasPermission("cc.global") && args.length == 0 || !sender.hasPermission("cc.player") && args.length > 0)
            sender.sendMessage(RED + "You do not have permission to do that" + RESET);
        else if (sender.hasPermission("cc.player") && args.length > 1)
            sender.sendMessage(RED + "Please refrain from using 2 or more args" + RESET);
        return true;
    }
}