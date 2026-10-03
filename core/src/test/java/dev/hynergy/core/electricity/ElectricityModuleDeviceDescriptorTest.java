package dev.hynergy.core.electricity;

import dev.hynergy.core.electricity.device.DeviceDescriptor;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class ElectricityModuleDeviceDescriptorTest {
    @Test
    void builtInResistanceDescriptorUsesFrozenStableMemberIds() {
        DeviceDescriptor<Resistance> descriptor = ElectricityModule.RESISTANCE_DESCRIPTOR;

        assertEquals("hynergy:resistance", descriptor.id());
        assertSame(Resistance.TYPE, descriptor.type());
        assertEquals(0, descriptor.parameters().nativeIndex(0));
        assertEquals(-1, descriptor.parameters().nativeIndex(1));
        assertEquals(0, descriptor.terminals().nativeIndex(0));
        assertEquals(1, descriptor.terminals().nativeIndex(1));
        assertEquals(0, descriptor.observers().nativeIndex(0));
        assertEquals(1, descriptor.observers().nativeIndex(1));
    }
}
