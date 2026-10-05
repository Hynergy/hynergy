package dev.hynergy.electrical;

/**
 * Native primitive definitions, independent of Java convenience wrappers.
 *
 * <p>Use these types for generic runtime creation and descriptor registration.
 * The definition IDs are part of the native protocol.</p>
 */
public final class PrimitiveDeviceTypes {
    public static final DeviceType RESISTANCE = DeviceType.primitive(1);
    public static final DeviceType CONDUCTANCE = DeviceType.primitive(2);
    public static final DeviceType VOLTAGE_SOURCE = DeviceType.primitive(3);
    public static final DeviceType CURRENT_SOURCE = DeviceType.primitive(4);
    public static final DeviceType VOLTAGE_CONTROLLED_CURRENT_SOURCE = DeviceType.primitive(5);
    public static final DeviceType VOLTAGE_CONTROLLED_VOLTAGE_SOURCE = DeviceType.primitive(6);
    public static final DeviceType CAPACITOR = DeviceType.primitive(7);
    public static final DeviceType INDUCTOR = DeviceType.primitive(8);
    public static final DeviceType VOLTAGE_CONTROLLED_SWITCH = DeviceType.primitive(9);
    public static final DeviceType VOLTAGE_CONTROLLED_CONDUCTANCE = DeviceType.primitive(10);
    public static final DeviceType TICK_DELAY = DeviceType.primitive(11);
    public static final DeviceType DIODE = DeviceType.primitive(12);
    public static final DeviceType NOT = DeviceType.primitive(13);
    public static final DeviceType AND = DeviceType.primitive(14);
    public static final DeviceType NAND = DeviceType.primitive(15);
    public static final DeviceType OR = DeviceType.primitive(16);
    public static final DeviceType NOR = DeviceType.primitive(17);
    public static final DeviceType SCHMITT_BUFFER = DeviceType.primitive(18);

    private PrimitiveDeviceTypes() {
    }
}
