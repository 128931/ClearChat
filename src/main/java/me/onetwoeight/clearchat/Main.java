package me.onetwoeight.clearchat;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import static java.util.Objects.requireNonNull;

public final class Main extends JavaPlugin {

    private final PluginDescriptionFile file = getDescription();
    private final String version = file.getVersion();
    private final String name = file.getName();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        requireNonNull(getCommand("cc"), "getCommand must not be null").setExecutor(new ChatListener(this));
        getLogger().info(name + " v" + version + " Enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info(name + " v" + version + " Disabled");
    }
}
