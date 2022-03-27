package me.onetwoeight.clearchat;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

// Bozo IntelliJ
@SuppressWarnings("unused")
public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        PluginDescriptionFile file = getDescription();
        String version = file.getVersion();
        String name = file.getName();
        getCommand("cc").setExecutor(new ClearChat());
        getLogger().info(name + " v" + version + " Enabled");
    }

    @Override
    public void onDisable() {

    }
}
