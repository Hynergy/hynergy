package dev.hynergy.electrical.primitives.logic;

import dev.hynergy.electrical.*;

/**
 * Models a non-inverting Schmitt buffer with a finite-conductance output stage.
 *
 * <p>The input voltage uses VSS as its reference. The lower switching voltage
 * is the threshold voltage minus half the hysteresis width. The upper switching
 * voltage is the threshold voltage plus half the hysteresis width.</p>
 *
 * <p>The output and input observation voltages use VSS as their reference.</p>
 */
public final class SchmittBuffer {
    /**
     * The device type for {@code SchmittBuffer}.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.SCHMITT_BUFFER;

    public static final DeviceTerminal OUTPUT = TYPE.terminal(0);
    public static final DeviceTerminal VDD = TYPE.terminal(1);
    public static final DeviceTerminal VSS = TYPE.terminal(2);
    public static final DeviceTerminal INPUT = TYPE.terminal(3);

    public static final DeviceParameter THRESHOLD_RELATIVE_TO_VSS = TYPE.parameter(0);
    public static final DeviceParameter HYSTERESIS_WIDTH = TYPE.parameter(1);
    public static final DeviceParameter MAXIMUM_CONDUCTANCE = TYPE.parameter(2);
    public static final DeviceParameter MINIMUM_CONDUCTANCE = TYPE.parameter(3);

    public static final DeviceObserver OUTPUT_VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver INPUT_VOLTAGE = TYPE.observer(1);
    public static final DeviceObserver SUPPLY_CURRENT = TYPE.observer(2);

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public SchmittBuffer(Device device) {
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
     * Creates a Schmitt buffer.
     *
     * @param system the electrical system
     * @param thresholdRelativeToVss the center of the hysteresis interval
     *     relative to VSS, in volts; the value must be finite
     * @param hysteresisWidth the hysteresis width, in volts; the value must be
     *     finite and non-negative
     * @param maximumConductance the active output-stage conductance, in
     *     siemens; the value must be finite and greater than zero
     * @param minimumConductance the inactive output-stage conductance, in
     *     siemens; the value must be finite and non-negative, and less than
     *     {@code maximumConductance}
     *
     * @return the Schmitt buffer
     */
    public static SchmittBuffer create(
        ElectricalSystem system,
        double thresholdRelativeToVss,
        double hysteresisWidth,
        double maximumConductance,
        double minimumConductance
    ) {
        SchmittBuffer device = new SchmittBuffer(system.create(TYPE));

        device.setThresholdRelativeToVss(thresholdRelativeToVss);
        device.setHysteresisWidth(hysteresisWidth);
        device.setMaximumConductance(maximumConductance);
        device.setMinimumConductance(minimumConductance);

        return device;
    }

    /**
     * Sets the center of the hysteresis interval relative to VSS.
     *
     * @param thresholdRelativeToVss the threshold voltage, in volts; the value
     *     must be finite
     */
    public void setThresholdRelativeToVss(double thresholdRelativeToVss) {
        device.setParameter(THRESHOLD_RELATIVE_TO_VSS, thresholdRelativeToVss);
    }

    /**
     * Sets the hysteresis width.
     *
     * @param hysteresisWidth the hysteresis width, in volts; the value must be
     *     finite and non-negative
     */
    public void setHysteresisWidth(double hysteresisWidth) {
        device.setParameter(HYSTERESIS_WIDTH, hysteresisWidth);
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
     * Attaches the input terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachInput(Wire wire) {
        device.attachTerminal(INPUT, wire);
    }

    /**
     * Detaches the input terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachInput(Wire wire) {
        device.detachTerminal(INPUT, wire);
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
     * Subscribes to the input voltage relative to VSS.
     *
     * @param listener the observation listener
     *
     * @return the observation subscription
     */
    public ObservationSubscription observeInputVoltage(
        ObservationListener listener
    ) {
        return device.observe(INPUT_VOLTAGE, listener);
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
