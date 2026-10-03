package dev.hynergy.electrical;

/**
 * Low-level infrastructure access to a device by native definition indexes.
 *
 * <p>This bridge is intended for generic integration code that cannot use a
 * concrete device's semantic API. Parameter, terminal, and observer IDs here
 * are native definition-order indexes; they are not persistent stable IDs.</p>
 *
 * <p>Validation and ownership checks remain owned by {@link Device} and
 * {@link ElectricalSystem}; this class deliberately does not duplicate them.</p>
 */
public final class DeviceAccess {
    private DeviceAccess() {
    }

    /** Checks native constraints without enqueueing a parameter command. */
    public static void validateParameter(Device device, int nativeParameterId, double value) {
        device.validateParameter(nativeParameterId, value);
    }

    public static void setParameter(
            Device device,
            int nativeParameterId,
            double value
    ) {
        device.setParameter(nativeParameterId, value);
    }

    public static void attachTerminal(Device device, int nativeTerminalId, Wire wire) {
        device.attachTerminal(nativeTerminalId, wire);
    }

    public static void detachTerminal(Device device, int nativeTerminalId, Wire wire) {
        device.detachTerminal(nativeTerminalId, wire);
    }

    public static ObservationSubscription observe(
            Device device,
            int nativeObserverId,
            ObservationListener listener
    ) {
        return device.observe(nativeObserverId, listener);
    }
}
