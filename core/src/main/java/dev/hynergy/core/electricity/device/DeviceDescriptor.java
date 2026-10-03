package dev.hynergy.core.electricity.device;

import dev.hynergy.electrical.Device;
import dev.hynergy.electrical.DeviceType;

import java.util.Objects;

/**
 * Bridges one stable device config type to its electrical device definition
 * and stable member mappings.
 *
 * @param id         the stable descriptor ID
 * @param type       the electrical device type
 * @param parameters stable parameter IDs to native parameter indexes
 * @param terminals  stable terminal IDs to native terminal indexes
 * @param observers  stable observer IDs to native observer indexes
 * @param <T>        the electrical device class
 */
public record DeviceDescriptor<T extends Device>(
        String id,
        DeviceType<T> type,
        MemberMapping parameters,
        MemberMapping terminals,
        MemberMapping observers
) {
    public DeviceDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(terminals, "terminals");
        Objects.requireNonNull(observers, "observers");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Device descriptor id must not be blank");
        }
    }
}
