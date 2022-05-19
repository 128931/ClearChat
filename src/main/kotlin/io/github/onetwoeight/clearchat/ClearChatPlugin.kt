package io.github.onetwoeight.clearchat

import io.github.onetwoeight.clearchat.listeners.ChatListener
import io.github.onetwoeight.clearchat.statistics.Metrics
import io.github.onetwoeight.clearchat.utilities.CC
import org.bukkit.plugin.java.JavaPlugin


/**
 * @author onetwoeight
 * @since 4/14/2022
 */
class ClearChatPlugin : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        getCommand("cc")?.setExecutor(ChatListener(this)) ?: run {
            server.consoleSender.sendMessage(
                CC.translate(
                    "[${description.name}] &cExpression 'getCommand(\"cc\")' must not be null&r"
                )
            )
            isEnabled = false
            return
        }
        Metrics(this, 14_968)
        logger.info("${description.name} v${description.version} Enabled")
    }

    override fun onDisable() {
        logger.info("${description.name} v${description.version} Disabled")
    }
}
