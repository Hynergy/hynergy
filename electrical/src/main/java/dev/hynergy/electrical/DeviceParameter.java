package dev.hynergy.electrical;

/** A canonical parameter reference in one declaration. */
public final class DeviceParameter {
    final Object owner;
    private final int id;
    private final String name;
    private final ParameterConstraints constraints;

    DeviceParameter(Object owner, int id, String name, ParameterConstraints constraints) {
        this.owner = owner; this.id = id; this.name = name; this.constraints = constraints;
    }
    public int id() { return id; }
    public String name() { return name; }
    public ParameterConstraints constraints() { return constraints; }
}
