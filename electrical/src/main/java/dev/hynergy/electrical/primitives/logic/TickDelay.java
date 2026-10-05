package dev.hynergy.electrical.primitives.logic;

import dev.hynergy.electrical.*;

/**
 * Delays an input voltage by one simulation tick.
 *
 * <p>The input voltage is the input-positive voltage minus the input-negative
 * voltage. The output applies the input voltage that was captured on the
 * previous tick. The initial delayed voltage is zero volts.</p>
 *
 * <p>Positive output current flows from output-positive to output-negative.</p>
 */
public final class TickDelay {
    /**
     * The device type for {@code TickDelay}.
     */
    public static final DeviceType TYPE = PrimitiveDeviceTypes.TICK_DELAY;

    private static final int TERMINAL_INPUT_POSITIVE = 0;
    private static final int TERMINAL_INPUT_NEGATIVE = 1;
    private static final int TERMINAL_OUTPUT_POSITIVE = 2;
    private static final int TERMINAL_OUTPUT_NEGATIVE = 3;

    private static final int OBSERVER_INPUT_VOLTAGE = 0;
    private static final int OBSERVER_OUTPUT_VOLTAGE = 1;
    private static final int OBSERVER_OUTPUT_CURRENT = 2;

    private final Device device;

    /**
     * Wraps a live device with this primitive definition.
     *
     * @throws IllegalArgumentException if the native definition differs
     * @throws IllegalStateException if the device is no longer usable
     */
    public TickDelay(Device device) {
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
     * Creates a one-tick voltage delay.
     *
     * @param system the electrical system
     *
     * @return the tick delay
     */
    public static TickDelay create(ElectricalSystem system) {
        return new TickDelay(system.create(TYPE));
    }

    /**
     * Attaches the input-positive terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachInputPositive(Wire wire) {
        device.attachTerminal(TERMINAL_INPUT_POSITIVE, wire);
    }

    /**
     * Detaches the input-positive terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachInputPositive(Wire wire) {
        device.detachTerminal(TERMINAL_INPUT_POSITIVE, wire);
    }

    /**
     * Attaches the input-negative terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachInputNegative(Wire wire) {
        device.attachTerminal(TERMINAL_INPUT_NEGATIVE, wire);
    }

    /**
     * Detaches the input-negative terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachInputNegative(Wire wire) {
        device.detachTerminal(TERMINAL_INPUT_NEGATIVE, wire);
    }

    /**
     * Attaches the output-positive terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachOutputPositive(Wire wire) {
        device.attachTerminal(TERMINAL_OUTPUT_POSITIVE, wire);
    }

    /**
     * Detaches the output-positive terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachOutputPositive(Wire wire) {
        device.detachTerminal(TERMINAL_OUTPUT_POSITIVE, wire);
    }

    /**
     * Attaches the output-negative terminal to a wire.
     *
     * @param wire the wire
     */
    public void attachOutputNegative(Wire wire) {
        device.attachTerminal(TERMINAL_OUTPUT_NEGATIVE, wire);
    }

    /**
     * Detaches the output-negative terminal from a wire.
     *
     * @param wire the wire
     */
    public void detachOutputNegative(Wire wire) {
        device.detachTerminal(TERMINAL_OUTPUT_NEGATIVE, wire);
    }

    /**
     * Subscribes to the current input voltage.
     *
     * <p>The input voltage is the input-positive voltage minus the
     * input-negative voltage.</p>
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
     * Subscribes to the delayed output voltage.
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
        return device.observe(OBSERVER_OUTPUT_VOLTAGE, listener);
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
        return device.observe(OBSERVER_OUTPUT_CURRENT, listener);
    }
}
