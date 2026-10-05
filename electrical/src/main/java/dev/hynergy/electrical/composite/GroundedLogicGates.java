package dev.hynergy.electrical.composite;

import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class GroundedLogicGates {


    public static final DeviceType GROUNDED_OR = DeviceType.define(b -> {
        var primitive = PrimitiveDeviceTypes.OR;

        var output = b.terminal(0, "output");
        var supply = b.terminal(1, "supply");
        var inputA = b.terminal(2, "input_a");
        var inputB = b.terminal(3, "input_b");
        var ground = b.ground();

        var threshold = b.parameter(
                0, "threshold", primitive.parameter(0).constraints());
        var maximumConductance = b.parameter(
                1, "maximum_conductance", primitive.parameter(1).constraints());
        var minimumConductance = b.parameter(
                2, "minimum_conductance", primitive.parameter(2).constraints());

        var gate = b.element(primitive, e -> {
            e.connect(primitive.terminal(0), output);
            e.connect(primitive.terminal(1), supply);
            e.connect(primitive.terminal(2), ground);
            e.connect(primitive.terminal(3), inputA);
            e.connect(primitive.terminal(4), inputB);

            e.bind(primitive.parameter(0), threshold);
            e.bind(primitive.parameter(1), maximumConductance);
            e.bind(primitive.parameter(2), minimumConductance);
        });

        b.childObserver(0, "output_voltage", gate, primitive.observer(0));
        b.childObserver(1, "input_a_voltage", gate, primitive.observer(1));
        b.childObserver(2, "input_b_voltage", gate, primitive.observer(2));
        b.childObserver(3, "supply_current", gate, primitive.observer(3));
    });

    public static final DeviceType GROUNDED_AND = DeviceType.define(b -> {
        var primitive = PrimitiveDeviceTypes.AND;

        var output = b.terminal(0, "output");
        var supply = b.terminal(1, "supply");
        var inputA = b.terminal(2, "input_a");
        var inputB = b.terminal(3, "input_b");
        var ground = b.ground();

        var threshold = b.parameter(
                0, "threshold", primitive.parameter(0).constraints());
        var maximumConductance = b.parameter(
                1, "maximum_conductance", primitive.parameter(1).constraints());
        var minimumConductance = b.parameter(
                2, "minimum_conductance", primitive.parameter(2).constraints());

        var gate = b.element(primitive, e -> {
            e.connect(primitive.terminal(0), output);
            e.connect(primitive.terminal(1), supply);
            e.connect(primitive.terminal(2), ground);
            e.connect(primitive.terminal(3), inputA);
            e.connect(primitive.terminal(4), inputB);

            e.bind(primitive.parameter(0), threshold);
            e.bind(primitive.parameter(1), maximumConductance);
            e.bind(primitive.parameter(2), minimumConductance);
        });

        b.childObserver(0, "output_voltage", gate, primitive.observer(0));
        b.childObserver(1, "input_a_voltage", gate, primitive.observer(1));
        b.childObserver(2, "input_b_voltage", gate, primitive.observer(2));
        b.childObserver(3, "supply_current", gate, primitive.observer(3));
    });

    public static final DeviceType GROUNDED_NAND = DeviceType.define(b -> {
        var primitive = PrimitiveDeviceTypes.NAND;

        var output = b.terminal(0, "output");
        var supply = b.terminal(1, "supply");
        var inputA = b.terminal(2, "input_a");
        var inputB = b.terminal(3, "input_b");
        var ground = b.ground();

        var threshold = b.parameter(
                0, "threshold", primitive.parameter(0).constraints());
        var maximumConductance = b.parameter(
                1, "maximum_conductance", primitive.parameter(1).constraints());
        var minimumConductance = b.parameter(
                2, "minimum_conductance", primitive.parameter(2).constraints());

        var gate = b.element(primitive, e -> {
            e.connect(primitive.terminal(0), output);
            e.connect(primitive.terminal(1), supply);
            e.connect(primitive.terminal(2), ground);
            e.connect(primitive.terminal(3), inputA);
            e.connect(primitive.terminal(4), inputB);

            e.bind(primitive.parameter(0), threshold);
            e.bind(primitive.parameter(1), maximumConductance);
            e.bind(primitive.parameter(2), minimumConductance);
        });

        b.childObserver(0, "output_voltage", gate, primitive.observer(0));
        b.childObserver(1, "input_a_voltage", gate, primitive.observer(1));
        b.childObserver(2, "input_b_voltage", gate, primitive.observer(2));
        b.childObserver(3, "supply_current", gate, primitive.observer(3));
    });

    public static final DeviceType GROUNDED_NOR = DeviceType.define(b -> {
        var primitive = PrimitiveDeviceTypes.NOR;

        var output = b.terminal(0, "output");
        var supply = b.terminal(1, "supply");
        var inputA = b.terminal(2, "input_a");
        var inputB = b.terminal(3, "input_b");
        var ground = b.ground();

        var threshold = b.parameter(
                0, "threshold", primitive.parameter(0).constraints());
        var maximumConductance = b.parameter(
                1, "maximum_conductance", primitive.parameter(1).constraints());
        var minimumConductance = b.parameter(
                2, "minimum_conductance", primitive.parameter(2).constraints());

        var gate = b.element(primitive, e -> {
            e.connect(primitive.terminal(0), output);
            e.connect(primitive.terminal(1), supply);
            e.connect(primitive.terminal(2), ground);
            e.connect(primitive.terminal(3), inputA);
            e.connect(primitive.terminal(4), inputB);

            e.bind(primitive.parameter(0), threshold);
            e.bind(primitive.parameter(1), maximumConductance);
            e.bind(primitive.parameter(2), minimumConductance);
        });

        b.childObserver(0, "output_voltage", gate, primitive.observer(0));
        b.childObserver(1, "input_a_voltage", gate, primitive.observer(1));
        b.childObserver(2, "input_b_voltage", gate, primitive.observer(2));
        b.childObserver(3, "supply_current", gate, primitive.observer(3));
    });

    public static final DeviceType GROUNDED_NOT = DeviceType.define(b -> {
        var primitive = PrimitiveDeviceTypes.NOT;

        var output = b.terminal(0, "output");
        var supply = b.terminal(1, "supply");
        var input = b.terminal(2, "input");
        var ground = b.ground();

        var threshold = b.parameter(
                0, "threshold", primitive.parameter(0).constraints());
        var maximumConductance = b.parameter(
                1, "maximum_conductance", primitive.parameter(1).constraints());
        var minimumConductance = b.parameter(
                2, "minimum_conductance", primitive.parameter(2).constraints());

        var gate = b.element(primitive, e -> {
            e.connect(primitive.terminal(0), output);
            e.connect(primitive.terminal(1), supply);
            e.connect(primitive.terminal(2), ground);
            e.connect(primitive.terminal(3), input);

            e.bind(primitive.parameter(0), threshold);
            e.bind(primitive.parameter(1), maximumConductance);
            e.bind(primitive.parameter(2), minimumConductance);
        });

        b.childObserver(0, "output_voltage", gate, primitive.observer(0));
        b.childObserver(1, "input_voltage", gate, primitive.observer(1));
        b.childObserver(2, "supply_current", gate, primitive.observer(2));
    });
}
