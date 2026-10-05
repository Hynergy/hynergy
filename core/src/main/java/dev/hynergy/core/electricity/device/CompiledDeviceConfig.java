package dev.hynergy.core.electricity.device;

import dev.hynergy.core.port.BlockPortDefinition;
import dev.hynergy.electrical.DeviceParameter;
import dev.hynergy.electrical.DeviceTerminal;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Immutable asset configuration. Persistence uses the declaration's stable member IDs.
 */
public final class CompiledDeviceConfig {
    private final DeviceRegistration registration;
    private final List<ParameterBinding> parameters;
    private final List<PortBinding> ports;
    private final BlockPortDefinition portDefinition;

    CompiledDeviceConfig(DeviceRegistration registration, List<ParameterBinding> parameters,
                         List<PortBinding> ports, BlockPortDefinition portDefinition) {
        this.registration = Objects.requireNonNull(registration, "registration");
        this.parameters = List.copyOf(parameters);
        this.ports = List.copyOf(ports);
        this.portDefinition = Objects.requireNonNull(portDefinition, "portDefinition");
        if (ports.size() != portDefinition.size())
            throw new IllegalArgumentException("Compiled ports must match the block port definition");
    }

    public record ParameterBinding(DeviceParameter parameter, double defaultValue) {
        public ParameterBinding {
            Objects.requireNonNull(parameter, "parameter");
        }

        public int stableId() {
            return parameter.id();
        }
    }

    public record PortBinding(int portId, DeviceTerminal terminal) {
        public PortBinding {
            Objects.requireNonNull(terminal, "terminal");
        }
    }

    public DeviceRegistration registration() {
        return registration;
    }

    public List<ParameterBinding> parameters() {
        return parameters;
    }

    public List<PortBinding> ports() {
        return ports;
    }

    public BlockPortDefinition portDefinition() {
        return portDefinition;
    }

    public @Nullable ParameterBinding parameter(int stableId) {
        for (var binding : parameters) if (binding.stableId() == stableId) return binding;
        return null;
    }

    public @Nullable DeviceParameter declaredParameter(int stableId) {
        for (var parameter : registration.type().parameters()) if (parameter.id() == stableId) return parameter;
        return null;
    }
}
