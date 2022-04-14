package me.onetwoeight.clearchat

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

internal class ChatListener(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender.hasPermission("cc.global") && args.isEmpty()) {
            for (i in 0..999) {
                Bukkit.broadcastMessage("")
            }
            Bukkit.broadcastMessage(
                ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.config.getString("Prefix") + plugin.config.getString("Global")
                ).replace("%sender%", sender.name)
            )
        } else if (sender.hasPermission("cc.player") && args.size == 1) {
            for (i in 0..999) {
                if (Bukkit.getPlayer(args[0])?.isOnline == true) {
                    Bukkit.getPlayer(args[0])?.sendMessage("")
                } else {
                    sender.sendMessage(ChatColor.RED.toString() + "Could not find specified player" + ChatColor.RESET)
                    break
                }
            }
            if (Bukkit.getPlayer(args[0])?.isOnline == true) {
                Bukkit.getPlayer(args[0])?.sendMessage(
                    ChatColor.translateAlternateColorCodes(
                        '&',
                        plugin.config.getString("Prefix") + plugin.config.getString("Player")?.replace(
                            "%sender%",
                            sender.name
                        )
                    )
                )
                sender.sendMessage(
                    ChatColor.translateAlternateColorCodes(
                        '&',
                        plugin.config.getString("Prefix") + plugin.config.getString("Success")?.replace(
                            "%person%",
                            args[0]
                        )
                    )
                )
            }
        } else if (!sender.hasPermission("cc.global") && args.isEmpty() || !sender.hasPermission("cc.player") && args.isNotEmpty()) {
            sender.sendMessage(
                plugin.config.getString("NoPermission")?.let {
                    ChatColor.translateAlternateColorCodes(
                        '&', it.replace("%sender%", sender.name)
                    )
                }
            )
        } else if (sender.hasPermission("cc.player") && args.size > 1) {
            sender.sendMessage(ChatColor.RED.toString() + "Please refrain from using 2 or more args" + ChatColor.RESET)
        }
        return true
    }
}