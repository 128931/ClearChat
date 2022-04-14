package me.onetwoeight.clearchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;
import static org.bukkit.Bukkit.broadcastMessage;
import static org.bukkit.Bukkit.getPlayer;
import static org.bukkit.ChatColor.*;

/**
 * @author onetwoeight
 * @since 3/27/2022
 */
final class ChatListener implements CommandExecutor {

    private final Main plugin;

    public ChatListener(final Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        String player = "cc.player";
        String global = "cc.global";
        String send = "%sender%";
        String prefix = "Prefix";
        String getPlayer = "getPlayer must not be null";
        String getString = "getString must not be null";
        if (sender.hasPermission("cc.global") && args.length == 0) {
            for (int i = 0; i < 1000; i++) {
                broadcastMessage("");
            }
            broadcastMessage(translateAlternateColorCodes('&', plugin.getConfig().getString(prefix) + requireNonNull(plugin.getConfig().getString("Global"), getString).replace(send, sender.getName())));
        } else if (sender.hasPermission(player) && args.length == 1) {
            for (int i = 0; i < 1000; i++) {
                if (getPlayer(args[0]) != null && requireNonNull(getPlayer(args[0]), getPlayer).isOnline()) {
                    requireNonNull(getPlayer(args[0]), getPlayer).sendMessage("");
                } else {
                    sender.sendMessage(RED + "Could not find specified player" + RESET);
                    break;
                }
            }
            if (getPlayer(args[0]) != null && requireNonNull(getPlayer(args[0]), getPlayer).isOnline()) {
                requireNonNull(getPlayer(args[0]), getPlayer).sendMessage(translateAlternateColorCodes('&', plugin.getConfig().getString(prefix) + requireNonNull(plugin.getConfig().getString("Player"), getString).replace(send, sender.getName())));
                sender.sendMessage(translateAlternateColorCodes('&', plugin.getConfig().getString(prefix) + requireNonNull(plugin.getConfig().getString("Success"), getString).replace("%person%", args[0])));
            }
        } else if (!sender.hasPermission(global) && args.length == 0 || !sender.hasPermission(player) && args.length > 0) {
            sender.sendMessage(translateAlternateColorCodes('&', requireNonNull(plugin.getConfig().getString("NoPermission"), getString).replace(send, sender.getName())));
        } else if (sender.hasPermission(player) && args.length > 1) {
            sender.sendMessage(RED + "Please refrain from using 2 or more args" + RESET);
        }
        return true;
    }
}