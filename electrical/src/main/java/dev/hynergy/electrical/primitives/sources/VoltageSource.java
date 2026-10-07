package dev.hynergy.electrical.primitives.sources;

import dev.hynergy.electrical.*;

import java.util.Objects;

/**
 * Models an ideal voltage source between a positive terminal and a negative terminal.
 *
 * <p>Voltage is the positive-terminal voltage minus the negative-terminal voltage.
 * Positive current flows from the positive terminal to the negative terminal.</p>
 */
public record VoltageSource(Device device) {
    public static final DeviceType TYPE = PrimitiveDeviceTypes.VOLTAGE_SOURCE;

    public static final DeviceTerminal POSITIVE = TYPE.terminal(0);
    public static final DeviceTerminal NEGATIVE = TYPE.terminal(1);

    public static final DeviceParameter VOLTAGE = TYPE.parameter(0);

    public static final DeviceObserver VOLTAGE_OBSERVER = TYPE.observer(0);
    public static final DeviceObserver CURRENT = TYPE.observer(1);

    /**
     * Uses a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the device has a different type
     * @throws IllegalStateException    if the device is no longer usable
     */
    public VoltageSource {
        Objects.requireNonNull(device, "device").requireDefinition(TYPE);
    }

    @Override
    public Device device() {
        return device;
    }

    public DeviceId id() {
        return device.id();
    }

    /**
     * Destroys the device and deactivates its observation subscriptions.
     */
    public void destroy() {
        device.destroy();
    }

    /**
     * Creates a voltage source.
     *
     * @param system  the electrical system
     * @param voltage the source voltage, in volts. The value must be finite.
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
     * @param voltage the source voltage, in volts. The value must be finite.
     */
    public void setVoltage(double voltage) {
        device.setParameter(VOLTAGE, voltage);
    }

    public void attachPositive(Wire wire) {
        device.attachTerminal(POSITIVE, wire);
    }

    public void attachNegative(Wire wire) {
        device.attachTerminal(NEGATIVE, wire);
    }

    public void detachPositive(Wire wire) {
        device.detachTerminal(POSITIVE, wire);
    }

    public void detachNegative(Wire wire) {
        device.detachTerminal(NEGATIVE, wire);
    }

    /**
     * Subscribes to the voltage across this voltage source.
     *
     * <p>Positive voltage is measured from the positive terminal to the negative terminal.</p>
     *
     * @param listener the observation listener
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
     * @return the observation subscription
     */
    public ObservationSubscription observeCurrent(
            ObservationListener listener
    ) {
        return device.observe(CURRENT, listener);
    }
}
