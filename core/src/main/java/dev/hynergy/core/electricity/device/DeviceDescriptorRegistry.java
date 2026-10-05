package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import dev.hynergy.electrical.Device;
import dev.hynergy.electrical.DeviceType;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Setup-time registry for stable electrical device descriptors.
 *
 * <p>Registration is disabled permanently after {@link #freeze()}.</p>
 */
public final class DeviceDescriptorRegistry {
    public static final String DATA_SET = "DeviceTypes";

    private final Map<String, DeviceDescriptor<?>> descriptors = new HashMap<>();
    @Getter private boolean frozen;

    /**
     * Registers one stable device descriptor.
     *
     * @return the registered descriptor
     * @throws IllegalStateException if the registry is frozen or the ID is
     *                               already registered
     */
    public <T extends Device> DeviceDescriptor<T> register(
            String id,
            DeviceType<T> type,
            MemberMapping parameters,
            MemberMapping terminals,
            MemberMapping observers
    ) {
        requireOpen();

        DeviceDescriptor<T> descriptor = new DeviceDescriptor<>(id, type, parameters, terminals, observers);
        if (descriptors.putIfAbsent(descriptor.id(), descriptor) != null) {
            throw new IllegalStateException("Device descriptor is already registered: " + descriptor.id());
        }

        return descriptor;
    }

    /**
     * Returns a descriptor by stable ID, or {@code null} when it is unknown.
     */
    public @Nullable DeviceDescriptor<?> get(String id) {
        Objects.requireNonNull(id, "id");
        return descriptors.get(id);
    }

    /** Returns the registered IDs in stable order for the asset editor. */
    public void populateDataSet(AssetEditorRequestDataSetEvent event) {
        event.setResults(descriptors.keySet().stream().sorted().toArray(String[]::new));
    }

    /**
     * Returns a descriptor by stable ID.
     *
     * @throws IllegalArgumentException if the ID is unknown
     */
    public DeviceDescriptor<?> require(String id) {
        DeviceDescriptor<?> descriptor = get(id);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown device descriptor: " + id);
        }
        return descriptor;
    }

    /**
     * Permanently closes this registry to further registration.
     *
     * @throws IllegalStateException if the registry is already frozen
     */
    public void freeze() {
        requireOpen();
        frozen = true;
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("Device descriptor registry is frozen");
        }
    }
}
