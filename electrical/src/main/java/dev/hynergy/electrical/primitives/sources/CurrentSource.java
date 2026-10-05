package dev.hynergy.electrical.primitives.sources;

import dev.hynergy.electrical.*;

/**
 * Models an ideal current source between a positive terminal and a negative terminal.
 *
 * <p>Voltage is the positive-terminal voltage minus the negative-terminal voltage.
 * Positive current flows from the positive terminal to the negative terminal.</p>
 */
public final class CurrentSource {
    /**
     * The device type for CurrentSource.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.CURRENT_SOURCE;

    public static final DeviceTerminal POSITIVE = TYPE.terminal(0);
    public static final DeviceTerminal NEGATIVE = TYPE.terminal(1);

    public static final DeviceParameter CURRENT = TYPE.parameter(0);

    public static final DeviceObserver VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver CURRENT_OBSERVER = TYPE.observer(1);

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public CurrentSource(Device device) {
        java.util.Objects.requireNonNull(device, "device").requireDefinition(TYPE);
        this.device = device;
    }

    /** Returns the underlying runtime handle. */
    public Device device() {
        return device;
    }

    /** Returns the runtime identity. */
    public DeviceId id() {
        return device.id();
    }

    /** Destroys the underlying device and its observation subscriptions. */
    public void destroy() {
        device.destroy();
    }

    /**
     * Creates a current source.
     *
     * @param system the electrical system
     * @param current the source current, in amperes; the value must be finite
     *
     * @return the current source
     */
    public static CurrentSource create(ElectricalSystem system, double current) {
        CurrentSource device = new CurrentSource(system.create(TYPE));

        device.setCurrent(current);

        return device;
    }

    /**
     * Sets the current.
     *
     * @param current the source current, in amperes; the value must be finite
     */
    public void setCurrent(double current) {
        device.setParameter(CURRENT, current);
    }

    /**
     * Attaches the positive terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachPositive(Wire wire) {
        device.attachTerminal(POSITIVE, wire);
    }

    /**
     * Attaches the negative terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachNegative(Wire wire) {
        device.attachTerminal(NEGATIVE, wire);
    }

    /**
     * Detaches the positive terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachPositive(Wire wire) {
        device.detachTerminal(POSITIVE, wire);
    }

    /**
     * Detaches the negative terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachNegative(Wire wire) {
        device.detachTerminal(NEGATIVE, wire);
    }

    /**
     * Subscribes to the voltage across this current source.
     *
     * <p>Positive voltage is measured from the positive terminal to the negative terminal.</p>
     *
     * @param listener the observation listener
     *
     * @return the observation subscription
     */
    public ObservationSubscription observeVoltage(
        ObservationListener listener
    ) {
        return device.observe(VOLTAGE, listener);
    }

    /**
     * Subscribes to the current through this current source.
     *
     * <p>Positive current flows from the positive terminal to the negative terminal.</p>
     *
     * @param listener the observation listener
     *
     * @return the observation subscription
     */
    public ObservationSubscription observeCurrent(
        ObservationListener listener
    ) {
        return device.observe(CURRENT_OBSERVER, listener);
    }
}
