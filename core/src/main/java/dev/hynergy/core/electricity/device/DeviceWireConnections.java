package dev.hynergy.core.electricity.device;

import dev.hynergy.electrical.Device;
import dev.hynergy.electrical.Wire;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/** Connects block-derived device terminals without exposing runtime device handles. */
public final class DeviceWireConnections {
    private DeviceWireConnections() {
    }

    /**
     * Resolves a discovered port through the target's bound configuration.
     * Unbound targets and ports absent from that configuration are skipped.
     */
    public static boolean attachPortIfNew(
            @Nullable DeviceComponent component,
            int portId,
            @Nullable Wire wire,
            AttachmentDedup dedup
    ) {
        Objects.requireNonNull(dedup, "dedup");
        if (component == null || wire == null) {
            return false;
        }
        Device device = component.device();
        CompiledDeviceConfig compiled = component.compiledConfig();
        if (device == null || compiled == null) {
            return false;
        }
        for (var port : compiled.ports()) {
            if (port.portId() == portId) {
                return attachIfNew(device, port.terminal(), wire, dedup);
            }
        }
        return false;
    }

    static boolean attachIfNew(
            Device device,
            dev.hynergy.electrical.DeviceTerminal terminal,
            @Nullable Wire wire,
            AttachmentDedup dedup
    ) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(dedup, "dedup");
        if (wire == null || !dedup.add(device.id().packed(), terminal.id(), wire.id().packed())) {
            return false;
        }
        device.attachTerminal(terminal, wire);
        return true;
    }

    /** Reuses storage for one discovery pass. */
    public static final class AttachmentDedup {
        private long[] deviceIds = new long[8];
        private int[] terminalIds = new int[8];
        private long[] wireIds = new long[8];
        private int size;

        private boolean add(long deviceId, int terminalId, long wireId) {
            for (int index = 0; index < size; index++) {
                if (deviceIds[index] == deviceId && terminalIds[index] == terminalId && wireIds[index] == wireId) {
                    return false;
                }
            }
            if (size == terminalIds.length) {
                int newCapacity = Math.multiplyExact(size, 2);
                deviceIds = Arrays.copyOf(deviceIds, newCapacity);
                terminalIds = Arrays.copyOf(terminalIds, newCapacity);
                wireIds = Arrays.copyOf(wireIds, newCapacity);
            }
            deviceIds[size] = deviceId;
            terminalIds[size] = terminalId;
            wireIds[size] = wireId;
            size++;
            return true;
        }

        public void clear() {
            size = 0;
        }
    }
}
