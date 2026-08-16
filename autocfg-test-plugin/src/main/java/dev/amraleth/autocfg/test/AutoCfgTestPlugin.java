package dev.amraleth.autocfg.test;

import dev.amraleth.autocfg.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class AutoCfgTestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            TestConfig testConfig = ConfigLoader.load(new File(getDataFolder(), "config.yml"), TestConfig.class);

            getLogger().info(testConfig.name());
            getLogger().info(String.valueOf(testConfig.material()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
