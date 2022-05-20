package io.github.onetwoeight.clearchat.statistics

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.util.*
import java.util.function.Consumer
import java.util.logging.Level
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection


/**
 * The code was converted to Kotlin and cleaned up by 128931.
 *
 * @author BtoBastian
 * @since 2/17/2017
 */
class Metrics(
    private val plugin: JavaPlugin,
    serviceId: Int
) {

    /**
     * Creates a new Metrics instance.
     */
    init {
        // Get the config file
        val bStatsFolder = File(plugin.dataFolder.parentFile, "bStats")
        val configFile = File(bStatsFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val serverUuid = "serverUuid"
        if (!config.isSet(serverUuid)) {
            config.addDefault("enabled", true)
            config.addDefault(serverUuid, UUID.randomUUID().toString())
            config.addDefault("logFailedRequests", false)
            config.addDefault("logSentData", false)
            config.addDefault("logResponseStatusText", false)
            // Inform the server owners about bStats
            @Suppress("DEPRECATION")
            config
                .options()
                /*
                Because servers are still operating on older versions of snakeyaml, such as 1.8, which utilizes snakeyaml 1.15,
                we must use this approach, which is deprecated in current versions that use snakeyaml 1.30.
                */
                .header(
                    """
                    bStats (https://bStats.org) collects some basic information for plugin authors, like how
                    many people use their plugin and their total player count. It's recommended to keep bStats
                    enabled, but if you're not comfortable with this, you can turn this setting off. There is no
                    performance penalty associated with having metrics enabled, and data sent to bStats is fully
                    anonymous.
                    """.trimIndent()
                )
                .copyDefaults(true)
            config.save(configFile)
        }
        // Load the data
        val enabled = config.getBoolean("enabled", true)
        val serverUUID = config.getString(serverUuid)
        val logErrors = config.getBoolean("logFailedRequests", false)
        val logSentData = config.getBoolean("logSentData", false)
        val logResponseStatusText = config.getBoolean("logResponseStatusText", false)
        MetricsBase(
            plugin,
            serverUUID.toString(),
            serviceId,
            enabled,
            ::appendPlatformData,
            ::appendServiceData,
            logErrors,
            logSentData,
            logResponseStatusText
        )
    }

    private fun appendPlatformData(builder: JsonObject) {
        builder.addProperty("playerAmount", playerAmount)
        builder.addProperty("onlineMode", if (Bukkit.getOnlineMode()) 1 else 0)
        builder.addProperty("bukkitVersion", Bukkit.getVersion())
        builder.addProperty("bukkitName", Bukkit.getName())
        builder.addProperty("javaVersion", System.getProperty("java.version"))
        builder.addProperty("osName", System.getProperty("os.name"))
        builder.addProperty("osArch", System.getProperty("os.arch"))
        builder.addProperty("osVersion", System.getProperty("os.version"))
        builder.addProperty("coreCount", Runtime.getRuntime().availableProcessors())
    }

    private fun appendServiceData(builder: JsonObject) {
        builder.addProperty("pluginVersion", plugin.description.version)
    }

    private val playerAmount: Int
        get() = try {
            // Around MC 1.8 the return type was changed from an array to a collection,
            // This fixes java.lang.NoSuchMethodError
            val onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers")
            if (onlinePlayersMethod.returnType == MutableCollection::class.java) (onlinePlayersMethod.invoke(Bukkit.getServer()) as Collection<*>).size else (onlinePlayersMethod.invoke(
                Bukkit.getServer()
            ) as Array<*>).size
        } catch (e: Exception) {
            when (e) {
                // Just use the new method if the reflection failed
                is ClassNotFoundException, is NoSuchMethodException, is IllegalAccessException, is InvocationTargetException -> Bukkit.getOnlinePlayers().size
                else -> throw e
            }
        }

    private class MetricsBase(
        private val plugin: JavaPlugin,
        private val serverUuid: String,
        private val serviceId: Int,
        enabled: Boolean,
        private val appendPlatformDataConsumer: Consumer<JsonObject>,
        private val appendServiceDataConsumer: Consumer<JsonObject>,
        private val logErrors: Boolean,
        private val logSentData: Boolean,
        private val logResponseStatusText: Boolean
    ) {
        /**
         * Creates a new MetricsBase class instance.
         */
        init {
            if (enabled) {
                // WARNING: Removing the option to opt-out will get your plugin banned from bStats
                startSubmitting()
            }
        }

        private fun startSubmitting() {
            /*
             Instead of using random values that constantly bombard my console with 429 (too many requests) errors,
             we simply set it to send the request every 30 minutes to resolve the problem. Furthermore,
             it only begins sending the request(s) 30 minutes after the server has booted up to avoid sending requests
             immediately every time we boot up our server.


             NOTICE:
             bStats staff if you are concerned with the fact that I transmit a request every 30 minutes, please let me know,
             and I will raise it to 35 minutes since that was the most amount of minutes your rng could make/reach.
             */
            CoroutineScope(Dispatchers.Default).launch {
                // If someone knows a cleaner way to do this please lmk
                while (true) {
                    delay(1_800_000L)
                    submitData()
                }
            }
        }

        private fun submitData() {
            val baseJsonBuilder = JsonObject()
            appendPlatformDataConsumer.accept(baseJsonBuilder)
            val serviceJsonBuilder = JsonObject()
            appendServiceDataConsumer.accept(serviceJsonBuilder)
            serviceJsonBuilder.addProperty("id", serviceId)
            serviceJsonBuilder.add("customCharts", JsonArray())
            baseJsonBuilder.add("service", serviceJsonBuilder)
            baseJsonBuilder.addProperty("serverUUID", serverUuid)
            baseJsonBuilder.addProperty("metricsVersion", "3.0.0")
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // Send the data
                    sendData(baseJsonBuilder)
                } catch (e: IOException) {
                    // Something went wrong! :(
                    if (logErrors) {
                        plugin.logger.log(Level.WARNING, "Could not submit bStats metrics data", e)
                    }
                }
            }
        }

        private fun sendData(data: JsonObject) {
            if (logSentData) {
                plugin.logger.info("Sent bStats metrics data: $data")
            }
            val url = "https://bStats.org/api/v2/data/bukkit"
            val connection = URL(url).openConnection() as HttpsURLConnection
            // Compress the data to save bandwidth
            val compressedData = compress("$data")
            connection.requestMethod = "POST"
            connection.addRequestProperty("Accept", "application/json")
            connection.addRequestProperty("Connection", "close")
            connection.addRequestProperty("Content-Encoding", "gzip")
            connection.addRequestProperty("Content-Length", compressedData.size.toString())
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Metrics-Service/1")
            connection.doOutput = true
            DataOutputStream(connection.outputStream).use {
                it.write(compressedData)
            }
            val builder = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it ->
                // NOTICE: Removing the ability to allow for null values breaks the bStats config boolean "logResponseStatusText"
                var line: String?
                while (it.readLine().also { line = it } != null) {
                    builder.append(line)
                }
            }
            if (logResponseStatusText) {
                plugin.logger.info("Sent data to bStats and received response: $builder")
            }
        }

        /**
         * Zips the given string.
         *
         * @param str The string to gzip.
         * @return The gzipped string.
         */
        private fun compress(str: String): ByteArray {
            val outputStream = ByteArrayOutputStream()
            GZIPOutputStream(outputStream).use {
                it.write(str.toByteArray(Charsets.UTF_8))
            }
            return outputStream.toByteArray()
        }
    }
}
