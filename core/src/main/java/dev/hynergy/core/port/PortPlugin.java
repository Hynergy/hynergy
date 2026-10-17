package dev.hynergy.core.port;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

@SuppressWarnings("unused")
public final class PortPlugin extends JavaPlugin {

    private final PortService ports = new PortService();

    public PortPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        ports.freeze();
    }

    public PortService ports() {
        return ports;
    }
}