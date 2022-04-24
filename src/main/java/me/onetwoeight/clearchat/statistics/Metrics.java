package me.onetwoeight.clearchat.statistics;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

/**
 * Code Cleaned up by 128931 at 4/18/2022
 *
 * @author BtoBastian
 * @since 2/17/2017
 */
public final class Metrics {

    private final Plugin plugin;

    /**
     * Creates a new Metrics instance.
     *
     * @param plugin    Your plugin instance.
     * @param serviceId The id of the service. It can be found at <a href="https://bstats.org/what-is-my-plugin-id">What is my plugin id?</a>
     */
    public Metrics(final JavaPlugin plugin, final int serviceId) {
        this.plugin = plugin;
        // Get the config file
        final File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
        final File configFile = new File(bStatsFolder, "config.yml");
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        final String serverUuid = "serverUuid";
        if (!config.isSet(serverUuid)) {
            config.addDefault("enabled", true);
            config.addDefault(serverUuid, UUID.randomUUID().toString());
            config.addDefault("logFailedRequests", false);
            config.addDefault("logSentData", false);
            config.addDefault("logResponseStatusText", false);
            // Inform the server owners about bStats
            //noinspection deprecation
            config
                    .options()
                    // Due to servers still running on older versions such as 1.8 we have to use this method that's deprecated in newer versions.
                    .header(
                            "bStats (https://bStats.org) collects some basic information for plugin authors, like how\n"
                                    + "many people use their plugin and their total player count. It's recommended to keep bStats\n"
                                    + "enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n"
                                    + "performance penalty associated with having metrics enabled, and data sent to bStats is fully\n"
                                    + "anonymous.")
                    .copyDefaults(true);
            try {
                config.save(configFile);
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.WARNING, e.toString());
            }
        }
        // Load the data
        final boolean enabled = config.getBoolean("enabled", true);
        final String serverUUID = config.getString(serverUuid);
        final boolean logErrors = config.getBoolean("logFailedRequests", false);
        final boolean logSentData = config.getBoolean("logSentData", false);
        final boolean logResponseStatusText = config.getBoolean("logResponseStatusText", false);
        new MetricsBase(
                "bukkit",
                serverUUID,
                serviceId,
                enabled,
                this::appendPlatformData,
                this::appendServiceData,
                submitDataTask -> Bukkit.getScheduler().runTask(plugin, submitDataTask),
                plugin::isEnabled,
                (message, error) -> this.plugin.getLogger().log(Level.WARNING, message, error),
                message -> this.plugin.getLogger().log(Level.INFO, message),
                logErrors,
                logSentData,
                logResponseStatusText);
    }

    private void appendPlatformData(final JsonObjectBuilder builder) {
        builder.appendField("playerAmount", getPlayerAmount());
        builder.appendField("onlineMode", Bukkit.getOnlineMode() ? 1 : 0);
        builder.appendField("bukkitVersion", Bukkit.getVersion());
        builder.appendField("bukkitName", Bukkit.getName());
        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(final JsonObjectBuilder builder) {
        builder.appendField("pluginVersion", plugin.getDescription().getVersion());
    }

    private int getPlayerAmount() {
        try {
            // Around MC 1.8 the return type was changed from an array to a collection,
            // This fixes java.lang.NoSuchMethodError
            final Method onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers");
            return onlinePlayersMethod.getReturnType().equals(Collection.class)
                    ? ((Collection<?>) onlinePlayersMethod.invoke(Bukkit.getServer())).size()
                    : ((Player[]) onlinePlayersMethod.invoke(Bukkit.getServer())).length;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            // Just use the new method if the reflection failed
            return Bukkit.getOnlinePlayers().size();
        }
    }

    private static final class MetricsBase {

        private static final ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1, task -> new Thread(task, "bStats-Metrics"));

        private final String platform;

        private final String serverUuid;

        private final int serviceId;

        private final Consumer<JsonObjectBuilder> appendPlatformDataConsumer;

        private final Consumer<JsonObjectBuilder> appendServiceDataConsumer;

        private final Consumer<Runnable> submitTaskConsumer;

        private final Supplier<Boolean> checkServiceEnabledSupplier;

        private final BiConsumer<String, Throwable> errorLogger;

        private final Consumer<String> infoLogger;

        private final boolean logErrors;

        private final boolean logSentData;

        private final boolean logResponseStatusText;

        private final boolean enabled;

        /**
         * Creates a new MetricsBase class instance.
         *
         * @param platform                    The platform of the service.
         * @param serviceId                   The id of the service.
         * @param serverUuid                  The server uuid.
         * @param enabled                     Whether data sending is enabled.
         * @param appendPlatformDataConsumer  A consumer that receives a {@code JsonObjectBuilder} and
         *                                    appends all platform-specific data.
         * @param appendServiceDataConsumer   A consumer that receives a {@code JsonObjectBuilder} and
         *                                    appends all service-specific data.
         * @param submitTaskConsumer          A consumer that takes a runnable with the submit task. This can be
         *                                    used to delegate the data collection to another thread to prevent errors caused by
         *                                    concurrency. Can be {@code null}.
         * @param checkServiceEnabledSupplier A supplier to check if the service is still enabled.
         * @param errorLogger                 A consumer that accepts log message and an error.
         * @param infoLogger                  A consumer that accepts info log messages.
         * @param logErrors                   Whether errors should be logged.
         * @param logSentData                 Whether the sent data should be logged.
         * @param logResponseStatusText       Whether the response status text should be logged.
         */
        @SuppressWarnings("GrazieInspection")
        public MetricsBase(
                final String platform,
                final String serverUuid,
                final int serviceId,
                final boolean enabled,
                final Consumer<JsonObjectBuilder> appendPlatformDataConsumer,
                final Consumer<JsonObjectBuilder> appendServiceDataConsumer,
                final Consumer<Runnable> submitTaskConsumer,
                final Supplier<Boolean> checkServiceEnabledSupplier,
                final BiConsumer<String, Throwable> errorLogger,
                final Consumer<String> infoLogger,
                final boolean logErrors,
                final boolean logSentData,
                final boolean logResponseStatusText) {
            this.platform = platform;
            this.serverUuid = serverUuid;
            this.serviceId = serviceId;
            this.enabled = enabled;
            this.appendPlatformDataConsumer = appendPlatformDataConsumer;
            this.appendServiceDataConsumer = appendServiceDataConsumer;
            this.submitTaskConsumer = submitTaskConsumer;
            this.checkServiceEnabledSupplier = checkServiceEnabledSupplier;
            this.errorLogger = errorLogger;
            this.infoLogger = infoLogger;
            this.logErrors = logErrors;
            this.logSentData = logSentData;
            this.logResponseStatusText = logResponseStatusText;
            if (enabled) {
                // WARNING: Removing the option to opt-out will get your plugin banned from bStats
                startSubmitting();
            }
        }

        /**
         * Gzips the given string.
         *
         * @param str The string to gzip.
         * @return The gzipped string.
         */
        @SuppressWarnings("SpellCheckingInspection")
        private static byte[] compress(final String str) throws IOException {
            if (str == null) {
                return new byte[0];
            }
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
                gzip.write(str.getBytes(StandardCharsets.UTF_8));
            }
            return outputStream.toByteArray();
        }

        @SuppressWarnings("GrazieInspection")
        private void startSubmitting() {
            final Runnable submitTask =
                    () -> {
                        if (!enabled || Boolean.FALSE.equals(checkServiceEnabledSupplier.get())) {
                            // Submitting data or service is disabled
                            scheduler.shutdown();
                            return;
                        }
                        if (submitTaskConsumer != null) {
                            submitTaskConsumer.accept(this::submitData);
                        } else {
                            this.submitData();
                        }
                    };
            // Many servers tend to restart at a fixed time at xx:00 which causes an uneven distribution
            // of requests on the
            // bStats backend. To circumvent this problem, we introduce some randomness into the initial
            // and second delay.
            // WARNING: You must not modify and part of this Metrics class, including the submit delay or
            // frequency!
            // WARNING: Modifying this code will get your plugin banned on bStats. Just don't do it!
            final SecureRandom random = new SecureRandom();
            final long initialDelay = (long) (1000 * 60 * (3 + random.nextDouble() * 3));
            final long secondDelay = (long) (1000 * 60 * (random.nextDouble() * 30));
            scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(
                    submitTask, initialDelay + secondDelay, 1_800_000L, TimeUnit.MILLISECONDS);
        }

        private void submitData() {
            final JsonObjectBuilder baseJsonBuilder = new JsonObjectBuilder();
            appendPlatformDataConsumer.accept(baseJsonBuilder);
            final JsonObjectBuilder serviceJsonBuilder = new JsonObjectBuilder();
            appendServiceDataConsumer.accept(serviceJsonBuilder);
            serviceJsonBuilder.appendField("id", serviceId);
            serviceJsonBuilder.appendField("customCharts");
            baseJsonBuilder.appendField("service", serviceJsonBuilder.build());
            baseJsonBuilder.appendField("serverUUID", serverUuid);
            baseJsonBuilder.appendField("metricsVersion", "3.0.0");
            final JsonObjectBuilder.JsonObject data = baseJsonBuilder.build();
            scheduler.execute(
                    () -> {
                        try {
                            // Send the data
                            sendData(data);
                        } catch (IOException e) {
                            // Something went wrong! :(
                            if (logErrors) {
                                errorLogger.accept("Could not submit bStats metrics data", e);
                            }
                        }
                    });
        }

        private void sendData(final JsonObjectBuilder.JsonObject data) throws IOException {
            if (logSentData) {
                infoLogger.accept("Sent bStats metrics data: " + data.toString());
            }
            final String url = String.format("https://bStats.org/api/v2/data/%s", platform);
            final HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            // Compress the data to save bandwidth
            final byte[] compressedData = compress(data.toString());
            connection.setRequestMethod("POST");
            connection.addRequestProperty("Accept", "application/json");
            connection.addRequestProperty("Connection", "close");
            connection.addRequestProperty("Content-Encoding", "gzip");
            connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Metrics-Service/1");
            connection.setDoOutput(true);
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(compressedData);
            }
            final StringBuilder builder = new StringBuilder();
            try (BufferedReader bufferedReader =
                         new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    builder.append(line);
                }
            }
            if (logResponseStatusText) {
                infoLogger.accept("Sent data to bStats and received response: " + builder);
            }
        }

    }

    /**
     * An extremely simple JSON builder.
     *
     * <p>While this class is neither feature-rich nor the most performant one, it's sufficient
     * for its use-case.
     */
    private static final class JsonObjectBuilder {

        private StringBuilder builder = new StringBuilder();

        private boolean hasAtLeastOneField = false;

        public JsonObjectBuilder() {
            builder.append("{");
        }

        /**
         * Escapes the given string like stated in <a href="https://www.ietf.org/rfc/rfc4627.txt">https://www.ietf.org/rfc/rfc4627.txt</a>.
         *
         * <p>This method escapes only the necessary characters '"', '\'. and '\u0000' - '\u001F'.
         * Compact escapes are not used (e.g., '\n' is escaped as "\u000a" and not as "\n").
         *
         * @param value The value to escape.
         * @return The escaped value.
         */
        private static String escape(final String value) {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                final char c = value.charAt(i);
                if (c == '"') {
                    builder.append("\\\"");
                } else if (c == '\\') {
                    builder.append("\\\\");
                } else if (c <= '\u000F') {
                    builder.append("\\u000").append(Integer.toHexString(c));
                } else if (c <= '\u001F') {
                    builder.append("\\u00").append(Integer.toHexString(c));
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

        /**
         * Appends an object array to the JSON.
         *
         * @param key The key of the field.
         */
        public void appendField(final String key) {
            appendFieldUnescaped(key, "[" + "]");
        }

        /**
         * Appends a string field to the JSON.
         *
         * @param key   The key of the field.
         * @param value The value of the field.
         */
        private void appendField(final String key, final String value) {
            if (value == null) {
                throw new IllegalArgumentException("JSON value must not be null");
            }
            appendFieldUnescaped(key, "\"" + escape(value) + "\"");
        }

        /**
         * Appends an integer field to the JSON.
         *
         * @param key   The key of the field.
         * @param value The value of the field.
         */
        private void appendField(final String key, final int value) {
            appendFieldUnescaped(key, String.valueOf(value));
        }

        /**
         * Appends an object to the JSON.
         *
         * @param key    The key of the field.
         * @param object The object.
         */
        private void appendField(final String key, final JsonObject object) {
            if (object == null) {
                throw new IllegalArgumentException("JSON object must not be null");
            }
            appendFieldUnescaped(key, object.toString());
        }

        /**
         * Appends a field to the object.
         *
         * @param key          The key of the field.
         * @param escapedValue The escaped value of the field.
         */
        private void appendFieldUnescaped(final String key, final String escapedValue) {
            if (builder == null) {
                throw new IllegalStateException("JSON has already been built");
            }
            if (key == null) {
                throw new IllegalArgumentException("JSON key must not be null");
            }
            if (hasAtLeastOneField) {
                builder.append(",");
            }
            builder.append("\"").append(escape(key)).append("\":").append(escapedValue);
            hasAtLeastOneField = true;
        }

        /**
         * Builds the JSON string and invalidates this builder.
         *
         * @return The built JSON string.
         */
        private JsonObject build() {
            if (builder == null) {
                throw new IllegalStateException("JSON has already been built");
            }
            final JsonObject object = new JsonObject(builder.append("}").toString());
            builder = null;
            return object;
        }

        /**
         * A super simple representation of a JSON object.
         *
         * <p>This class only exists to make methods of the {@link JsonObjectBuilder} type-safe and not
         * allow a raw string inputs for methods like {@link JsonObjectBuilder#appendField(String,
         * JsonObject)}.
         */
        private static final class JsonObject {

            private final String value;

            private JsonObject(final String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return value;
            }
        }
    }

}