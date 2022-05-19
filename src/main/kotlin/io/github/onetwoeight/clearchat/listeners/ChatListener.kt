package io.github.onetwoeight.clearchat.listeners

import io.github.onetwoeight.clearchat.ClearChatPlugin
import io.github.onetwoeight.clearchat.utilities.CC
import io.github.onetwoeight.clearchat.utilities.Random
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender


/**
 * @author onetwoeight
 * @since 4/14/2022
 */
class ChatListener(private val plugin: ClearChatPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val prefix = "Prefix"
        val send = "%sender%"
        if (args.isEmpty()) {
            for (i in 0..999) {
                Bukkit.broadcastMessage(Random.nextSpace(16))
            }
            Bukkit.broadcastMessage(
                CC.translate(
                    plugin.config.getString(prefix) + plugin.config.getString("Global")?.replace(send, sender.name)
                )
            )
        } else if (args.size == 1) {
            for (i in 0..999) {
                if (Bukkit.getPlayer(args[0])?.isOnline == true) {
                    Bukkit.getPlayer(args[0])?.sendMessage(Random.nextSpace(16))
                } else {
                    sender.sendMessage("${ChatColor.RED}Could not find specified player${ChatColor.RESET}")
                    break
                }
            }
            if (Bukkit.getPlayer(args[0])?.isOnline == true) {
                Bukkit.getPlayer(args[0])?.sendMessage(
                    CC.translate(
                        plugin.config.getString(prefix) + plugin.config.getString("Player")?.replace(send, sender.name)
                    )
                )
                sender.sendMessage(
                    CC.translate(
                        plugin.config.getString(prefix) + plugin.config.getString("Success")
                            ?.replace("%person%", args[0])
                    )
                )
            }
        } else if (args.size > 1) {
            sender.sendMessage("${ChatColor.RED}Please refrain from using 2 or more args${ChatColor.RESET}")
        }
        return true
    }
}
