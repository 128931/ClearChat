package io.github.onetwoeight.clearchat.listeners

import io.github.onetwoeight.clearchat.ClearChatPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import kotlin.random.Random

/**
 * @author onetwoeight
 * @since 4/14/2022
 */
internal class ChatListener(private val plugin: ClearChatPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val prefix = "Prefix"
        val send = "%sender%"
        val global = "cc.global"
        val player = "cc.player"
        if (sender.hasPermission(global) && args.isEmpty()) {
            for (i in 0..999) {
                Bukkit.broadcastMessage(rsg(10))
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
                    Bukkit.getPlayer(args[0])?.sendMessage(rsg(10))
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

    /**
     * Random Spaces Generator (RSG).
     * Randomly generates a number of spaces to be announced because of clients such as Lunar, Wurst, FDP, etc.
     * Have anti-spam features where it will stack a msg sent twice as "hello [x2]" and if we broadcast
     * a single space a thousand times for them, it would view as " [x1000]" not clearing their chat.
     *
     * Also, this was written at 3 a.m., and I haven't slept in about 24 hours as of writing this,
     * so the code is probably not nice. I'll probably improve it later when I have more sleep to think.
     *
     * @param length The number of random spaces that will be created
     * @return String with a random amount of spaces
     */
    @Suppress("KDocUnresolvedReference", "SameParameterValue")
    private fun rsg(length: Int) : String {
        val random = Random.nextInt(length)
        val spaces = StringBuilder(random)
        for (i in 0..random) {
            spaces.append(" ")
        }
        return spaces.toString()
    }
}
