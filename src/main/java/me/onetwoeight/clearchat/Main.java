package me.onetwoeight.clearchat;

import me.onetwoeight.clearchat.listeners.ChatListener;
import me.onetwoeight.clearchat.statistics.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

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
        PluginCommand pluginCommand = getCommand("cc");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(new ChatListener(this));
        } else {
            getLogger().severe("getCommand(\"cc\") is null disabling " + name);
            getPluginLoader().disablePlugin(this);
            return;
        }
        new Metrics(this, 14968);
        getLogger().info(() -> name + " v" + version + " Enabled"); // since Java 8, we can use Supplier, which will be evaluated lazily
    }

    @Override
    public void onDisable() {
        getLogger().info(() -> name + " v" + version + " Disabled"); // since Java 8, we can use Supplier, which will be evaluated lazily
    }
}
