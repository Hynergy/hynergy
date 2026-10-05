package dev.hynergy.electrical.primitives.passive;

import dev.hynergy.electrical.*;

/**
 * Models a two-state conductance diode.
 *
 * <p>Voltage is the anode voltage minus the cathode voltage. Positive current
 * flows from the anode to the cathode.</p>
 *
 * <p>The diode uses the maximum conductance when the anode-to-cathode voltage
 * is positive. It uses the minimum conductance when the voltage is zero or
 * negative.</p>
 */
public final class Diode {
    /**
     * The device type for {@code Diode}.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.DIODE;

    private static final int TERMINAL_ANODE = 0;
    private static final int TERMINAL_CATHODE = 1;

    private static final int PARAMETER_MAXIMUM_CONDUCTANCE = 0;
    private static final int PARAMETER_MINIMUM_CONDUCTANCE = 1;

    private static final int OBSERVER_VOLTAGE = 0;
    private static final int OBSERVER_CURRENT = 1;

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public Diode(Device device) {
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
     * Creates a diode.
     *
     * @param system the electrical system
     * @param maximumConductance the forward conductance, in siemens; the value
     *     must be finite and greater than zero
     * @param minimumConductance the reverse conductance, in siemens; the value
     *     must be finite and non-negative, and less than
     *     {@code maximumConductance}
     *
     * @return the diode
     */
    public static Diode create(ElectricalSystem system, double maximumConductance, double minimumConductance) {
        Diode device = new Diode(system.create(TYPE));

        device.setMaximumConductance(maximumConductance);
        device.setMinimumConductance(minimumConductance);

        return device;
    }

    /**
     * Sets the maximum conductance.
     *
     * @param maximumConductance the conductance, in siemens; the value must be
     *     finite, greater than zero, and greater than the current minimum
     *     conductance
     */
    public void setMaximumConductance(double maximumConductance) {
        device.setParameter(PARAMETER_MAXIMUM_CONDUCTANCE, maximumConductance);
    }

    /**
     * Sets the minimum conductance.
     *
     * @param minimumConductance the conductance, in siemens; the value must be
     *     finite, non-negative, and less than the current maximum conductance
     */
    public void setMinimumConductance(double minimumConductance) {
        device.setParameter(PARAMETER_MINIMUM_CONDUCTANCE, minimumConductance);
    }

    /**
     * Attaches the anode to a wire.
     *
     * @param wire the wire
     */
    public void attachAnode(Wire wire) {
        device.attachTerminal(TERMINAL_ANODE, wire);
    }

    /**
     * Attaches the cathode to a wire.
     *
     * @param wire the wire
     */
    public void attachCathode(Wire wire) {
        device.attachTerminal(TERMINAL_CATHODE, wire);
    }

    /**
     * Detaches the anode from a wire.
     *
     * @param wire the wire
     */
    public void detachAnode(Wire wire) {
        device.detachTerminal(TERMINAL_ANODE, wire);
    }

    /**
     * Detaches the cathode from a wire.
     *
     * @param wire the wire
     */
    public void detachCathode(Wire wire) {
        device.detachTerminal(TERMINAL_CATHODE, wire);
    }

    /**
     * Subscribes to the voltage across this diode.
     *
     * <p>Positive voltage is measured from the anode to the cathode.</p>
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
     * Subscribes to the current through this diode.
     *
     * <p>Positive current flows from the anode to the cathode.</p>
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
