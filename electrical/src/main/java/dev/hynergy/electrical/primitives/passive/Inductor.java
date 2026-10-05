package dev.hynergy.electrical.primitives.passive;

import dev.hynergy.electrical.*;

/**
 * Models an inductor between a positive terminal and a negative terminal.
 *
 * <p>Voltage is the positive-terminal voltage minus the negative-terminal voltage.
 * Positive current flows from the positive terminal to the negative terminal.</p>
 */
public final class Inductor {
    /**
     * The device type for Inductor.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.INDUCTOR;

    public static final DeviceTerminal POSITIVE = TYPE.terminal(0);
    public static final DeviceTerminal NEGATIVE = TYPE.terminal(1);

    public static final DeviceParameter INDUCTANCE = TYPE.parameter(0);

    public static final DeviceObserver VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver CURRENT = TYPE.observer(1);

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public Inductor(Device device) {
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
     * Creates an inductor.
     *
     * @param system the electrical system
     * @param inductance the inductance, in henries; the value must be finite and greater than zero
     *
     * @return the inductor
     */
    public static Inductor create(ElectricalSystem system, double inductance) {
        Inductor device = new Inductor(system.create(TYPE));

        device.setInductance(inductance);

        return device;
    }

    /**
     * Sets the inductance.
     *
     * @param inductance the inductance, in henries; the value must be finite and greater than zero
     */
    public void setInductance(double inductance) {
        device.setParameter(INDUCTANCE, inductance);
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
     * Subscribes to the voltage across this inductor.
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
     * Subscribes to the current through this inductor.
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
