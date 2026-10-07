package dev.hynergy.electrical;

/**
 * Provides a runtime handle to a wire in an {@link ElectricalSystem}.
 *
 * <p>A wire connects device terminals to the same electrical network.
 * Each handle belongs to the electrical system that created or resolved it.
 * Do not use the handle with another system.</p>
 *
 * <p>The {@link WireId} identifies the wire independently of this Java handle.
 * Save the wire ID to restore the wire.
 * The handle is valid only during the lifetime of its electrical system.
 * After restoration, use the saved wire ID to get a new handle.</p>
 */
public final class Wire {
    private final ElectricalSystem system;
    private final WireId wireId;

    Wire(ElectricalSystem system, WireId wireId) {
        this.system = system;
        this.wireId = wireId;
    }

    public void connect(Wire other) {
        system.connect(this, other);
    }

    public void disconnect(Wire other) {
        system.disconnect(this, other);
    }

    public void destroy() {
        system.remove(this);
    }
    
    public WireId id() {
        return wireId;
    }


    boolean belongsTo(ElectricalSystem system) {
        return this.system == system;
    }
}
