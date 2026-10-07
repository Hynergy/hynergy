package dev.hynergy.electrical.primitives.logic;

import dev.hynergy.electrical.*;

import java.util.Objects;

/**
 * Connects VDD to the output with the maximum conductance when the OR result is HIGH.
 * LOW uses the minimum supply conductance. A zero minimum disconnects the supply path.
 * Permanent pull-down branches connect each input and the output to VSS.
 * The supply path conducts in both directions. Voltage observations use VSS as their reference.
 */
public record SwitchedOr(Device device) {
    public static final DeviceType TYPE = PrimitiveDeviceTypes.SWITCHED_OR;

    public static final DeviceTerminal OUTPUT = TYPE.terminal(0);
    public static final DeviceTerminal VDD = TYPE.terminal(1);
    public static final DeviceTerminal VSS = TYPE.terminal(2);
    public static final DeviceTerminal INPUT_A = TYPE.terminal(3);
    public static final DeviceTerminal INPUT_B = TYPE.terminal(4);

    public static final DeviceParameter THRESHOLD_RELATIVE_TO_VSS = TYPE.parameter(0);
    public static final DeviceParameter MAXIMUM_CONDUCTANCE = TYPE.parameter(1);
    public static final DeviceParameter MINIMUM_CONDUCTANCE = TYPE.parameter(2);
    public static final DeviceParameter OUTPUT_BIAS_CONDUCTANCE = TYPE.parameter(3);
    public static final DeviceParameter INPUT_BIAS_CONDUCTANCE = TYPE.parameter(4);

    public static final DeviceObserver OUTPUT_VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver INPUT_A_VOLTAGE = TYPE.observer(1);
    public static final DeviceObserver INPUT_B_VOLTAGE = TYPE.observer(2);
    public static final DeviceObserver SUPPLY_CURRENT = TYPE.observer(3);

    /**
     * Uses a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the device has a different type
     * @throws IllegalStateException    if the device is no longer usable
     */
    public SwitchedOr {
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
     * Creates an OR gate.
     *
     * @param system                 the electrical system
     * @param thresholdRelativeToVss the input threshold relative to VSS, in
     *                               volts. The value must be finite.
     * @param maximumConductance     the active supply-path conductance, in
     *                               siemens. The value must be finite and greater than zero.
     * @param minimumConductance     the inactive supply-path conductance, in
     *                               siemens. The value must be finite and zero or greater.
     *                               The value must be less than {@code maximumConductance}.
     * @return the gate
     */
    public static SwitchedOr create(
            ElectricalSystem system,
            double thresholdRelativeToVss,
            double maximumConductance,
            double minimumConductance,
            double outputBiasConductance,
            double inputBiasConductance
    ) {
        return new SwitchedOr(system.create(TYPE, thresholdRelativeToVss,
                maximumConductance, minimumConductance, outputBiasConductance, inputBiasConductance));
    }

    /**
     * Sets the permanent output pull-down conductance.
     *
     * @param conductance the conductance in siemens. The value must be finite and greater than zero.
     */
    public void setOutputBiasConductance(double conductance) {
        device.setParameter(OUTPUT_BIAS_CONDUCTANCE, conductance);
    }

    /**
     * Sets each permanent input pull-down conductance.
     *
     * @param conductance the conductance in siemens. The value must be finite and greater than zero.
     */
    public void setInputBiasConductance(double conductance) {
        device.setParameter(INPUT_BIAS_CONDUCTANCE, conductance);
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
     * Sets the maximum supply-path conductance.
     *
     * @param maximumConductance the conductance, in siemens. The value must be
     *                           finite and greater than the current minimum conductance.
     *                           The value must also be greater than zero.
     */
    public void setMaximumConductance(double maximumConductance) {
        device.setParameter(MAXIMUM_CONDUCTANCE, maximumConductance);
    }

    /**
     * Sets the minimum supply-path conductance.
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

    public void attachInputA(Wire wire) {
        device.attachTerminal(INPUT_A, wire);
    }

    public void detachInputA(Wire wire) {
        device.detachTerminal(INPUT_A, wire);
    }

    public void attachInputB(Wire wire) {
        device.attachTerminal(INPUT_B, wire);
    }

    public void detachInputB(Wire wire) {
        device.detachTerminal(INPUT_B, wire);
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
     * Subscribes to input A voltage relative to VSS.
     *
     * @param listener the observation listener
     * @return the observation subscription
     */
    public ObservationSubscription observeInputVoltageA(
            ObservationListener listener
    ) {
        return device.observe(INPUT_A_VOLTAGE, listener);
    }

    /**
     * Subscribes to input B voltage relative to VSS.
     *
     * @param listener the observation listener
     * @return the observation subscription
     */
    public ObservationSubscription observeInputVoltageB(
            ObservationListener listener
    ) {
        return device.observe(INPUT_B_VOLTAGE, listener);
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
