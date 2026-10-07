package dev.hynergy.electrical.primitives.controlled;

import dev.hynergy.electrical.*;

import java.util.Objects;

/**
 * Models a voltage-controlled conductance between two output terminals.
 *
 * <p>The control voltage is the control-terminal voltage minus the
 * output-negative-terminal voltage. The output conductance changes from the
 * minimum conductance to the maximum conductance across a transition interval
 * that is centered on the threshold voltage.</p>
 *
 * <p>Positive output current flows from output-positive to output-negative.</p>
 */
public record VoltageControlledConductance(Device device) {
    public static final DeviceType TYPE =
            PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_CONDUCTANCE;

    public static final DeviceTerminal OUTPUT_POSITIVE = TYPE.terminal(0);
    public static final DeviceTerminal OUTPUT_NEGATIVE = TYPE.terminal(1);
    public static final DeviceTerminal CONTROL = TYPE.terminal(2);

    public static final DeviceParameter THRESHOLD_VOLTAGE = TYPE.parameter(0);
    public static final DeviceParameter TRANSITION_VOLTAGE = TYPE.parameter(1);
    public static final DeviceParameter MINIMUM_CONDUCTANCE = TYPE.parameter(2);
    public static final DeviceParameter MAXIMUM_CONDUCTANCE = TYPE.parameter(3);

    public static final DeviceObserver OUTPUT_VOLTAGE = TYPE.observer(0);
    public static final DeviceObserver CONTROL_VOLTAGE = TYPE.observer(1);
    public static final DeviceObserver OUTPUT_CURRENT = TYPE.observer(2);

    /**
     * Uses a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the device has a different type
     * @throws IllegalStateException    if the device is no longer usable
     */
    public VoltageControlledConductance {
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
     * Creates a voltage-controlled conductance.
     *
     * @param system             the electrical system
     * @param thresholdVoltage   the center of the transition interval, in volts.
     *                           The value must be finite.
     * @param transitionVoltage  the width of the transition interval, in volts.
     *                           The value must be finite and greater than zero.
     * @param minimumConductance the minimum conductance, in siemens. The value
     *                           must be finite and zero or greater.
     * @param maximumConductance the maximum conductance, in siemens. The value
     *                           must be finite and greater than zero.
     *                           The value must be greater than {@code minimumConductance}.
     * @return the voltage-controlled conductance
     */
    public static VoltageControlledConductance create(
            ElectricalSystem system,
            double thresholdVoltage,
            double transitionVoltage,
            double minimumConductance,
            double maximumConductance
    ) {
        VoltageControlledConductance device = new VoltageControlledConductance(system.create(TYPE));

        device.setThresholdVoltage(thresholdVoltage);
        device.setTransitionVoltage(transitionVoltage);
        device.setMinimumConductance(minimumConductance);
        device.setMaximumConductance(maximumConductance);

        return device;
    }

    /**
     * Sets the center of the transition interval.
     *
     * @param thresholdVoltage the threshold voltage, in volts. The value must
     *                         be finite.
     */
    public void setThresholdVoltage(double thresholdVoltage) {
        device.setParameter(THRESHOLD_VOLTAGE, thresholdVoltage);
    }

    /**
     * Sets the width of the transition interval.
     *
     * @param transitionVoltage the transition voltage, in volts. The value
     *                          must be finite and greater than zero.
     */
    public void setTransitionVoltage(double transitionVoltage) {
        device.setParameter(TRANSITION_VOLTAGE, transitionVoltage);
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

    public void attachControl(Wire wire) {
        device.attachTerminal(CONTROL, wire);
    }

    public void detachControl(Wire wire) {
        device.detachTerminal(CONTROL, wire);
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
     * <p>The control voltage is the control-terminal voltage minus the
     * output-negative-terminal voltage.</p>
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
