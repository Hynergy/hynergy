package dev.hynergy.core.electricity;

import dev.hynergy.core.electricity.device.DeviceRegistry;
import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import dev.hynergy.electrical.composite.GroundedLogicGates;
import dev.hynergy.electrical.composite.GroundedSwitchedLogicGates;
import dev.hynergy.electrical.composite.ResistiveLoad;
import dev.hynergy.electrical.composite.VoltageSupply;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElectricityModuleDeviceRegistryTest {
    @Test
    void builtinRegistrationsPublishPrimitivesAndCompositesBeforeFreezing() {
        try (var runtime = ElectricalRuntime.create()) {
            var registry = new DeviceRegistry(runtime);
            ElectricityModule.registerBuiltinDevices(registry);
            assertFalse(registry.isFrozen());

            assertRegistered(registry, "hynergy:resistance", PrimitiveDeviceTypes.RESISTANCE);
            assertRegistered(registry, "hynergy:conductance", PrimitiveDeviceTypes.CONDUCTANCE);
            assertRegistered(registry, "hynergy:voltage_source", PrimitiveDeviceTypes.VOLTAGE_SOURCE);
            assertRegistered(registry, "hynergy:current_source", PrimitiveDeviceTypes.CURRENT_SOURCE);
            assertRegistered(registry, "hynergy:voltage_controlled_current_source", PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_CURRENT_SOURCE);
            assertRegistered(registry, "hynergy:voltage_controlled_voltage_source", PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_VOLTAGE_SOURCE);
            assertRegistered(registry, "hynergy:capacitor", PrimitiveDeviceTypes.CAPACITOR);
            assertRegistered(registry, "hynergy:inductor", PrimitiveDeviceTypes.INDUCTOR);
            assertRegistered(registry, "hynergy:voltage_controlled_switch", PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_SWITCH);
            assertRegistered(registry, "hynergy:voltage_controlled_conductance", PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_CONDUCTANCE);
            assertRegistered(registry, "hynergy:tick_delay", PrimitiveDeviceTypes.TICK_DELAY);
            assertRegistered(registry, "hynergy:diode", PrimitiveDeviceTypes.DIODE);
            assertRegistered(registry, "hynergy:not", PrimitiveDeviceTypes.NOT);
            assertRegistered(registry, "hynergy:and", PrimitiveDeviceTypes.AND);
            assertRegistered(registry, "hynergy:nand", PrimitiveDeviceTypes.NAND);
            assertRegistered(registry, "hynergy:or", PrimitiveDeviceTypes.OR);
            assertRegistered(registry, "hynergy:nor", PrimitiveDeviceTypes.NOR);
            assertRegistered(registry, "hynergy:schmitt_buffer", PrimitiveDeviceTypes.SCHMITT_BUFFER);
            assertRegistered(registry, "hynergy:voltage_supply", VoltageSupply.TYPE);
            assertRegistered(registry, "hynergy:resistive_load", ResistiveLoad.RESISTIVE_LOAD);
            assertRegistered(registry, "hynergy:grounded_and", GroundedLogicGates.GROUNDED_AND);
            assertRegistered(registry, "hynergy:grounded_or", GroundedLogicGates.GROUNDED_OR);
            assertRegistered(registry, "hynergy:grounded_nand", GroundedLogicGates.GROUNDED_NAND);
            assertRegistered(registry, "hynergy:grounded_nor", GroundedLogicGates.GROUNDED_NOR);
            assertRegistered(registry, "hynergy:grounded_not", GroundedLogicGates.GROUNDED_NOT);
            assertRegistered(registry, "hynergy:grounded_switched_not", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOT);
            assertRegistered(registry, "hynergy:grounded_switched_and", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_AND);
            assertRegistered(registry, "hynergy:grounded_switched_nand", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NAND);
            assertRegistered(registry, "hynergy:grounded_switched_or", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_OR);
            assertRegistered(registry, "hynergy:grounded_switched_nor", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOR);

            registry.freeze();
            assertThrows(IllegalStateException.class,
                    () -> registry.register("hynergy:late", PrimitiveDeviceTypes.RESISTANCE));
            try (var system = runtime.createSystem(20)) {
                for (var type : new DeviceType[]{
                        VoltageSupply.TYPE, ResistiveLoad.RESISTIVE_LOAD,
                        GroundedLogicGates.GROUNDED_AND, GroundedLogicGates.GROUNDED_OR,
                        GroundedLogicGates.GROUNDED_NAND, GroundedLogicGates.GROUNDED_NOR,
                        GroundedLogicGates.GROUNDED_NOT
                }) {
                    var device = system.create(type);
                    device.requireDefinition(type);
                    device.destroy();
                }
            }
        }
    }

    private static void assertRegistered(DeviceRegistry registry, String id, DeviceType type) {
        var registration = registry.require(id);
        assertEquals(id, registration.id());
        assertSame(type, registration.type());
        assertSame(type, registration.binding().type());
    }

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
