package dev.hynergy.electrical.primitives.logic;

import dev.hynergy.electrical.*;

/**
 * Models a NOT gate with a finite-conductance output stage.
 *
 * <p>The input is high when its voltage relative to VSS is greater than or
 * equal to the threshold voltage. The output stage uses the maximum and
 * minimum conductances to connect the output towards VDD or VSS.</p>
 *
 * <p>The output and input observation voltages use VSS as their reference.</p>
 */
public final class Not {
    /**
     * The device type for {@code Not}.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.NOT;

    private static final int TERMINAL_OUTPUT = 0;
    private static final int TERMINAL_VDD = 1;
    private static final int TERMINAL_VSS = 2;
    private static final int TERMINAL_INPUT = 3;

    private static final int PARAMETER_THRESHOLD_RELATIVE_TO_VSS = 0;
    private static final int PARAMETER_MAXIMUM_CONDUCTANCE = 1;
    private static final int PARAMETER_MINIMUM_CONDUCTANCE = 2;

    private static final int OBSERVER_OUTPUT_VOLTAGE = 0;
    private static final int OBSERVER_INPUT_VOLTAGE = 1;
    private static final int OBSERVER_SUPPLY_CURRENT = 2;

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public Not(Device device) {
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
     * Creates a NOT gate.
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
     * @param thresholdRelativeToVss the threshold voltage, in volts; the value
     *     must be finite
     */
    public void setThresholdRelativeToVss(double thresholdRelativeToVss) {
        device.setParameter(PARAMETER_THRESHOLD_RELATIVE_TO_VSS, thresholdRelativeToVss);
    }

    /**
     * Sets the maximum output-stage conductance.
     *
     * @param maximumConductance the conductance, in siemens; the value must be
     *     finite, greater than zero, and greater than the current minimum
     *     conductance
     */
    public void setMaximumConductance(double maximumConductance) {
        device.setParameter(PARAMETER_MAXIMUM_CONDUCTANCE, maximumConductance);
    }

    /**
     * Sets the minimum output-stage conductance.
     *
     * @param minimumConductance the conductance, in siemens; the value must be
     *     finite, non-negative, and less than the current maximum conductance
     */
    public void setMinimumConductance(double minimumConductance) {
        device.setParameter(PARAMETER_MINIMUM_CONDUCTANCE, minimumConductance);
    }

    /**
     * Attaches the output terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachOutput(Wire wire) {
        device.attachTerminal(TERMINAL_OUTPUT, wire);
    }

    /**
     * Detaches the output terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachOutput(Wire wire) {
        device.detachTerminal(TERMINAL_OUTPUT, wire);
    }

    /**
     * Attaches the VDD terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachVdd(Wire wire) {
        device.attachTerminal(TERMINAL_VDD, wire);
    }

    /**
     * Detaches the VDD terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachVdd(Wire wire) {
        device.detachTerminal(TERMINAL_VDD, wire);
    }

    /**
     * Attaches the VSS terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachVss(Wire wire) {
        device.attachTerminal(TERMINAL_VSS, wire);
    }

    /**
     * Detaches the VSS terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachVss(Wire wire) {
        device.detachTerminal(TERMINAL_VSS, wire);
    }

    /**
     * Attaches the input terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachInput(Wire wire) {
        device.attachTerminal(TERMINAL_INPUT, wire);
    }

    /**
     * Detaches the input terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachInput(Wire wire) {
        device.detachTerminal(TERMINAL_INPUT, wire);
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
        return device.observe(OBSERVER_OUTPUT_VOLTAGE, listener);
    }

    /**
     * Subscribes to the input voltage relative to VSS.
     *
     * @param listener the observation listener
     *
     * @return the observation subscription
     */
    public ObservationSubscription observeInputVoltage(
        ObservationListener listener
    ) {
        return device.observe(OBSERVER_INPUT_VOLTAGE, listener);
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
        return device.observe(OBSERVER_SUPPLY_CURRENT, listener);
    }
}
