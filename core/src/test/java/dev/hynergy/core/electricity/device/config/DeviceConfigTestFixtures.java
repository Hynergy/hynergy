package dev.hynergy.core.electricity.device.config;

import dev.hynergy.core.electricity.device.DeviceRegistration;
import dev.hynergy.core.port.layout.BlockPortDefinition;
import org.joml.Vector3i;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Test-only access to package-private configuration constructors. */
public final class DeviceConfigTestFixtures {
    private DeviceConfigTestFixtures() {
    }

    public static DeviceConfig config(
            String id,
            String type,
            DeviceParameterConfig @Nullable [] parameters,
            DevicePortConfig @Nullable [] ports
    ) {
        return new DeviceConfig(id, type, parameters, ports);
    }

    public static DeviceParameterConfig parameter(int id, double defaultValue) {
        return new DeviceParameterConfig(id, defaultValue);
    }

    public static DevicePortConfig port(
            int id,
            int terminal,
            @Nullable Vector3i anchor,
            Vector3i normal
    ) {
        return new DevicePortConfig(id, terminal, anchor, normal);
    }

    public static CompiledDeviceConfig compiled(
            DeviceRegistration registration,
            List<CompiledDeviceConfig.ParameterBinding> parameters,
            List<CompiledDeviceConfig.PortBinding> ports,
            BlockPortDefinition portDefinition
    ) {
        return new CompiledDeviceConfig(registration, parameters, ports, portDefinition);
    }
}
