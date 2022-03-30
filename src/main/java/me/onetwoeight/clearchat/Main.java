package me.onetwoeight.clearchat;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        PluginDescriptionFile file = getDescription();
        String version = file.getVersion();
        String name = file.getName();
        saveDefaultConfig();
        Objects.requireNonNull(getCommand("cc"), "Executor must not be null").setExecutor(new ChatListener(this));
        getLogger().info(name + " v" + version + " Enabled");
    }

    @Override
    public void onDisable() {

    }
}
