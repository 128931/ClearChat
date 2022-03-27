package me.onetwoeight.clearchat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static org.bukkit.Bukkit.broadcastMessage;

public final class ClearChat implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("cc.clear")) {
            for (int i = 0; i < 1000; i++)
                broadcastMessage("");
            broadcastMessage(ChatColor.DARK_AQUA + "Chat Cleared by " + sender.getName() + ChatColor.RESET);
        } else sender.sendMessage(ChatColor.RED + "You do not have permission to do that." + ChatColor.RESET);
        return true;
    }
}