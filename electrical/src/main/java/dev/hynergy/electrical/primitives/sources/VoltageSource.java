package dev.hynergy.electrical.primitives.sources;

import dev.hynergy.electrical.*;

/**
 * Models an ideal voltage source between a positive terminal and a negative terminal.
 *
 * <p>Voltage is the positive-terminal voltage minus the negative-terminal voltage.
 * Positive current flows from the positive terminal to the negative terminal.</p>
 */
public final class VoltageSource {
    /**
     * The device type for VoltageSource.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.VOLTAGE_SOURCE;

    public static final DeviceTerminal POSITIVE = TYPE.terminal(0);
    public static final DeviceTerminal NEGATIVE = TYPE.terminal(1);

    public static final DeviceParameter VOLTAGE = TYPE.parameter(0);

    public static final DeviceObserver VOLTAGE_OBSERVER = TYPE.observer(0);
    public static final DeviceObserver CURRENT = TYPE.observer(1);

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public VoltageSource(Device device) {
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
     * Creates a voltage source.
     *
     * @param system the electrical system
     * @param voltage the source voltage, in volts; the value must be finite
     *
     * @return the voltage source
     */
    public static VoltageSource create(ElectricalSystem system, double voltage) {
        VoltageSource device = new VoltageSource(system.create(TYPE));

        device.setVoltage(voltage);

        return device;
    }

    /**
     * Sets the voltage.
     *
     * @param voltage the source voltage, in volts; the value must be finite
     */
    public void setVoltage(double voltage) {
        device.setParameter(VOLTAGE, voltage);
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
     * Subscribes to the voltage across this voltage source.
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
        return device.observe(VOLTAGE_OBSERVER, listener);
    }

    /**
     * Subscribes to the current through this voltage source.
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
        return device.observe(CURRENT, listener);
    }
}
