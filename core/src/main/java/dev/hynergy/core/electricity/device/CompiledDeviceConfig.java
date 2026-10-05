package dev.hynergy.core.electricity.device;

import dev.hynergy.core.port.BlockPortDefinition;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, resolved asset configuration shared by runtime devices.
 * Stable parameter IDs remain available for persisted overrides.
 */
public final class CompiledDeviceConfig {
    private final DeviceDescriptor descriptor;
    private final List<ParameterBinding> parameters;
    private final List<PortBinding> ports;
    private final BlockPortDefinition portDefinition;

    CompiledDeviceConfig(DeviceDescriptor descriptor, List<ParameterBinding> parameters,
                         List<PortBinding> ports, BlockPortDefinition portDefinition) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.parameters = List.copyOf(parameters);
        this.ports = List.copyOf(ports);
        this.portDefinition = Objects.requireNonNull(portDefinition, "portDefinition");
        if (ports.size() != portDefinition.size()) {
            throw new IllegalArgumentException("Compiled ports must match the block port definition");
        }
    }

    public record ParameterBinding(int stableId, int nativeId, double defaultValue) { }
    public record PortBinding(int portId, int nativeTerminalId) { }

    public DeviceDescriptor descriptor() { return descriptor; }
    public List<ParameterBinding> parameters() { return parameters; }
    public List<PortBinding> ports() { return ports; }
    public BlockPortDefinition portDefinition() { return portDefinition; }

    public @Nullable ParameterBinding parameter(int stableId) {
        for (ParameterBinding parameter : parameters) {
            if (parameter.stableId() == stableId) {
                return parameter;
            }
        }
        return null;
    }

    public int nativeParameterId(int stableId) {
        return descriptor.parameters().nativeIndex(stableId);
    }

    public int nativeObserverId(int stableId) {
        return descriptor.observers().nativeIndex(stableId);
    }
}
