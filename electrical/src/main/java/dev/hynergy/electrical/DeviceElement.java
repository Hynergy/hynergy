package dev.hynergy.electrical;

/**
 * Identifies one child instance in its enclosing declaration.
 */
public final class DeviceElement {
    final Object owner;
    final DeviceType type;
    DeviceElement(Object owner, DeviceType type) {
        this.owner = owner; this.type = type;
    }
}
