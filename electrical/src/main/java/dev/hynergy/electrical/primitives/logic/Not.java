package dev.hynergy.electrical.primitives.logic;

import dev.hynergy.electrical.*;

import java.util.Objects;

/**
 * Models a NOT gate with a finite-conductance output stage.
 *
 * <p>The input is high when its voltage relative to VSS is greater than or
 * equal to the threshold voltage. The output stage uses the maximum and
 * minimum conductances to connect the output to VDD or VSS.</p>
 *
 * <p>The output and input observation voltages use VSS as their reference.</p>
 */
public record Not(Device device) {
    public static final DeviceType TYPE = PrimitiveDeviceTypes.NOT;

    public static final DeviceTerminal OUTPUT = TYPE.terminal(0);
    public static final DeviceTerminal VDD = TYPE.terminal(1);
    public static final DeviceTerminal VSS = TYPE.terminal(2);
    public static final DeviceTerminal INPUT = TYPE.terminal(3);

    public static final DeviceParameter THRESHOLD_RELATIVE_TO_VSS = TYPE.parameter(0);
    public static final DeviceParameter MAXIMUM_CONDUCTANCE = TYPE.parameter(1);
    public static final DeviceParameter MINIMUM_CONDUCTANCE = TYPE.parameter(2);

    public static final DeviceObserver OUTPUT_VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver INPUT_VOLTAGE = TYPE.observer(1);
    public static final DeviceObserver SUPPLY_CURRENT = TYPE.observer(2);

    /**
     * Uses a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the device has a different type
     * @throws IllegalStateException    if the device is no longer usable
     */
    public Not {
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
     * Creates a NOT gate.
     *
     * @param system                 the electrical system
     * @param thresholdRelativeToVss the input threshold relative to VSS, in
     *                               volts. The value must be finite.
     * @param maximumConductance     the active output-stage conductance, in
     *                               siemens. The value must be finite and greater than zero.
     * @param minimumConductance     the inactive output-stage conductance, in
     *                               siemens. The value must be finite and zero or greater.
     *                               The value must be less than {@code maximumConductance}.
     * @return the NOT gate
     */
    public static Not create(
            ElectricalSystem system,
            double thresholdRelativeToVss,
            double maximumConductance,
            double minimumConductance
    ) {
        Not device = new Not(system.create(TYPE));

        device.setThresholdRelativeToVss(thresholdRelativeToVss);
        device.setMaximumConductance(maximumConductance);
        device.setMinimumConductance(minimumConductance);

        return device;
    }

    /**
     * Sets the input threshold relative to VSS.
     *
     * @param thresholdRelativeToVss the threshold voltage, in volts. The value
     *                               must be finite.
     */
    public void setThresholdRelativeToVss(double thresholdRelativeToVss) {
        device.setParameter(THRESHOLD_RELATIVE_TO_VSS, thresholdRelativeToVss);
    }

    /**
     * Sets the maximum output-stage conductance.
     *
     * @param maximumConductance the conductance, in siemens. The value must be
     *                           finite and greater than the current minimum conductance.
     *                           The value must also be greater than zero.
     */
    public void setMaximumConductance(double maximumConductance) {
        device.setParameter(MAXIMUM_CONDUCTANCE, maximumConductance);
    }

    /**
     * Sets the minimum output-stage conductance.
     *
     * @param minimumConductance the conductance, in siemens. The value must be
     *                           finite and zero or greater.
     *                           The value must be less than the current maximum conductance.
     */
    public void setMinimumConductance(double minimumConductance) {
        device.setParameter(MINIMUM_CONDUCTANCE, minimumConductance);
    }

    public void attachOutput(Wire wire) {
        device.attachTerminal(OUTPUT, wire);
    }

    public void detachOutput(Wire wire) {
        device.detachTerminal(OUTPUT, wire);
    }

    public void attachVdd(Wire wire) {
        device.attachTerminal(VDD, wire);
    }

    public void detachVdd(Wire wire) {
        device.detachTerminal(VDD, wire);
    }

    public void attachVss(Wire wire) {
        device.attachTerminal(VSS, wire);
    }

    public void detachVss(Wire wire) {
        device.detachTerminal(VSS, wire);
    }

    public void attachInput(Wire wire) {
        device.attachTerminal(INPUT, wire);
    }

    public void detachInput(Wire wire) {
        device.detachTerminal(INPUT, wire);
    }

    /**
     * Subscribes to the output voltage relative to VSS.
     *
     * @param listener the observation listener
     * @return the observation subscription
     */
    public ObservationSubscription observeOutputVoltage(
            ObservationListener listener
    ) {
        return device.observe(OUTPUT_VOLTAGE, listener);
    }

    /**
     * Subscribes to the input voltage relative to VSS.
     *
     * @param listener the observation listener
     * @return the observation subscription
     */
    public ObservationSubscription observeInputVoltage(
            ObservationListener listener
    ) {
        return device.observe(INPUT_VOLTAGE, listener);
    }

    /**
     * Subscribes to signed current from VDD to the output through the supply branch.
     * This observation excludes input pull-down currents.
     *
     * @param listener the observation listener
     * @return the observation subscription
     */
    public ObservationSubscription observeSupplyCurrent(
            ObservationListener listener
    ) {
        return device.observe(SUPPLY_CURRENT, listener);
    }
}
