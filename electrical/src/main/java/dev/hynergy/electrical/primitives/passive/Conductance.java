package dev.hynergy.electrical.primitives.passive;

import dev.hynergy.electrical.*;

import java.util.Objects;

/**
 * Models a conductance between a positive terminal and a negative terminal.
 *
 * <p>Voltage is the positive-terminal voltage minus the negative-terminal voltage.
 * Positive current flows from the positive terminal to the negative terminal.</p>
 */
public record Conductance(Device device) {
    public static final DeviceType TYPE = PrimitiveDeviceTypes.CONDUCTANCE;

    public static final DeviceTerminal POSITIVE = TYPE.terminal(0);
    public static final DeviceTerminal NEGATIVE = TYPE.terminal(1);

    public static final DeviceParameter CONDUCTANCE = TYPE.parameter(0);

    public static final DeviceObserver VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver CURRENT = TYPE.observer(1);

    /**
     * Uses a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the device has a different type
     * @throws IllegalStateException    if the device is no longer usable
     */
    public Conductance {
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
     * Creates a conductance element.
     *
     * @param system      the electrical system
     * @param conductance the conductance, in siemens. The value must be finite and zero or greater.
     * @return the conductance element
     */
    public static Conductance create(ElectricalSystem system, double conductance) {
        Conductance device = new Conductance(system.create(TYPE));

        device.setConductance(conductance);

        return device;
    }

    /**
     * Sets the conductance.
     *
     * @param conductance the conductance, in siemens. The value must be finite and zero or greater.
     */
    public void setConductance(double conductance) {
        device.setParameter(CONDUCTANCE, conductance);
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
     * Subscribes to the voltage across this conductance element.
     *
     * <p>Positive voltage is measured from the positive terminal to the negative terminal.</p>
     *
     * @param listener the observation listener
     * @return the observation subscription
     */
    public ObservationSubscription observeVoltage(
            ObservationListener listener
    ) {
        return device.observe(VOLTAGE, listener);
    }

    /**
     * Subscribes to the current through this conductance element.
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
