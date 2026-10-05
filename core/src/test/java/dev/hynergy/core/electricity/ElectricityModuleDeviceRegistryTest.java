package dev.hynergy.core.electricity;

import dev.hynergy.core.electricity.device.DeviceRegistry;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ElectricityModuleDeviceRegistryTest {
    @Test
    void builtinResistancePreservesAssetAndMemberIds() {
        try (var runtime = ElectricalRuntime.create()) {
            var registration = new DeviceRegistry(runtime).register(ElectricityModule.RESISTANCE_ID, Resistance.TYPE);
            assertEquals("hynergy:resistance", registration.id());
            assertSame(Resistance.RESISTANCE, registration.type().parameter(0));
            assertSame(Resistance.POSITIVE, registration.type().terminal(0));
            assertSame(Resistance.NEGATIVE, registration.type().terminal(1));
            assertSame(Resistance.VOLTAGE, registration.type().observer(0));
            assertSame(Resistance.CURRENT, registration.type().observer(1));
            registration.binding().validateParameter(Resistance.RESISTANCE, 100);
        }
    }
}
