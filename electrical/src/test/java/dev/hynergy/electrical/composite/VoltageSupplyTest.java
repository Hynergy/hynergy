package dev.hynergy.electrical.composite;

import dev.hynergy.electrical.ElectricalRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VoltageSupplyTest {
    @Test
    void registersWithIndependentVoltageAndResistanceParameters() {
        var type = VoltageSupply.TYPE;
        assertEquals("voltage", type.parameter(0).name());
        assertEquals("output_resistance", type.parameter(1).name());
        try (var runtime = ElectricalRuntime.create()) {
            var binding = runtime.register(type);
            binding.validateParameter(type.parameter(0), 5);
            binding.validateParameter(type.parameter(1), 1);
            assertThrows(IllegalArgumentException.class,
                    () -> binding.validateParameter(type.parameter(1), 0));
            assertThrows(IllegalArgumentException.class,
                    () -> binding.validateParameter(type.parameter(1), -1));
            try (var system = runtime.createSystem(20)) {
                var supply = system.create(type);
                supply.setParameter(type.parameter(0), 5);
                supply.setParameter(type.parameter(1), 1);
                supply.destroy();
            }
        }
    }
}
