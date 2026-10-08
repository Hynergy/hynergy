package dev.hynergy.electrical.primitives.passive;

import dev.hynergy.electrical.*;

import java.util.Objects;

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
public record Diode(Device device) {
    public static final DeviceType TYPE = PrimitiveDeviceTypes.DIODE;

    public static final DeviceTerminal ANODE = TYPE.terminal(0);
    public static final DeviceTerminal CATHODE = TYPE.terminal(1);

    public static final DeviceParameter MAXIMUM_CONDUCTANCE = TYPE.parameter(0);
    public static final DeviceParameter MINIMUM_CONDUCTANCE = TYPE.parameter(1);

    public static final DeviceObserver VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver CURRENT = TYPE.observer(1);

    /**
     * Uses a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the device has a different type
     * @throws IllegalStateException    if the device is no longer usable
     */
    public Diode {
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
     * Creates a diode.
     *
     * @param system             the electrical system
     * @param maximumConductance the forward conductance, in siemens. The value
     *                           must be finite and greater than zero.
     * @param minimumConductance the reverse conductance, in siemens. The value
     *                           must be finite and zero or greater.
     *                           The value must be less than {@code maximumConductance}.
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
     * @param maximumConductance the conductance, in siemens. The value must be
     *                           finite and greater than the current minimum conductance.
     *                           The value must also be greater than zero.
     */
    public void setMaximumConductance(double maximumConductance) {
        device.setParameter(MAXIMUM_CONDUCTANCE, maximumConductance);
    }

    /**
     * Sets the minimum conductance.
     *
     * @param minimumConductance the conductance, in siemens. The value must be
     *                           finite and zero or greater.
     *                           The value must be less than the current maximum conductance.
     */
    public void setMinimumConductance(double minimumConductance) {
        device.setParameter(MINIMUM_CONDUCTANCE, minimumConductance);
    }

    public void attachAnode(Wire wire) {
        device.attachTerminal(ANODE, wire);
    }

    public void attachCathode(Wire wire) {
        device.attachTerminal(CATHODE, wire);
    }

    public void detachAnode(Wire wire) {
        device.detachTerminal(ANODE, wire);
    }

    public void detachCathode(Wire wire) {
        device.detachTerminal(CATHODE, wire);
    }

    /**
     * Subscribes to the voltage across this diode.
     *
     * <p>Positive voltage is measured from the anode to the cathode.</p>
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
     * Subscribes to the current through this diode.
     *
     * <p>Positive current flows from the anode to the cathode.</p>
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
