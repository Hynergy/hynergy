package dev.hynergy.electrical.composite;

import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.PrimitiveDeviceTypes;

/**
 * Defines switched logic gates with an internal voltage supply and a ground reference.
 *
 * <p>HIGH uses the maximum conductance between the supply and the output.
 * LOW uses the minimum conductance. A zero minimum disconnects the supply path.
 * Permanent pull-down branches connect each input and the output to ground.
 * The supply path conducts in both directions.</p>
 *
 * <p>Terminal IDs are output 0 and input 2 for NOT.
 * Binary gates use input A 2 and input B 3. Terminal ID 1 is not exposed.</p>
 *
 * <p>Parameter IDs are threshold 0, maximum conductance 1, minimum conductance 2,
 * supply voltage 3, output pull-down conductance 4, and input pull-down conductance 5.
 * The maximum must be greater than the minimum. Both pull-down conductances must be positive and finite.
 * The declaration does not set default parameter values.</p>
 *
 * <p>Observer IDs are output voltage 0 and input voltage 1 for NOT.
 * NOT uses supply current 2.
 * Binary gates use input A voltage 1, input B voltage 2, and supply current 3.
 * Voltage observations use ground as their reference.
 * Supply current is positive from the supply to the output and excludes input pull-down currents.</p>
 */
public final class GroundedSwitchedLogicGates {
    /**
     * Defines a switched NOT gate with an internal supply and ground.
     */
    public static final DeviceType GROUNDED_SWITCHED_NOT = define(PrimitiveDeviceTypes.SWITCHED_NOT, false);
    /**
     * Defines a switched AND gate with an internal supply and ground.
     */
    public static final DeviceType GROUNDED_SWITCHED_AND = define(PrimitiveDeviceTypes.SWITCHED_AND, true);
    /**
     * Defines a switched NAND gate with an internal supply and ground.
     */
    public static final DeviceType GROUNDED_SWITCHED_NAND = define(PrimitiveDeviceTypes.SWITCHED_NAND, true);
    /**
     * Defines a switched OR gate with an internal supply and ground.
     */
    public static final DeviceType GROUNDED_SWITCHED_OR = define(PrimitiveDeviceTypes.SWITCHED_OR, true);
    /**
     * Defines a switched NOR gate with an internal supply and ground.
     */
    public static final DeviceType GROUNDED_SWITCHED_NOR = define(PrimitiveDeviceTypes.SWITCHED_NOR, true);

    private GroundedSwitchedLogicGates() {
    }

    private static DeviceType define(DeviceType primitive, boolean binary) {
        return DeviceType.define(b -> {
            var output = b.terminal(0, "output");
            var inputA = b.terminal(2, binary ? "input_a" : "input");
            var inputB = binary ? b.terminal(3, "input_b") : null;
            var supply = b.node();
            var ground = b.ground();
            var threshold = b.parameter(0, "threshold", primitive.parameter(0).constraints());
            var on = b.parameter(1, "maximum_conductance", primitive.parameter(1).constraints());
            var off = b.parameter(2, "minimum_conductance", primitive.parameter(2).constraints());
            var source = PrimitiveDeviceTypes.VOLTAGE_SOURCE;
            var voltage = b.parameter(3, "supply_voltage", source.parameter(0).constraints());
            var outputBias = b.parameter(4, "output_bias_conductance", primitive.parameter(3).constraints());
            var inputBias = b.parameter(5, "input_bias_conductance", primitive.parameter(4).constraints());
            b.element(source, e -> {
                e.connect(source.terminal(0), supply);
                e.connect(source.terminal(1), ground);
                e.bind(source.parameter(0), voltage);
            });
            var gate = b.element(primitive, e -> {
                e.connect(primitive.terminal(0), output);
                e.connect(primitive.terminal(1), supply);
                e.connect(primitive.terminal(2), ground);
                e.connect(primitive.terminal(3), inputA);
                if (binary) e.connect(primitive.terminal(4), inputB);
                e.bind(primitive.parameter(0), threshold);
                e.bind(primitive.parameter(1), on);
                e.bind(primitive.parameter(2), off);
                e.bind(primitive.parameter(3), outputBias);
                e.bind(primitive.parameter(4), inputBias);
            });
            b.childObserver(0, "output_voltage", gate, primitive.observer(0));
            b.childObserver(1, binary ? "input_a_voltage" : "input_voltage", gate, primitive.observer(1));
            if (binary) b.childObserver(2, "input_b_voltage", gate, primitive.observer(2));
            b.childObserver(binary ? 3 : 2, "supply_current", gate, primitive.observer(binary ? 3 : 2));
        });
    }
}
