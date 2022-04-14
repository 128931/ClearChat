package me.onetwoeight.clearchat

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

internal class ChatListener(private val plugin: Main) : CommandExecutor {

    private val prefix = "Prefix"
    private val send = "%sender%"
    private val global = "cc.global"
    private val player = "cc.player"

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender.hasPermission(global) && args.isEmpty()) {
            for (i in 0..999) {
                Bukkit.broadcastMessage("")
            }
            Bukkit.broadcastMessage(
                ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.config.getString(prefix) + plugin.config.getString("Global")
                ).replace(send, sender.name)
            )
        } else if (sender.hasPermission(player) && args.size == 1) {
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
                        plugin.config.getString(prefix) + plugin.config.getString("Player")?.replace(
                            send,
                            sender.name
                        )
                    )
                )
                sender.sendMessage(
                    ChatColor.translateAlternateColorCodes(
                        '&',
                        plugin.config.getString(prefix) + plugin.config.getString("Success")?.replace(
                            "%person%",
                            args[0]
                        )
                    )
                )
            }
        } else if (!sender.hasPermission(global) && args.isEmpty() || !sender.hasPermission(player) && args.isNotEmpty()) {
            sender.sendMessage(
                plugin.config.getString("NoPermission")?.let {
                    ChatColor.translateAlternateColorCodes(
                        '&', it.replace(send, sender.name)
                    )
                }
            )
        } else if (sender.hasPermission(player) && args.size > 1) {
            sender.sendMessage(ChatColor.RED.toString() + "Please refrain from using 2 or more args" + ChatColor.RESET)
        }
        return true
    }
}