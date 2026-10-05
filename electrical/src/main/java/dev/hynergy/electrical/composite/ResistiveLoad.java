package dev.hynergy.electrical.composite;

import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ResistiveLoad {

    public static final DeviceType RESISTIVE_LOAD = DeviceType.define(b -> {
        var resistor = PrimitiveDeviceTypes.RESISTANCE;

        var input = b.terminal(0, "input");
        var ground = b.ground();
        var resistance = b.parameter(
                0, "resistance", resistor.parameter(0).constraints());

        var load = b.element(resistor, e -> {
            e.connect(resistor.terminal(0), input);
            e.connect(resistor.terminal(1), ground);
            e.bind(resistor.parameter(0), resistance);
        });

        b.voltageObserver(0, "voltage", input, ground);
        b.childObserver(1, "current", load, resistor.observer(1));
    });
}
