package me.onetwoeight.clearchat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static org.bukkit.Bukkit.broadcastMessage;
import static org.bukkit.Bukkit.getPlayer;

public final class ClearChat implements CommandExecutor {

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (sender.hasPermission("cc.global") && args.length == 0) {
            for (int i = 0; i < 1000; i++)
                broadcastMessage("");
            broadcastMessage(ChatColor.DARK_AQUA + "Chat Cleared by " + sender.getName() + ChatColor.RESET);
        } else if (sender.hasPermission("cc.player") && args.length == 1) {
            for (int i = 0; i < 1000; i++) {
                if (getPlayer(args[0]) != null && getPlayer(args[0]).isOnline())
                    getPlayer(args[0]).sendMessage("");
                else {
                    sender.sendMessage(ChatColor.RED + "Could not find specified player" + ChatColor.RESET);
                    break;
                }
            }
            if (getPlayer(args[0]) != null && getPlayer(args[0]).isOnline())
                getPlayer(args[0]).sendMessage(ChatColor.DARK_AQUA + "Your Chat Was Cleared by " + sender.getName() + ChatColor.RESET);
        } else if (!sender.hasPermission("cc.global") && args.length == 0 || !sender.hasPermission("cc.player") && args.length > 0)
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that" + ChatColor.RESET);
        else if (sender.hasPermission("cc.player") && args.length > 1)
            sender.sendMessage(ChatColor.RED + "Please refrain from using 2 or more args" + ChatColor.RESET);
        return true;
    }
}