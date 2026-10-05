package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DeviceDefinitionValidationTest {
    @Test
    void invalidMappingsAreRejectedWithoutPublishingTheDescriptor() {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        MemberMapping empty = new MemberMapping();
        assertThrows(IllegalArgumentException.class, () -> registry.register("test:parameter",
                PrimitiveDeviceTypes.RESISTANCE, new MemberMapping(1), empty, empty));
        assertThrows(IllegalArgumentException.class, () -> registry.register("test:terminal",
                PrimitiveDeviceTypes.RESISTANCE, empty, new MemberMapping(2), empty));
        assertThrows(IllegalArgumentException.class, () -> registry.register("test:observer",
                PrimitiveDeviceTypes.RESISTANCE, empty, empty, new MemberMapping(2)));
        assertNull(registry.get("test:parameter"));
        assertNull(registry.get("test:terminal"));
        assertNull(registry.get("test:observer"));
        assertDoesNotThrow(() -> registry.register("test:parameter", PrimitiveDeviceTypes.RESISTANCE,
                new MemberMapping(-1, 0), new MemberMapping(-1, 0, 1), new MemberMapping(1, 0)));
    }

    @Test
    void compilationRejectsNativeInvalidDefaultsBeforeCreatingDevices() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            runtime.register(PrimitiveDeviceTypes.RESISTANCE);
            DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
            MemberMapping empty = new MemberMapping();
            registry.register("test:resistance", PrimitiveDeviceTypes.RESISTANCE,
                    new MemberMapping(0), empty, empty);
            registry.freeze();
            PortModule ports = new PortModule();
            var domain = ports.<ElectricalPortConnection>registerDomain("test:electrical");
            var conductor = ports.registerStandard("test:conductor", domain, ElectricalPortProfile.class,
                    (first, second, geometry) -> ElectricalPortConnection.DIRECT);
            DeviceConfig invalid = new DeviceConfig("test:invalid", "test:resistance",
                    new DeviceParameterConfig[]{new DeviceParameterConfig(0, 0.0)}, new DevicePortConfig[0]);
            assertThrows(IllegalArgumentException.class, () -> invalid.compile(registry, conductor));
            DeviceConfig valid = new DeviceConfig("test:valid", "test:resistance",
                    new DeviceParameterConfig[]{new DeviceParameterConfig(0, 100.0)}, new DevicePortConfig[0]);
            assertDoesNotThrow(() -> valid.compile(registry, conductor));
            try (var system = runtime.createSystem(20)) {
                assertEquals(1, system.create(PrimitiveDeviceTypes.RESISTANCE).id().value());
            }
        }
    }
}
