package dev.hynergy.core.electricity.device;

import dev.hynergy.core.port.BlockPortDefinition;

import java.util.Objects;

/**
 * Immutable runtime form of one device asset configuration.
 *
 * <p>Stable member IDs are retained for the Hytale/config boundary while the
 * corresponding native indexes are compiled once for runtime use.</p>
 */
public final class CompiledDeviceConfig {
    private final DeviceDescriptor<?> descriptor;

    private final int[] stableParameterIds;
    private final int[] nativeParameterIds;
    private final double[] parameterDefaults;

    private final int[] portIds;
    private final int[] stableTerminalIds;
    private final int[] nativeTerminalIds;
    private final BlockPortDefinition portDefinition;

    CompiledDeviceConfig(
            DeviceDescriptor<?> descriptor,
            int[] stableParameterIds,
            int[] nativeParameterIds,
            double[] parameterDefaults,
            int[] portIds,
            int[] stableTerminalIds,
            int[] nativeTerminalIds,
            BlockPortDefinition portDefinition
    ) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.stableParameterIds = stableParameterIds.clone();
        this.nativeParameterIds = nativeParameterIds.clone();
        this.parameterDefaults = parameterDefaults.clone();
        this.portIds = portIds.clone();
        this.stableTerminalIds = stableTerminalIds.clone();
        this.nativeTerminalIds = nativeTerminalIds.clone();
        this.portDefinition = Objects.requireNonNull(portDefinition, "portDefinition");

        if (this.stableParameterIds.length != this.nativeParameterIds.length
                || this.stableParameterIds.length != this.parameterDefaults.length) {
            throw new IllegalArgumentException("Compiled parameter arrays must have equal lengths");
        }
        if (this.portIds.length != this.stableTerminalIds.length
                || this.portIds.length != this.nativeTerminalIds.length
                || this.portIds.length != portDefinition.size()) {
            throw new IllegalArgumentException("Compiled port arrays must match the block port definition");
        }
    }

    public DeviceDescriptor<?> descriptor() {
        return descriptor;
    }

    public int nativeParameterId(int stableParameterId) {
        return descriptor.parameters().nativeIndex(stableParameterId);
    }

    public int nativeTerminalId(int stableTerminalId) {
        return descriptor.terminals().nativeIndex(stableTerminalId);
    }

    public int nativeObserverId(int stableObserverId) {
        return descriptor.observers().nativeIndex(stableObserverId);
    }

    public int parameterCount() {
        return stableParameterIds.length;
    }

    public int stableParameterIdAt(int index) {
        return stableParameterIds[index];
    }

    public int nativeParameterIdAt(int index) {
        return nativeParameterIds[index];
    }

    public double parameterDefaultAt(int index) {
        return parameterDefaults[index];
    }

    public int findParameterDefault(int stableParameterId) {
        for (int index = 0; index < stableParameterIds.length; index++) {
            if (stableParameterIds[index] == stableParameterId) {
                return index;
            }
        }
        return -1;
    }

    public int portCount() {
        return portIds.length;
    }

    public int portIdAt(int index) {
        return portIds[index];
    }

    public int stableTerminalIdAt(int index) {
        return stableTerminalIds[index];
    }

    public int nativeTerminalIdAt(int index) {
        return nativeTerminalIds[index];
    }

    public BlockPortDefinition portDefinition() {
        return portDefinition;
    }
}
