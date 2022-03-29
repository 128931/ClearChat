package me.onetwoeight.clearchat;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        PluginDescriptionFile file = getDescription();
        String version = file.getVersion();
        String name = file.getName();
        saveDefaultConfig();
        getCommand("cc").setExecutor(new ChatListener(this));
        getLogger().info(name + " v" + version + " Enabled");
    }

    @Override
    public void onDisable() {

    }
}
