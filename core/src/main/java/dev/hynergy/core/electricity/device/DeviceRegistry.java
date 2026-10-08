package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.RegisteredDeviceType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registers asset IDs during setup.
 * The registry publishes an asset ID after native registration succeeds.
 */
public final class DeviceRegistry {
    public static final String DATA_SET = "DeviceTypes";
    private final ElectricalRuntime runtime;
    private final Map<String, DeviceRegistration> registrations = new HashMap<>();
    private boolean frozen;

    public DeviceRegistry(ElectricalRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    /**
     * Registers a complete declaration for asset use during plugin setup.
     * The registry publishes the asset ID only after native registration succeeds.
     * Different IDs can share one declaration and runtime binding.
     * @param id the asset ID with one colon and nonblank namespace and local name
     * @param type the immutable device declaration
     * @return the asset registration and runtime binding
     * @throws IllegalArgumentException if the ID or declaration is invalid
     * @throws IllegalStateException if the ID already exists, the registry is frozen, or native registration fails
     */
    public synchronized DeviceRegistration register(String id, DeviceType type) {
        requireOpen();
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        int colon = id.indexOf(':');
        if (colon < 0 || colon != id.lastIndexOf(':') || id.substring(0, colon).isBlank() || id.substring(colon + 1)
                                                                                               .isBlank())
            throw new IllegalArgumentException("Device type ID must contain one namespace and local name: " + id);
        if (registrations.containsKey(id)) throw new IllegalStateException("Device type is already registered: " + id);
        final RegisteredDeviceType binding;
        try {
            binding = runtime.register(type);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Cannot register device type " + id + ": " + failure.getMessage(), failure);
        } catch (IllegalStateException failure) {
            throw new IllegalStateException("Cannot register device type " + id + ": " + failure.getMessage(), failure);
        }
        var registration = new DeviceRegistration(id, type, binding);
        registrations.put(id, registration);
        return registration;
    }

    public synchronized @Nullable DeviceRegistration get(String id) {
        return registrations.get(Objects.requireNonNull(id, "id"));
    }

    public synchronized DeviceRegistration require(String id) {
        var entry = get(id);
        if (entry == null) throw new IllegalArgumentException("Unknown device type: " + id);
        return entry;
    }

    public synchronized void populateDataSet(AssetEditorRequestDataSetEvent event) {
        event.setResults(registrations.keySet().stream().sorted().toArray(String[]::new));
    }

    public synchronized boolean isFrozen() {
        return frozen;
    }

    /**
     * Prevents further registrations while keeping existing entries available.
     * @throws IllegalStateException if the registry is already frozen
     */
    public synchronized void freeze() {
        requireOpen();
        frozen = true;
    }

    private void requireOpen() {
        if (frozen) throw new IllegalStateException("Device registry is frozen");
    }
}
