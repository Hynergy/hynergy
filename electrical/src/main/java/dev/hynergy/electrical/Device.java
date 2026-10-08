package dev.hynergy.electrical;

import org.jspecify.annotations.Nullable;
import java.util.Objects;

/** Provides access to a live device. Typed members must belong to its exact declaration. */
@SuppressWarnings("resource")
public final class Device {
    private @Nullable ElectricalSystem system;
    private @Nullable DeviceId deviceId;
    private @Nullable RegisteredDeviceType binding;
    Device() { }
    void bind(ElectricalSystem system, DeviceId deviceId, DeviceDefinition definition, RegisteredDeviceType binding) {
        requireUnbound();
        this.system = Objects.requireNonNull(system, "system");
        this.deviceId = Objects.requireNonNull(deviceId, "deviceId");
        this.binding = Objects.requireNonNull(binding, "binding");
    }
    RegisteredDeviceType binding() { return Objects.requireNonNull(binding, "binding"); }
    DeviceDefinition definition() { return binding().definition(); }
    DeviceType.Metadata metadata() { return binding().type().metadata(); }
    boolean belongsTo(ElectricalSystem system) { return this.system == system; }
    public DeviceId id() {
        if (deviceId == null) throw new IllegalStateException("Electrical device is not bound");
        return deviceId;
    }
    public void requireDefinition(DeviceType type) { requireBound().requireDefinition(this, type); }
    /**
     * Checks and queues one parameter change for the next electrical tick.
     * Validation includes pending parameter values and native parameter relationships.
     * This method does not save instance overrides.
     * Use DeviceComponent to save parameter overrides for a block.
     *
     * @param parameter a parameter from this device's declaration
     * @param value the proposed value
     * @throws IllegalArgumentException if the reference or value is invalid
     * @throws IllegalStateException if the device or system is unusable
     */
    public void setParameter(DeviceParameter parameter, double value) {
        requireBound().setParameter(this, binding().parameterIndex(parameter), value);
    }
    public void validateParameter(DeviceParameter parameter, double value) {
        requireBound().validateParameter(this, binding().parameterIndex(parameter), value);
    }
    /**
     * Checks ordered parameter changes against pending values without queuing writes.
     * Native parameter writes occur in sequence.
     * Each intermediate parameter vector must be valid.
     * This method uses the map's iteration order.
     * Use a map with a defined order when changes depend on each other.
     * Successful validation does not apply the changes.
     * Queue the changes separately in the same order.
     *
     * @param updates the ordered parameter changes
     * @throws IllegalArgumentException if a reference or intermediate parameter vector is invalid
     * @throws IllegalStateException if the device or system is unusable
     */
    public void validateParameters(java.util.Map<DeviceParameter, Double> updates) {
        requireBound().validateParameters(this, updates);
    }

    public void attachTerminal(DeviceTerminal terminal, Wire wire) {
        requireBound().attachTerminal(this, binding().terminalIndex(terminal), wire);
    }
    public void detachTerminal(DeviceTerminal terminal, Wire wire) {
        requireBound().detachTerminal(this, binding().terminalIndex(terminal), wire);
    }
    public ObservationSubscription observe(DeviceObserver observer, ObservationListener listener) {
        return requireBound().subscribe(this, binding().observerIndex(observer), listener);
    }
    public void destroy() { requireBound().remove(this); }
    void requireUnbound() {
        if (system != null) throw new IllegalStateException("Electrical device is already bound");
    }
    private ElectricalSystem requireBound() {
        if (system == null) throw new IllegalStateException("Electrical device is not bound");
        return system;
    }
}
