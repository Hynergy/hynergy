package dev.hynergy.electrical.primitives.sources;

import dev.hynergy.electrical.*;

import java.util.Objects;

/**
 * Models a voltage-controlled current source.
 *
 * <p>The control voltage is the control-positive voltage minus the
 * control-negative voltage. The output current is the control voltage
 * multiplied by the transconductance. Positive output current flows from
 * output-positive to output-negative.</p>
 */
public record VoltageControlledCurrentSource(Device device) {
    public static final DeviceType TYPE =
            PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_CURRENT_SOURCE;

    public static final DeviceTerminal OUTPUT_POSITIVE = TYPE.terminal(0);
    public static final DeviceTerminal OUTPUT_NEGATIVE = TYPE.terminal(1);
    public static final DeviceTerminal CONTROL_POSITIVE = TYPE.terminal(2);
    public static final DeviceTerminal CONTROL_NEGATIVE = TYPE.terminal(3);

    public static final DeviceParameter TRANSCONDUCTANCE = TYPE.parameter(0);

    public static final DeviceObserver OUTPUT_VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver CONTROL_VOLTAGE = TYPE.observer(1);
    public static final DeviceObserver OUTPUT_CURRENT = TYPE.observer(2);

    /**
     * Uses a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the device has a different type
     * @throws IllegalStateException    if the device is no longer usable
     */
    public VoltageControlledCurrentSource {
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
     * Creates a voltage-controlled current source.
     *
     * @param system           the electrical system
     * @param transconductance the transconductance, in siemens. The value must
     *                         be finite.
     * @return the voltage-controlled current source
     */
    public static VoltageControlledCurrentSource create(ElectricalSystem system, double transconductance) {
        VoltageControlledCurrentSource device = new VoltageControlledCurrentSource(system.create(TYPE));

        device.setTransconductance(transconductance);

        return device;
    }

    /**
     * Sets the transconductance.
     *
     * @param transconductance the transconductance, in siemens. The value must
     *                         be finite.
     */
    public void setTransconductance(double transconductance) {
        device.setParameter(TRANSCONDUCTANCE, transconductance);
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
