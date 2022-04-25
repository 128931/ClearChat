package me.onetwoeight.clearchat.statistics

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Level
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection

/**
 * Code Converted to Kotlin by 128931 at 4/24/2022
 *
 * @author BtoBastian
 * @since 2/17/2017
 */
class Metrics(plugin: JavaPlugin, serviceId: Int) {
    private val plugin: Plugin

    /**
     * Creates a new Metrics instance.
     */
    init {
        this.plugin = plugin
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
                Due to servers still running on older versions such as 1.8 that use snakeyaml 1.15
                we have to use this method that's deprecated in newer versions that use snakeyaml 1.30.
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
            try {
                config.save(configFile)
            } catch (e: IOException) {
                this.plugin.getLogger().log(Level.WARNING, e.toString())
            }
        }
        // Load the data
        val enabled = config.getBoolean("enabled", true)
        val serverUUID = config.getString(serverUuid)
        val logErrors = config.getBoolean("logFailedRequests", false)
        val logSentData = config.getBoolean("logSentData", false)
        val logResponseStatusText = config.getBoolean("logResponseStatusText", false)
        MetricsBase(
            "bukkit",
            serverUUID.toString(),
            serviceId,
            enabled,
            { builder: JsonObjectBuilder -> appendPlatformData(builder) },
            { builder: JsonObjectBuilder -> appendServiceData(builder) },
            { submitDataTask: Runnable? -> submitDataTask?.let { Bukkit.getScheduler().runTask(plugin, it) } },
            { plugin.isEnabled },
            { message: String?, error: Throwable? -> this.plugin.getLogger().log(Level.WARNING, message, error) },
            { message: String? -> this.plugin.getLogger().log(Level.INFO, message) },
            logErrors,
            logSentData,
            logResponseStatusText
        )
    }

    private fun appendPlatformData(builder: JsonObjectBuilder) {
        builder.appendField("playerAmount", playerAmount)
        builder.appendField("onlineMode", if (Bukkit.getOnlineMode()) 1 else 0)
        builder.appendField("bukkitVersion", Bukkit.getVersion())
        builder.appendField("bukkitName", Bukkit.getName())
        builder.appendField("javaVersion", System.getProperty("java.version"))
        builder.appendField("osName", System.getProperty("os.name"))
        builder.appendField("osArch", System.getProperty("os.arch"))
        builder.appendField("osVersion", System.getProperty("os.version"))
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors())
    }

    private fun appendServiceData(builder: JsonObjectBuilder) {
        builder.appendField("pluginVersion", plugin.description.version)
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
        private val platform: String,
        private val serverUuid: String,
        private val serviceId: Int,
        private val enabled: Boolean,
        private val appendPlatformDataConsumer: Consumer<JsonObjectBuilder>,
        private val appendServiceDataConsumer: Consumer<JsonObjectBuilder>,
        private val submitTaskConsumer: Consumer<Runnable?>?,
        private val checkServiceEnabledSupplier: Supplier<Boolean>,
        private val errorLogger: BiConsumer<String?, Throwable?>,
        private val infoLogger: Consumer<String?>,
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
            val submitTask = Runnable {
                if (!enabled || java.lang.Boolean.FALSE == checkServiceEnabledSupplier.get()) {
                    // Submitting data or service is disabled
                    scheduler.shutdown()
                    return@Runnable
                }
                if (submitTaskConsumer != null) {
                    submitTaskConsumer.accept(Runnable { submitData() })
                } else {
                    submitData()
                }
            }
            // Many servers tend to restart at a fixed time at xx:00 which causes an uneven distribution
            // of requests on the
            // bStats backend. To circumvent this problem, we introduce some randomness into the initial
            // and second delay.
            // WARNING: You must not modify and part of this Metrics class, including submit delay or
            // frequency!
            // WARNING: Modifying this code will get your plugin banned on bStats. Just don't do it!
            val random = SecureRandom()
            val initialDelay = (1000 * 60 * (3 + random.nextDouble() * 3)).toLong()
            val secondDelay = (1000 * 60 * (random.nextDouble() * 30)).toLong()
            scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS)
            scheduler.scheduleAtFixedRate(
                submitTask, initialDelay + secondDelay, 1800000L, TimeUnit.MILLISECONDS
            )
        }

        private fun submitData() {
            val baseJsonBuilder = JsonObjectBuilder()
            appendPlatformDataConsumer.accept(baseJsonBuilder)
            val serviceJsonBuilder = JsonObjectBuilder()
            appendServiceDataConsumer.accept(serviceJsonBuilder)
            serviceJsonBuilder.appendField("id", serviceId)
            serviceJsonBuilder.appendField("customCharts")
            baseJsonBuilder.appendField("service", serviceJsonBuilder.build())
            baseJsonBuilder.appendField("serverUUID", serverUuid)
            baseJsonBuilder.appendField("metricsVersion", "3.0.0")
            val data = baseJsonBuilder.build()
            scheduler.execute {
                try {
                    // Send the data
                    sendData(data)
                } catch (e: IOException) {
                    // Something went wrong! :(
                    if (logErrors) {
                        errorLogger.accept("Could not submit bStats metrics data", e)
                    }
                }
            }
        }

        @Throws(IOException::class)
        private fun sendData(data: JsonObjectBuilder.JsonObject) {
            if (logSentData) {
                infoLogger.accept("Sent bStats metrics data: $data")
            }
            val url = String.format("https://bStats.org/api/v2/data/%s", platform)
            val connection = URL(url).openConnection() as HttpsURLConnection
            // Compress the data to save bandwidth
            val compressedData = compress(data.toString())
            connection.requestMethod = "POST"
            connection.addRequestProperty("Accept", "application/json")
            connection.addRequestProperty("Connection", "close")
            connection.addRequestProperty("Content-Encoding", "gzip")
            connection.addRequestProperty("Content-Length", compressedData.size.toString())
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Metrics-Service/1")
            connection.doOutput = true
            DataOutputStream(connection.outputStream).use { outputStream -> outputStream.write(compressedData) }
            val builder = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { bufferedReader ->
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    builder.append(line)
                }
            }
            if (logResponseStatusText) {
                infoLogger.accept("Sent data to bStats and received response: $builder")
            }
        }

        companion object {
            private val scheduler =
                Executors.newScheduledThreadPool(1) { task: Runnable? -> Thread(task, "bStats-Metrics") }

            /**
             * Zips the given string.
             *
             * @param str The string to gzip.
             * @return The gzipped string.
             */
            @Throws(IOException::class)
            private fun compress(str: String?): ByteArray {
                if (str == null) {
                    return ByteArray(0)
                }
                val outputStream = ByteArrayOutputStream()
                GZIPOutputStream(outputStream).use { gzip -> gzip.write(str.toByteArray(StandardCharsets.UTF_8)) }
                return outputStream.toByteArray()
            }
        }
    }

    /**
     * An extremely simple JSON builder.
     *
     *
     * While this class is neither feature-rich nor the most performant one, it's sufficient
     * for its use-case.
     */
    private class JsonObjectBuilder {
        private var builder: StringBuilder? = StringBuilder()
        private var hasAtLeastOneField = false

        init {
            builder?.append("{")
        }

        /**
         * Appends an object array to the JSON.
         *
         * @param key The key of the field.
         */
        fun appendField(key: String?) {
            appendFieldUnescaped(key, "[" + "]")
        }

        /**
         * Appends a string field to the JSON.
         *
         * @param key   The key of the field.
         * @param value The value of the field.
         */
        fun appendField(key: String, value: String?) {
            requireNotNull(value) { "JSON value must not be null" }
            appendFieldUnescaped(key, "\"" + escape(value) + "\"")
        }

        /**
         * Appends an integer field to the JSON.
         *
         * @param key   The key of the field.
         * @param value The value of the field.
         */
        fun appendField(key: String, value: Int) {
            appendFieldUnescaped(key, value.toString())
        }

        /**
         * Appends an object to the JSON.
         *
         * @param key    The key of the field.
         * @param object The object.
         */
        fun appendField(key: String, `object`: JsonObject?) {
            requireNotNull(`object`) { "JSON object must not be null" }
            appendFieldUnescaped(key, `object`.toString())
        }

        /**
         * Appends a field to the object.
         *
         * @param key          The key of the field.
         * @param escapedValue The escaped value of the field.
         */
        private fun appendFieldUnescaped(key: String?, escapedValue: String) {
            checkNotNull(builder) { "JSON has already been built" }
            requireNotNull(key) { "JSON key must not be null" }
            if (hasAtLeastOneField) {
                builder?.append(",")
            }
            builder?.append("\"")?.append(escape(key))?.append("\":")?.append(escapedValue)
            hasAtLeastOneField = true
        }

        /**
         * Builds the JSON string and invalidates this builder.
         *
         * @return The built JSON string.
         */
        fun build(): JsonObject {
            checkNotNull(builder) { "JSON has already been built" }
            val `object` = JsonObject(
                builder?.append("}").toString()
            )
            builder = null
            return `object`
        }

        /**
         * A super simple representation of a JSON object.
         *
         *
         * This class only exists to make methods of the [JsonObjectBuilder] type-safe and not
         * allow a raw string inputs for methods like [JsonObjectBuilder.appendField].
         */
        class JsonObject(private val value: String) {
            override fun toString(): String {
                return value
            }
        }

        companion object {
            /**
             * Escapes the given string like stated in https://www.ietf.org/rfc/rfc4627.txt.
             *
             *
             * This method escapes only the necessary characters '"', '\'. and '\u0000' - '\u001F'.
             * Compact escapes are not used (e.g., '\n' is escaped as "\u000a" and not as "\n").
             *
             * @param value The value to escape.
             * @return The escaped value.
             */
            private fun escape(value: String): String {
                val builder = StringBuilder()
                for (element in value) {
                    if (element == '"') {
                        builder.append("\\\"")
                    } else if (element == '\\') {
                        builder.append("\\\\")
                    } else if (element <= '\u000F') {
                        builder.append("\\u000").append(Integer.toHexString(element.code))
                    } else if (element <= '\u001F') {
                        builder.append("\\u00").append(Integer.toHexString(element.code))
                    } else {
                        builder.append(element)
                    }
                }
                return builder.toString()
            }
        }
    }
}