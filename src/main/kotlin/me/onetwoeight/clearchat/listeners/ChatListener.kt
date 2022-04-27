package me.onetwoeight.clearchat.listeners

import me.onetwoeight.clearchat.Main
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
 * @author onetwoeight
 * @since 4/14/2022
 */
internal class ChatListener(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val prefix = "Prefix"
        val send = "%sender%"
        val global = "cc.global"
        val player = "cc.player"
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
                    sender.sendMessage("${ChatColor.RED}Could not find specified player${ChatColor.RESET}")
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
            sender.sendMessage("${ChatColor.RED}Please refrain from using 2 or more args${ChatColor.RESET}")
        }
        return true
    }
}