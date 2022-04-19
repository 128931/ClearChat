package me.onetwoeight.clearchat;

import me.onetwoeight.clearchat.listener.ChatListener;
import me.onetwoeight.clearchat.statistics.Metrics;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import static java.util.Objects.requireNonNull;

/**
 * @author onetwoeight
 * @since 3/27/2022
 */
public final class Main extends JavaPlugin {

    private final PluginDescriptionFile file = getDescription();
    private final String version = file.getVersion();
    private final String name = file.getName();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        requireNonNull(getCommand("cc"), "Expression 'getCommand(\"cc\")' must not be null").setExecutor(new ChatListener(this));
        new Metrics(this, 14968);
        getLogger().info(() -> name + " v" + version + " Enabled"); // since Java 8, we can use Supplier, which will be evaluated lazily
    }

    @Override
    public void onDisable() {
        getLogger().info(() -> name + " v" + version + " Disabled"); // since Java 8, we can use Supplier, which will be evaluated lazily
    }
}
