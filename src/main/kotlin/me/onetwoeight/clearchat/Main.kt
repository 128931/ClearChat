package me.onetwoeight.clearchat

import me.onetwoeight.clearchat.listeners.ChatListener
import me.onetwoeight.clearchat.statistics.Metrics
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author onetwoeight
 * @since 4/14/2022
 */
class Main : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        getCommand("cc")?.setExecutor(ChatListener(this))
        Metrics(this, 14_968)
        logger.info("${description.name} v${description.version} Enabled")
    }

    override fun onDisable() {
        logger.info("${description.name} v${description.version} Disabled")
    }
}
