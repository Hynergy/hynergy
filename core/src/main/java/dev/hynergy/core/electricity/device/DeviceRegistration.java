package dev.hynergy.core.electricity.device;

import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.RegisteredDeviceType;

import java.util.Objects;

/**
 * Associates one asset ID with a declaration and its runtime binding.
 */
public record DeviceRegistration(String id, DeviceType type, RegisteredDeviceType binding) {
    public DeviceRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(binding, "binding");
        if (binding.type() != type) throw new IllegalArgumentException("Binding belongs to another declaration");
    }
}
