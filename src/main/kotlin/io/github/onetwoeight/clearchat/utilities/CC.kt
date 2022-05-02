package io.github.onetwoeight.clearchat.utilities

import org.bukkit.ChatColor

/**
 * @author onetwoeight
 * @since 5/1/2022
 */
object CC {

    /**
     * Translates a string using an alternate color code character into a
     * string that uses the internal ChatColor.COLOR_CODE color code
     * character.
     *
     * @param textToTranslate Text containing the alternate color code character.
     * @return Text containing the ChatColor.COLOR_CODE color code character.
     */
    fun translate(textToTranslate: String): String {
        return ChatColor.translateAlternateColorCodes('&', textToTranslate)
    }
}