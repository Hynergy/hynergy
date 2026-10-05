package dev.hynergy.electrical;

import java.util.List;

/** Canonical primitive schemas. Native identities remain an internal protocol detail. */
public final class PrimitiveDeviceTypes {
    private PrimitiveDeviceTypes() { }
    private static final ParameterConstraints FINITE = ParameterConstraints.unconstrained();
    private static final ParameterConstraints POSITIVE = ParameterConstraints.positiveFinite();
    private static final ParameterConstraints NONNEGATIVE = new ParameterConstraints(
        ParameterConstraints.Bound.inclusive(0), null, false, false, null, null);
    public static final DeviceType RESISTANCE = DeviceType.define(1, b -> {
        var t0 = b.terminal(0, "positive");
        var t1 = b.terminal(1, "negative");
        b.parameter(0, "resistance", POSITIVE);
        b.voltageObserver(0, "voltage", t0, t1);
        b.voltageObserver(1, "current", t0, t1);
    });
    public static final DeviceType CONDUCTANCE = DeviceType.define(2, b -> {
        var t0 = b.terminal(0, "positive");
        var t1 = b.terminal(1, "negative");
        b.parameter(0, "conductance", NONNEGATIVE);
        b.voltageObserver(0, "voltage", t0, t1);
        b.voltageObserver(1, "current", t0, t1);
    });
    public static final DeviceType VOLTAGE_SOURCE = DeviceType.define(3, b -> {
        var t0 = b.terminal(0, "positive");
        var t1 = b.terminal(1, "negative");
        b.parameter(0, "voltage", FINITE);
        b.voltageObserver(0, "voltage", t0, t1);
        b.voltageObserver(1, "current", t0, t1);
    });
    public static final DeviceType CURRENT_SOURCE = DeviceType.define(4, b -> {
        var t0 = b.terminal(0, "positive");
        var t1 = b.terminal(1, "negative");
        b.parameter(0, "current", FINITE);
        b.voltageObserver(0, "voltage", t0, t1);
        b.voltageObserver(1, "current", t0, t1);
    });
    public static final DeviceType VOLTAGE_CONTROLLED_CURRENT_SOURCE = DeviceType.define(5, b -> {
        var t0 = b.terminal(0, "output_positive");
        var t1 = b.terminal(1, "output_negative");
        var t2 = b.terminal(2, "control_positive");
        var t3 = b.terminal(3, "control_negative");
        b.parameter(0, "transconductance", FINITE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "control_voltage", t0, t1);
        b.voltageObserver(2, "output_current", t0, t1);
    });
    public static final DeviceType VOLTAGE_CONTROLLED_VOLTAGE_SOURCE = DeviceType.define(6, b -> {
        var t0 = b.terminal(0, "output_positive");
        var t1 = b.terminal(1, "output_negative");
        var t2 = b.terminal(2, "control_positive");
        var t3 = b.terminal(3, "control_negative");
        b.parameter(0, "voltage_gain", FINITE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "control_voltage", t0, t1);
        b.voltageObserver(2, "output_current", t0, t1);
    });
    public static final DeviceType CAPACITOR = DeviceType.define(7, b -> {
        var t0 = b.terminal(0, "positive");
        var t1 = b.terminal(1, "negative");
        b.parameter(0, "capacitance", POSITIVE);
        b.voltageObserver(0, "voltage", t0, t1);
        b.voltageObserver(1, "current", t0, t1);
    });
    public static final DeviceType INDUCTOR = DeviceType.define(8, b -> {
        var t0 = b.terminal(0, "positive");
        var t1 = b.terminal(1, "negative");
        b.parameter(0, "inductance", POSITIVE);
        b.voltageObserver(0, "voltage", t0, t1);
        b.voltageObserver(1, "current", t0, t1);
    });
    public static final DeviceType VOLTAGE_CONTROLLED_SWITCH = DeviceType.define(9, b -> {
        var t0 = b.terminal(0, "output_positive");
        var t1 = b.terminal(1, "output_negative");
        var t2 = b.terminal(2, "control_positive");
        var t3 = b.terminal(3, "control_negative");
        b.parameter(0, "threshold_voltage", FINITE);
        b.parameter(1, "maximum_conductance", POSITIVE);
        b.parameter(2, "minimum_conductance", NONNEGATIVE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "control_voltage", t0, t1);
        b.voltageObserver(2, "output_current", t0, t1);
    });
    public static final DeviceType VOLTAGE_CONTROLLED_CONDUCTANCE = DeviceType.define(10, b -> {
        var t0 = b.terminal(0, "output_positive");
        var t1 = b.terminal(1, "output_negative");
        var t2 = b.terminal(2, "control");
        b.parameter(0, "threshold_voltage", FINITE);
        b.parameter(1, "transition_voltage", POSITIVE);
        b.parameter(2, "minimum_conductance", NONNEGATIVE);
        b.parameter(3, "maximum_conductance", POSITIVE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "control_voltage", t0, t1);
        b.voltageObserver(2, "output_current", t0, t1);
    });
    public static final DeviceType TICK_DELAY = DeviceType.define(11, b -> {
        var t0 = b.terminal(0, "input_positive");
        var t1 = b.terminal(1, "input_negative");
        var t2 = b.terminal(2, "output_positive");
        var t3 = b.terminal(3, "output_negative");
        b.voltageObserver(0, "input_voltage", t0, t1);
        b.voltageObserver(1, "output_voltage", t0, t1);
        b.voltageObserver(2, "output_current", t0, t1);
    });
    public static final DeviceType DIODE = DeviceType.define(12, b -> {
        var t0 = b.terminal(0, "anode");
        var t1 = b.terminal(1, "cathode");
        b.parameter(0, "maximum_conductance", POSITIVE);
        b.parameter(1, "minimum_conductance", NONNEGATIVE);
        b.voltageObserver(0, "voltage", t0, t1);
        b.voltageObserver(1, "current", t0, t1);
    });
    public static final DeviceType NOT = DeviceType.define(13, b -> {
        var t0 = b.terminal(0, "output");
        var t1 = b.terminal(1, "vdd");
        var t2 = b.terminal(2, "vss");
        var t3 = b.terminal(3, "input");
        b.parameter(0, "threshold_relative_to_vss", FINITE);
        b.parameter(1, "maximum_conductance", POSITIVE);
        b.parameter(2, "minimum_conductance", NONNEGATIVE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "input_voltage", t0, t1);
        b.voltageObserver(2, "supply_current", t0, t1);
    });
    public static final DeviceType AND = DeviceType.define(14, b -> {
        var t0 = b.terminal(0, "output");
        var t1 = b.terminal(1, "vdd");
        var t2 = b.terminal(2, "vss");
        var t3 = b.terminal(3, "input_a");
        var t4 = b.terminal(4, "input_b");
        b.parameter(0, "threshold_relative_to_vss", FINITE);
        b.parameter(1, "maximum_conductance", POSITIVE);
        b.parameter(2, "minimum_conductance", NONNEGATIVE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "input_a_voltage", t0, t1);
        b.voltageObserver(2, "input_b_voltage", t0, t1);
        b.voltageObserver(3, "supply_current", t0, t1);
    });
    public static final DeviceType NAND = DeviceType.define(15, b -> {
        var t0 = b.terminal(0, "output");
        var t1 = b.terminal(1, "vdd");
        var t2 = b.terminal(2, "vss");
        var t3 = b.terminal(3, "input_a");
        var t4 = b.terminal(4, "input_b");
        b.parameter(0, "threshold_relative_to_vss", FINITE);
        b.parameter(1, "maximum_conductance", POSITIVE);
        b.parameter(2, "minimum_conductance", NONNEGATIVE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "input_a_voltage", t0, t1);
        b.voltageObserver(2, "input_b_voltage", t0, t1);
        b.voltageObserver(3, "supply_current", t0, t1);
    });
    public static final DeviceType OR = DeviceType.define(16, b -> {
        var t0 = b.terminal(0, "output");
        var t1 = b.terminal(1, "vdd");
        var t2 = b.terminal(2, "vss");
        var t3 = b.terminal(3, "input_a");
        var t4 = b.terminal(4, "input_b");
        b.parameter(0, "threshold_relative_to_vss", FINITE);
        b.parameter(1, "maximum_conductance", POSITIVE);
        b.parameter(2, "minimum_conductance", NONNEGATIVE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "input_a_voltage", t0, t1);
        b.voltageObserver(2, "input_b_voltage", t0, t1);
        b.voltageObserver(3, "supply_current", t0, t1);
    });
    public static final DeviceType NOR = DeviceType.define(17, b -> {
        var t0 = b.terminal(0, "output");
        var t1 = b.terminal(1, "vdd");
        var t2 = b.terminal(2, "vss");
        var t3 = b.terminal(3, "input_a");
        var t4 = b.terminal(4, "input_b");
        b.parameter(0, "threshold_relative_to_vss", FINITE);
        b.parameter(1, "maximum_conductance", POSITIVE);
        b.parameter(2, "minimum_conductance", NONNEGATIVE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "input_a_voltage", t0, t1);
        b.voltageObserver(2, "input_b_voltage", t0, t1);
        b.voltageObserver(3, "supply_current", t0, t1);
    });
    public static final DeviceType SCHMITT_BUFFER = DeviceType.define(18, b -> {
        var t0 = b.terminal(0, "output");
        var t1 = b.terminal(1, "vdd");
        var t2 = b.terminal(2, "vss");
        var t3 = b.terminal(3, "input");
        b.parameter(0, "threshold_relative_to_vss", FINITE);
        b.parameter(1, "hysteresis_width", NONNEGATIVE);
        b.parameter(2, "maximum_conductance", POSITIVE);
        b.parameter(3, "minimum_conductance", NONNEGATIVE);
        b.voltageObserver(0, "output_voltage", t0, t1);
        b.voltageObserver(1, "input_voltage", t0, t1);
        b.voltageObserver(2, "supply_current", t0, t1);
    });
    static final List<DeviceType> ALL = List.of(RESISTANCE, CONDUCTANCE, VOLTAGE_SOURCE, CURRENT_SOURCE, VOLTAGE_CONTROLLED_CURRENT_SOURCE, VOLTAGE_CONTROLLED_VOLTAGE_SOURCE, CAPACITOR, INDUCTOR, VOLTAGE_CONTROLLED_SWITCH, VOLTAGE_CONTROLLED_CONDUCTANCE, TICK_DELAY, DIODE, NOT, AND, NAND, OR, NOR, SCHMITT_BUFFER);
}
