package me.onetwoeight.clearchat;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Main extends JavaPlugin {

    private final PluginDescriptionFile file = getDescription();
    private final String version = file.getVersion();
    private final String name = file.getName();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Objects.requireNonNull(getCommand("cc"), "Executor must not be null").setExecutor(new ChatListener(this));
        getLogger().info(name + " v" + version + " Enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info(name + " v" + version + " Disabled");
    }
}
