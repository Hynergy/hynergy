package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.electrical.*;
import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class CustomDevicePersistenceTest {
    @Test
    void customAssetUsesGenericHandlesAndRestoresOverridesAndRetainedConnections() {
        DeviceType type = DeviceType.create(builder -> {
            int positive = builder.addTerminal();
            int negative = builder.addTerminal();
            int resistance = builder.addParameter(DeviceDefinitionBuilder.Bound.inclusive(10.0),
                    DeviceDefinitionBuilder.Bound.exclusive(100.0), true);
            builder.beginElement(Resistance.TYPE)
                    .elementTerminal(positive).elementTerminal(negative)
                    .elementParameter(resistance).endElement();
            builder.addChildObserver(0, 1);
        });
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        DeviceDescriptor descriptor = registry.register("test:custom", type,
                new MemberMapping(-1, -1, -1, -1, 0),
                new MemberMapping(-1, -1, 0, -1, -1, 1),
                new MemberMapping(-1, -1, -1, -1, -1, -1, 0));
        registry.freeze();
        PortModule ports = new PortModule();
        var domain = ports.<ElectricalPortConnection>registerDomain("test:electrical");
        var conductor = ports.registerStandard("test:conductor", domain,
                ElectricalPortProfile.class, (first, second, geometry) -> ElectricalPortConnection.DIRECT);
        DeviceConfig config = new DeviceConfig("test:asset", "test:custom",
                new DeviceParameterConfig[]{new DeviceParameterConfig(4, 10.0)},
                new DevicePortConfig[]{
                        new DevicePortConfig(9, 2, null, new Vector3i(1, 0, 0)),
                        new DevicePortConfig(12, 5, null, new Vector3i(-1, 0, 0))});
        CompiledDeviceConfig compiled = config.compile(registry, conductor);
        assertSame(descriptor, compiled.descriptor());
        assertEquals(9, compiled.portIdAt(0));
        assertEquals(0, compiled.nativeTerminalIdAt(0));
        assertEquals(1, compiled.nativeTerminalIdAt(1));
        AtomicInteger dirty = new AtomicInteger();
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            try (ElectricalSystem system = runtime.createSystem(20)) {
                DeviceComponent component = new DeviceComponent("test:asset");
                ElectricalDeviceSystem.bindDevice(component, compiled, system, dirty::incrementAndGet);
                Device handle = component.device();
                assertNotNull(handle);
                assertEquals(Device.class, handle.getClass());
                VoltageSource source = VoltageSource.create(system, 5.0);
                Wire positive = system.createWire();
                Wire negative = system.createWire();
                source.attachPositive(positive);
                source.attachNegative(negative);
                handle.attachTerminal(compiled.nativeTerminalIdAt(0), positive);
                handle.attachTerminal(compiled.nativeTerminalIdAt(1), negative);
                ArrayList<Double> currents = new ArrayList<>();
                component.observe(6, (status, value) -> currents.add(value));
                component.setParameter(4, 20.0);
                assertEquals(1, dirty.get());
                assertThrows(IllegalArgumentException.class, () -> component.setParameter(4, 0.0));
                assertEquals(1, dirty.get());
                system.tick();
                assertEquals(0.25, currents.getLast(), 1e-9);
                DeviceComponent restored = DeviceComponent.CODEC.decode(DeviceComponent.CODEC.encode(component));
                assertEquals(handle.id(), restored.getDeviceId());
                assertEquals(20.0, restored.overrides().getOrDefault(4, -1.0));
                assertNull(restored.device());
                ElectricalDeviceSystem.unloadDevice(component);
                var result = ElectricalDeviceSystem.bindDevice(restored, compiled, system, dirty::incrementAndGet);
                assertFalse(result.created());
                assertFalse(result.persistenceChanged());
                assertEquals(handle.id(), restored.device().id());
                assertNotSame(handle, restored.device());
                system.tick();
                assertEquals(0.25, currents.getLast(), 1e-9);
                restored.setParameter(4, 50.0);
                system.tick();
                assertEquals(0.1, currents.getLast(), 1e-9);
                assertEquals(2, dirty.get());
                assertEquals(source.id().value() + 1, system.create(type).id().value());
            }
        }
    }
}
