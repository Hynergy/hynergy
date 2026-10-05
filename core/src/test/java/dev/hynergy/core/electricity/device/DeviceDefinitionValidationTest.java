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
    void compilationRejectsNativeInvalidDefaultsBeforeCreatingDevices() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            runtime.register(PrimitiveDeviceTypes.RESISTANCE);
            DeviceRegistry registry = new DeviceRegistry(runtime);
            registry.register("test:resistance", PrimitiveDeviceTypes.RESISTANCE);
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
