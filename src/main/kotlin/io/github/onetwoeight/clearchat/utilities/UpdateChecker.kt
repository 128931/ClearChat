package io.github.onetwoeight.clearchat.utilities

import java.net.URL
import java.util.*
import java.util.function.Consumer

/**
 * @author onetwoeight
 * @since 5/1/2022
 */
class UpdateChecker(private val resourceId: Int) {

    fun getLatestVersion(consumer: Consumer<String>) {
        URL("https://api.spigotmc.org/legacy/update.php?resource=$resourceId").openStream()
            .use { it ->
                Scanner(it, Charsets.UTF_8).use {
                    if (it.hasNext()) {
                        consumer.accept(it.next())
                    }
                }
            }
    }
}