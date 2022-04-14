package me.onetwoeight.clearchat

import org.bukkit.plugin.java.JavaPlugin

/**
 * @author onetwoeight
 * @since 4/14/2022
 */
class Main : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        getCommand("cc")?.setExecutor(ChatListener(this))
        logger.info("${description.name} v${description.version} Enabled")
    }

    override fun onDisable() {
        logger.info("${description.name} v${description.version} Disabled")
    }
}