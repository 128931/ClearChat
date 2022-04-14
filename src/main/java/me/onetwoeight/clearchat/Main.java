package me.onetwoeight.clearchat;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

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
        requireNonNull(getCommand("cc"), "getCommand must not be null").setExecutor(new ChatListener(this));
        getLogger().log(Level.INFO, () -> name + " v" + version + " Enabled"); // since Java 8, we can use Supplier, which will be evaluated lazily
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, () -> name + " v" + version + " Disabled"); // since Java 8, we can use Supplier, which will be evaluated lazily
    }
}
