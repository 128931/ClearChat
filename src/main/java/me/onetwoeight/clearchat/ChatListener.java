package me.onetwoeight.clearchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;
import static org.bukkit.Bukkit.broadcastMessage;
import static org.bukkit.Bukkit.getPlayer;
import static org.bukkit.ChatColor.*;

public final class ChatListener implements CommandExecutor {

    private final Main plugin;

    public ChatListener(final Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (sender.hasPermission("cc.global") && args.length == 0) {
            for (int i = 0; i < 1000; i++) {
                broadcastMessage("");
            }
            broadcastMessage(translateAlternateColorCodes('&', plugin.getConfig().getString("Prefix") + requireNonNull(plugin.getConfig().getString("Global"), "replace target/replacement must not be null").replace("%sender%", sender.getName())));
        } else if (sender.hasPermission("cc.player") && args.length == 1) {
            for (int i = 0; i < 1000; i++) {
                if (getPlayer(args[0]) != null && requireNonNull(getPlayer(args[0]), "player must not be null").isOnline()) {
                    requireNonNull(getPlayer(args[0]), "player must not be null").sendMessage("");
                } else {
                    sender.sendMessage(RED + "Could not find specified player" + RESET);
                    break;
                }
            }
            if (getPlayer(args[0]) != null && requireNonNull(getPlayer(args[0]), "player must not be null").isOnline()) {
                requireNonNull(getPlayer(args[0]), "player must not be null").sendMessage(translateAlternateColorCodes('&', plugin.getConfig().getString("Prefix") + requireNonNull(plugin.getConfig().getString("Player"), "replace target/replacement must not be null").replace("%sender%", sender.getName())));
            }
        } else if (!sender.hasPermission("cc.global") && args.length == 0 || !sender.hasPermission("cc.player") && args.length > 0) {
            sender.sendMessage(translateAlternateColorCodes('&', requireNonNull(plugin.getConfig().getString("NoPermission"), "target/replacement must not be null").replace("%sender%", sender.getName())));
        } else if (sender.hasPermission("cc.player") && args.length > 1) {
            sender.sendMessage(RED + "Please refrain from using 2 or more args" + RESET);
        }
        return true;
    }
}