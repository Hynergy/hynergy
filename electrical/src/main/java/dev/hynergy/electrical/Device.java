package dev.hynergy.electrical;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A runtime handle for an electrical device.
 *
 * <p>The electrical system owns the identity, native definition, and lifecycle.
 * Member IDs are native definition-order indexes, not persistent stable IDs.</p>
 *
 * <p>Gameplay integrations must set parameters through their persistent
 * component so that overrides and dirty marking are retained.</p>
 */
@SuppressWarnings("resource")
public final class Device {
    private @Nullable ElectricalSystem system;

    private @Nullable DeviceId deviceId;
    private @Nullable DeviceDefinition definition;

    /**
     * Creates an unbound device.
     *
     * <p>The electrical system binds the device after construction.</p>
     */
    Device() {
    }

    /**
     * Creates an observation subscription for this device.
     *
     * <p>The observer ID is the zero-based order in which the device
     * definition adds its observers.</p>
     *
     * @param observerId the observer ID
     * @param listener the observation listener
     *
     * @return the observation subscription
     *
     * @throws NullPointerException if {@code listener} is null
     * @throws IllegalStateException if the device is not bound or its system
     *     is not usable
     */
    public final ObservationSubscription observe(int observerId, ObservationListener listener) {
        return requireBound().subscribe(this, observerId, listener);
    }

    final void bind(ElectricalSystem system, DeviceId deviceId, DeviceDefinition definition) {
        requireUnbound();

        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(definition, "definition");

        this.system = system;
        this.deviceId = deviceId;
        this.definition = definition;
    }

    /**
     * Checks that this live handle uses the supplied native definition.
     *
     * @throws IllegalArgumentException if the definitions differ
     * @throws IllegalStateException if the handle is stale or the type is unregistered
     */
    public final void requireDefinition(DeviceType type) {
        requireBound().requireDefinition(this, type);
    }

    final DeviceDefinition definition() {
        requireBound();
        return Objects.requireNonNull(definition, "definition");
    }

    /** Checks native parameter constraints without queuing a mutation. */
    public final void validateParameter(int parameterId, double value) {
        requireBound().validateParameter(this, parameterId, value);
    }

    final boolean belongsTo(ElectricalSystem system) {
        return this.system == system;
    }

    /**
     * Returns the persistent identity of this device.
     *
     * @return the device ID
     * @throws IllegalStateException if the device is not bound
     */
    public final DeviceId id() {
        requireBound();
        return Objects.requireNonNull(deviceId, "Bound device identity is missing");
    }

    /**
     * Sets one parameter of this device.
     *
     * <p>The parameter ID is the zero-based order in which the device
     * definition adds its parameters.</p>
     *
     * <p>If this method is called from an observation callback, the change
     * applies to the next tick.</p>
     *
     * @param parameterId the parameter ID
     * @param value the parameter value
     * @throws IllegalArgumentException if the index or value violates the native definition
     *
     * @throws IllegalStateException if the device or its system is not usable
     */
    public final void setParameter(int parameterId, double value) {
        requireBound().setParameter(this, parameterId, value);
    }

    /**
     * Attaches one device terminal to a wire.
     *
     * <p>The terminal ID is the zero-based order in which the device
     * definition adds its terminals. The wire must belong to the same
     * electrical system as this device.</p>
     *
     * @param terminalId the terminal ID
     * @param wire the wire
     *
     * @throws IllegalArgumentException if the wire belongs to another system
     * @throws IllegalStateException if the device or its system is not usable
     */
    public final void attachTerminal(int terminalId, Wire wire) {
        requireBound().attachTerminal(this, terminalId, wire);
    }

    /**
     * Detaches one device terminal from a wire.
     *
     * <p>The wire must belong to the same electrical system as this device.</p>
     *
     * @param terminalId the terminal ID
     * @param wire the wire
     *
     * @throws IllegalArgumentException if the wire belongs to another system
     * @throws IllegalStateException if the device or its system is not usable
     */
    public final void detachTerminal(int terminalId, Wire wire) {
        requireBound().detachTerminal(this, terminalId, wire);
    }

    /**
     * Removes this device from its electrical system.
     *
     * <p>This operation makes all observation subscriptions for this device
     * inactive. Do not use this device after this method completes.</p>
     *
     * @throws IllegalStateException if the device or its system is not usable
     */
    public final void destroy() {
        requireBound().remove(this);
    }

    final void requireUnbound() {
        if (system != null) {
            throw new IllegalStateException("Electrical device is already bound");
        }
    }

    private ElectricalSystem requireBound() {
        ElectricalSystem system = this.system;

        if (system == null) {
            throw new IllegalStateException("Electrical device is not bound");
        }

        return system;
    }
}
