package dev.hynergy.electrical.primitives.sources;

import dev.hynergy.electrical.*;

/**
 * Models a voltage-controlled voltage source.
 *
 * <p>The control voltage is the control-positive voltage minus the
 * control-negative voltage. The output voltage is the control voltage
 * multiplied by the voltage gain. Positive output current flows from
 * output-positive to output-negative.</p>
 */
public final class VoltageControlledVoltageSource {
    /**
     * The device type for {@code VoltageControlledVoltageSource}.
     */
    public static final DeviceType TYPE =
        PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_VOLTAGE_SOURCE;

    public static final DeviceTerminal OUTPUT_POSITIVE = TYPE.terminal(0);
    public static final DeviceTerminal OUTPUT_NEGATIVE = TYPE.terminal(1);
    public static final DeviceTerminal CONTROL_POSITIVE = TYPE.terminal(2);
    public static final DeviceTerminal CONTROL_NEGATIVE = TYPE.terminal(3);

    public static final DeviceParameter VOLTAGE_GAIN = TYPE.parameter(0);

    public static final DeviceObserver OUTPUT_VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver CONTROL_VOLTAGE = TYPE.observer(1);
    public static final DeviceObserver OUTPUT_CURRENT = TYPE.observer(2);

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public VoltageControlledVoltageSource(Device device) {
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
     * Creates a voltage-controlled voltage source.
     *
     * @param system the electrical system
     * @param voltageGain the voltage gain; the value must be finite
     *
     * @return the voltage-controlled voltage source
     */
    public static VoltageControlledVoltageSource create(ElectricalSystem system, double voltageGain) {
        VoltageControlledVoltageSource device = new VoltageControlledVoltageSource(system.create(TYPE));

        device.setVoltageGain(voltageGain);

        return device;
    }

    /**
     * Sets the voltage gain.
     *
     * @param voltageGain the voltage gain; the value must be finite
     */
    public void setVoltageGain(double voltageGain) {
        device.setParameter(VOLTAGE_GAIN, voltageGain);
    }

    /**
     * Attaches the output-positive terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachOutputPositive(Wire wire) {
        device.attachTerminal(OUTPUT_POSITIVE, wire);
    }

    /**
     * Detaches the output-positive terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachOutputPositive(Wire wire) {
        device.detachTerminal(OUTPUT_POSITIVE, wire);
    }

    /**
     * Attaches the output-negative terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachOutputNegative(Wire wire) {
        device.attachTerminal(OUTPUT_NEGATIVE, wire);
    }

    /**
     * Detaches the output-negative terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachOutputNegative(Wire wire) {
        device.detachTerminal(OUTPUT_NEGATIVE, wire);
    }

    /**
     * Attaches the control-positive terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachControlPositive(Wire wire) {
        device.attachTerminal(CONTROL_POSITIVE, wire);
    }

    /**
     * Detaches the control-positive terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachControlPositive(Wire wire) {
        device.detachTerminal(CONTROL_POSITIVE, wire);
    }

    /**
     * Attaches the control-negative terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachControlNegative(Wire wire) {
        device.attachTerminal(CONTROL_NEGATIVE, wire);
    }

    /**
     * Detaches the control-negative terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachControlNegative(Wire wire) {
        device.detachTerminal(CONTROL_NEGATIVE, wire);
    }

    /**
     * Subscribes to the output voltage.
     *
     * <p>The output voltage is the output-positive voltage minus the
     * output-negative voltage.</p>
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
     * Subscribes to the control voltage.
     *
     * <p>The control voltage is the control-positive voltage minus the
     * control-negative voltage.</p>
     *
     * @param listener the observation listener
     *
     * @return the observation subscription
     */
    public ObservationSubscription observeControlVoltage(
        ObservationListener listener
    ) {
        return device.observe(CONTROL_VOLTAGE, listener);
    }

    /**
     * Subscribes to the output current.
     *
     * <p>Positive output current flows from output-positive to
     * output-negative.</p>
     *
     * @param listener the observation listener
     *
     * @return the observation subscription
     */
    public ObservationSubscription observeOutputCurrent(
        ObservationListener listener
    ) {
        return device.observe(OUTPUT_CURRENT, listener);
    }
}
