package dev.hynergy.electrical;

/**
 * Identifies an observer in one electrical declaration.
 */
public final class DeviceObserver {
    final Object owner;
    private final int id;
    private final String name;

    DeviceObserver(Object owner, int id, String name) {
        this.owner = owner; this.id = id; this.name = name;
    }
    public int id() { return id; }
    public String name() { return name; }
}
