package dev.hynergy.electrical.primitives.logic;

import dev.hynergy.electrical.*;

/**
 * Models a two-input NOR gate with a finite-conductance output stage.
 *
 * <p>An input is high when its voltage relative to VSS is greater than or
 * equal to the threshold voltage. The output stage uses the maximum and
 * minimum conductances to connect the output towards VDD or VSS.</p>
 *
 * <p>The output and input observation voltages use VSS as their reference.</p>
 */
public final class Nor {
    /**
     * The device type for {@code Nor}.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.NOR;

    public static final DeviceTerminal OUTPUT = TYPE.terminal(0);
    public static final DeviceTerminal VDD = TYPE.terminal(1);
    public static final DeviceTerminal VSS = TYPE.terminal(2);
    public static final DeviceTerminal INPUT_A = TYPE.terminal(3);
    public static final DeviceTerminal INPUT_B = TYPE.terminal(4);

    public static final DeviceParameter THRESHOLD_RELATIVE_TO_VSS = TYPE.parameter(0);
    public static final DeviceParameter MAXIMUM_CONDUCTANCE = TYPE.parameter(1);
    public static final DeviceParameter MINIMUM_CONDUCTANCE = TYPE.parameter(2);

    public static final DeviceObserver OUTPUT_VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver INPUT_A_VOLTAGE = TYPE.observer(1);
    public static final DeviceObserver INPUT_B_VOLTAGE = TYPE.observer(2);
    public static final DeviceObserver SUPPLY_CURRENT = TYPE.observer(3);

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public Nor(Device device) {
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
     * Creates a NOR gate.
     *
     * @param system the electrical system
     * @param thresholdRelativeToVss the input threshold relative to VSS, in
     *     volts; the value must be finite
     * @param maximumConductance the active output-stage conductance, in
     *     siemens; the value must be finite and greater than zero
     * @param minimumConductance the inactive output-stage conductance, in
     *     siemens; the value must be finite and non-negative, and less than
     *     {@code maximumConductance}
     *
     * @return the gate
     */
    public static Nor create(
        ElectricalSystem system,
        double thresholdRelativeToVss,
        double maximumConductance,
        double minimumConductance
    ) {
        Nor device = new Nor(system.create(TYPE));

        device.setThresholdRelativeToVss(thresholdRelativeToVss);
        device.setMaximumConductance(maximumConductance);
        device.setMinimumConductance(minimumConductance);

        return device;
    }

    /**
     * Sets the input threshold relative to VSS.
     *
     * @param thresholdRelativeToVss the threshold voltage, in volts; the value
     *     must be finite
     */
    public void setThresholdRelativeToVss(double thresholdRelativeToVss) {
        device.setParameter(THRESHOLD_RELATIVE_TO_VSS, thresholdRelativeToVss);
    }

    /**
     * Sets the maximum output-stage conductance.
     *
     * @param maximumConductance the conductance, in siemens; the value must be
     *     finite, greater than zero, and greater than the current minimum
     *     conductance
     */
    public void setMaximumConductance(double maximumConductance) {
        device.setParameter(MAXIMUM_CONDUCTANCE, maximumConductance);
    }

    /**
     * Sets the minimum output-stage conductance.
     *
     * @param minimumConductance the conductance, in siemens; the value must be
     *     finite, non-negative, and less than the current maximum conductance
     */
    public void setMinimumConductance(double minimumConductance) {
        device.setParameter(MINIMUM_CONDUCTANCE, minimumConductance);
    }

    /**
     * Attaches the output terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachOutput(Wire wire) {
        device.attachTerminal(OUTPUT, wire);
    }

    /**
     * Detaches the output terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachOutput(Wire wire) {
        device.detachTerminal(OUTPUT, wire);
    }

    /**
     * Attaches the VDD terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachVdd(Wire wire) {
        device.attachTerminal(VDD, wire);
    }

    /**
     * Detaches the VDD terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachVdd(Wire wire) {
        device.detachTerminal(VDD, wire);
    }

    /**
     * Attaches the VSS terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachVss(Wire wire) {
        device.attachTerminal(VSS, wire);
    }

    /**
     * Detaches the VSS terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachVss(Wire wire) {
        device.detachTerminal(VSS, wire);
    }

    /**
     * Attaches input A to a wire.
     *
     * @param wire the wire
     */
    public void attachInputA(Wire wire) {
        device.attachTerminal(INPUT_A, wire);
    }

    /**
     * Detaches input A from a wire.
     *
     * @param wire the wire
     */
    public void detachInputA(Wire wire) {
        device.detachTerminal(INPUT_A, wire);
    }

    /**
     * Attaches input B to a wire.
     *
     * @param wire the wire
     */
    public void attachInputB(Wire wire) {
        device.attachTerminal(INPUT_B, wire);
    }

    /**
     * Detaches input B from a wire.
     *
     * @param wire the wire
     */
    public void detachInputB(Wire wire) {
        device.detachTerminal(INPUT_B, wire);
    }

    /**
     * Subscribes to the output voltage relative to VSS.
     *
     * @param listener the observation listener
     *
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
     *
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
     *
     * @return the observation subscription
     */
    public ObservationSubscription observeInputVoltageB(
        ObservationListener listener
    ) {
        return device.observe(INPUT_B_VOLTAGE, listener);
    }

    /**
     * Subscribes to the current from VDD to the output through the pull-up
     * branch.
     *
     * @param listener the observation listener
     *
     * @return the observation subscription
     */
    public ObservationSubscription observeSupplyCurrent(
        ObservationListener listener
    ) {
        return device.observe(SUPPLY_CURRENT, listener);
    }
}
