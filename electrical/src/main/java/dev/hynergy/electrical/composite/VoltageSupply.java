package dev.hynergy.electrical.composite;

import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class VoltageSupply {

    public static DeviceType TYPE = DeviceType.define(b -> {
        var source = PrimitiveDeviceTypes.VOLTAGE_SOURCE;
        var resistor = PrimitiveDeviceTypes.RESISTANCE;

        var output = b.terminal(0, "output");
        var sourcePositive = b.node();
        var ground = b.ground();

        var voltage = b.parameter(0, "voltage", source.parameter(0).constraints());
        var outputResistance = b.parameter(1, "output_resistance", resistor.parameter(0).constraints());

        b.element(source, e -> {
            e.connect(source.terminal(0), sourcePositive);
            e.connect(source.terminal(1), ground);
            e.bind(source.parameter(0), voltage);
        });


        var seriesResistor = b.element(resistor, e -> {
            e.connect(resistor.terminal(0), sourcePositive);
            e.connect(resistor.terminal(1), output);
            e.bind(resistor.parameter(0), outputResistance);
        });

        b.voltageObserver(0, "output_voltage", output, ground);
        b.childObserver(1, "output_current", seriesResistor, resistor.observer(1));
    });
}
