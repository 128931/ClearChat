package io.github.onetwoeight.clearchat

import io.github.onetwoeight.clearchat.listeners.ChatListener
import io.github.onetwoeight.clearchat.statistics.Metrics
import io.github.onetwoeight.clearchat.utilities.UpdateChecker
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author onetwoeight
 * @since 4/14/2022
 */
class ClearChatPlugin : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        getCommand("cc")?.setExecutor(ChatListener(this))
        Metrics(this, 14_968)
        logger.info("${description.name} v${description.version} Enabled")
        UpdateChecker(101_734).getLatestVersion { version: String ->
            if (description.version.equals(version, ignoreCase = true)) {
                logger.info("The plugin is up to date.")
            } else {
                logger.warning("The plugin has been updated; please get it from https://spigotmc.org/resources/clearchat.101734/")
            }
        }
    }

    override fun onDisable() {
        logger.info("${description.name} v${description.version} Disabled")
    }
}
