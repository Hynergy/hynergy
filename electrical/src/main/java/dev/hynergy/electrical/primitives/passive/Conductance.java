package dev.hynergy.electrical.primitives.passive;

import dev.hynergy.electrical.*;

/**
 * Models a conductance between a positive terminal and a negative terminal.
 *
 * <p>Voltage is the positive-terminal voltage minus the negative-terminal voltage.
 * Positive current flows from the positive terminal to the negative terminal.</p>
 */
public final class Conductance {
    /**
     * The device type for Conductance.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.CONDUCTANCE;

    private static final int TERMINAL_POSITIVE = 0;
    private static final int TERMINAL_NEGATIVE = 1;

    private static final int PARAMETER_CONDUCTANCE = 0;

    private static final int OBSERVER_VOLTAGE = 0;
    private static final int OBSERVER_CURRENT = 1;

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public Conductance(Device device) {
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
     * Creates a conductance element.
     *
     * @param system the electrical system
     * @param conductance the conductance, in siemens; the value must be finite and non-negative
     *
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
     * @param conductance the conductance, in siemens; the value must be finite and non-negative
     */
    public void setConductance(double conductance) {
        device.setParameter(PARAMETER_CONDUCTANCE, conductance);
    }

    /**
     * Attaches the positive terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachPositive(Wire wire) {
        device.attachTerminal(TERMINAL_POSITIVE, wire);
    }

    /**
     * Attaches the negative terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachNegative(Wire wire) {
        device.attachTerminal(TERMINAL_NEGATIVE, wire);
    }

    /**
     * Detaches the positive terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachPositive(Wire wire) {
        device.detachTerminal(TERMINAL_POSITIVE, wire);
    }

    /**
     * Detaches the negative terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachNegative(Wire wire) {
        device.detachTerminal(TERMINAL_NEGATIVE, wire);
    }

    /**
     * Subscribes to the voltage across this conductance element.
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
        return device.observe(OBSERVER_VOLTAGE, listener);
    }

    /**
     * Subscribes to the current through this conductance element.
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
        return device.observe(OBSERVER_CURRENT, listener);
    }
}
