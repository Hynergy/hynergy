package dev.hynergy.electrical.primitives.sources;

import dev.hynergy.electrical.*;

import java.util.Objects;

/**
 * Models a voltage-controlled voltage source.
 *
 * <p>The control voltage is the control-positive voltage minus the
 * control-negative voltage. The output voltage is the control voltage
 * multiplied by the voltage gain. Positive output current flows from
 * output-positive to output-negative.</p>
 */
public record VoltageControlledVoltageSource(Device device) {
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

    /**
     * Uses a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the device has a different type
     * @throws IllegalStateException    if the device is no longer usable
     */
    public VoltageControlledVoltageSource {
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
     * Creates a voltage-controlled voltage source.
     *
     * @param system      the electrical system
     * @param voltageGain the voltage gain. The value must be finite.
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
     * @param voltageGain the voltage gain. The value must be finite.
     */
    public void setVoltageGain(double voltageGain) {
        device.setParameter(VOLTAGE_GAIN, voltageGain);
    }

    public void attachOutputPositive(Wire wire) {
        device.attachTerminal(OUTPUT_POSITIVE, wire);
    }

    public void detachOutputPositive(Wire wire) {
        device.detachTerminal(OUTPUT_POSITIVE, wire);
    }

    public void attachOutputNegative(Wire wire) {
        device.attachTerminal(OUTPUT_NEGATIVE, wire);
    }

    public void detachOutputNegative(Wire wire) {
        device.detachTerminal(OUTPUT_NEGATIVE, wire);
    }

    public void attachControlPositive(Wire wire) {
        device.attachTerminal(CONTROL_POSITIVE, wire);
    }

    public void detachControlPositive(Wire wire) {
        device.detachTerminal(CONTROL_POSITIVE, wire);
    }

    public void attachControlNegative(Wire wire) {
        device.attachTerminal(CONTROL_NEGATIVE, wire);
    }

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
     * @return the observation subscription
     */
    public ObservationSubscription observeOutputCurrent(
            ObservationListener listener
    ) {
        return device.observe(OUTPUT_CURRENT, listener);
    }
}
