package dev.hynergy.electrical;

/** A canonical terminal reference in one declaration. */
public final class DeviceTerminal implements NodeReference {
    final Object owner;
    private final int id;
    private final String name;

    DeviceTerminal(Object owner, int id, String name) {
        this.owner = owner; this.id = id; this.name = name;
    }
    public int id() { return id; }
    public String name() { return name; }
}
