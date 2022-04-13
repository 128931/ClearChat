package me.onetwoeight.clearchat

import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    private val file = description
    private val version = file.version
    private val filename = file.name

    override fun onEnable() {
        saveDefaultConfig()
        getCommand("cc")?.setExecutor(ChatListener(this))
        logger.info("$filename v$version Enabled")
    }

    override fun onDisable() {
        logger.info("$filename v$version Disabled")
    }
}