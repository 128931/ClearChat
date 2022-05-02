package io.github.onetwoeight.clearchat

import io.github.onetwoeight.clearchat.listeners.ChatListener
import io.github.onetwoeight.clearchat.statistics.Metrics
import io.github.onetwoeight.clearchat.utilities.CC
import io.github.onetwoeight.clearchat.utilities.UpdateChecker
import org.bukkit.Bukkit
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
        UpdateChecker(101_734).getLatestVersion { version: String ->
            if (description.version.equals(version, ignoreCase = true)) {
                logger.info("The plugin is up to date.")
            } else {
                Bukkit.getConsoleSender()
                    .sendMessage(CC.translate("[${description.name}] &cThe plugin has been updated; please download it at https://spigotmc.org/resources/clearchat.101734/&r"))
            }
        }
        logger.info("${description.name} v${description.version} Enabled")
    }

    override fun onDisable() {
        logger.info("${description.name} v${description.version} Disabled")
    }
}
