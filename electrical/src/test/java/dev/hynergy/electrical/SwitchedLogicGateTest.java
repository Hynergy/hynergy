package dev.hynergy.electrical;

import dev.hynergy.electrical.primitives.logic.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwitchedLogicGateTest {
    @Test
    void rejectedFactoryDoesNotLeaveAnIncompleteDevice() {
        try (var runtime = ElectricalRuntime.create(); var system = runtime.createSystem(20)) {
            java.util.List<java.util.function.BiFunction<Double, Double, Device>> factories = java.util.List.of(
                    (off, bias) -> SwitchedNot.create(system, 2.5, 1, off, bias, 1e-6).device(),
                    (off, bias) -> SwitchedAnd.create(system, 2.5, 1, off, bias, 1e-6).device(),
                    (off, bias) -> SwitchedNand.create(system, 2.5, 1, off, bias, 1e-6).device(),
                    (off, bias) -> SwitchedOr.create(system, 2.5, 1, off, bias, 1e-6).device(),
                    (off, bias) -> SwitchedNor.create(system, 2.5, 1, off, bias, 1e-6).device());
            for (var factory : factories) {
                assertThrows(IllegalArgumentException.class, () -> factory.apply(0.0, 0.0));
                assertDoesNotThrow(system::tick);
                assertThrows(IllegalArgumentException.class, () -> factory.apply(1.0, 1e-6));
                assertDoesNotThrow(system::tick);
            }
        }
    }

    @Test
    void registersNativeSchemasAndRejectsInvalidBiases() {
        var types = new DeviceType[]{SwitchedNot.TYPE, SwitchedAnd.TYPE, SwitchedNand.TYPE, SwitchedOr.TYPE, SwitchedNor.TYPE};
        try (var runtime = ElectricalRuntime.create()) {
            for (int i = 0; i < types.length; i++) {
                var type = types[i];
                var binding = runtime.register(type);
                assertEquals(19 + i, binding.definition().id());
                for (int parameter : new int[]{3, 4}) {
                    for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
                        assertThrows(IllegalArgumentException.class, () -> binding.validateParameter(type.parameter(parameter), invalid));
                    }
                }
            }
        }
    }
}
