package dev.hynergy.core;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

@SuppressWarnings("unused")
public class HynergyPlugin extends JavaPlugin {
    public HynergyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("Setting up Hynergy");
    }
}
