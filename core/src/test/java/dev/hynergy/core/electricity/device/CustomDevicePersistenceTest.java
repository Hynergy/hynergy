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
    @Test void invalidLaterPersistedOverrideLeavesRetainedParametersAndTopologyIntact() {
        var type = DeviceType.define(b -> {
            var p = b.terminal(2, "positive"); var n = b.terminal(5, "negative");
            var first = b.parameter(4, "first", ParameterConstraints.positiveFinite());
            var second = b.parameter(8, "second", ParameterConstraints.positiveFinite());
            var observed = b.element(Resistance.TYPE, e -> {
                e.connect(Resistance.POSITIVE, p); e.connect(Resistance.NEGATIVE, n); e.bind(Resistance.RESISTANCE, first);
            });
            b.element(Resistance.TYPE, e -> {
                e.connect(Resistance.POSITIVE, p); e.connect(Resistance.NEGATIVE, n); e.bind(Resistance.RESISTANCE, second);
            });
            b.childObserver(6, "current", observed, Resistance.CURRENT);
        });
        try (var runtime = ElectricalRuntime.create()) {
            var registry = new DeviceRegistry(runtime);
            var registration = registry.register("test:pair", type);
            assertThrows(IllegalArgumentException.class, () -> registration.binding().validateParameter(type.parameter(8), 0));
            var compiled = new CompiledDeviceConfig(registration, java.util.List.of(
                new CompiledDeviceConfig.ParameterBinding(type.parameter(4), 20),
                new CompiledDeviceConfig.ParameterBinding(type.parameter(8), 20)),
                java.util.List.of(), dev.hynergy.core.port.BlockPortDefinition.of());
            try (var system = runtime.createSystem(20)) {
                var component = new DeviceComponent("test:pair");
                ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {});
                var retained = component.device(); assertNotNull(retained);
                var source = VoltageSource.create(system, 5);
                var p = system.createWire(); var n = system.createWire();
                source.attachPositive(p); source.attachNegative(n);
                retained.attachTerminal(type.terminal(2), p); retained.attachTerminal(type.terminal(5), n);
                var currents = new ArrayList<Double>();
                component.observe(6, (status, value) -> currents.add(value));
                system.tick(); assertEquals(0.25, currents.getLast(), 1e-9);
                var serialized = DeviceComponent.CODEC.decode(DeviceComponent.CODEC.encode(component));
                serialized.overrides().set(4, 40); serialized.overrides().set(8, 0);
                ElectricalDeviceSystem.unloadDevice(component);
                var saves = new AtomicInteger();
                assertThrows(IllegalArgumentException.class, () -> ElectricalDeviceSystem.bindDevice(serialized, compiled, system, saves::incrementAndGet));
                assertNull(serialized.device()); assertEquals(retained.id(), serialized.getDeviceId());
                assertEquals(40, serialized.overrides().getOrDefault(4, -1));
                assertEquals(0, serialized.overrides().getOrDefault(8, -1)); assertEquals(0, saves.get());
                system.tick(); assertEquals(0.25, currents.getLast(), 1e-9);
                assertEquals(source.id().value() + 1, system.create(type).id().value());
            }
        }
    }
    @Test
    void customAssetUsesGenericHandlesAndRestoresOverridesAndRetainedConnections() {
        DeviceType type = DeviceType.define(builder -> {
            var p = builder.terminal(2, "positive");
            var middle = builder.node();
            var ground = builder.ground();
            var n = builder.terminal(5, "negative");
            var r = builder.parameter(4, "resistance", new ParameterConstraints(
                ParameterConstraints.Bound.inclusive(10), ParameterConstraints.Bound.exclusive(100), true, false, null, null));
            var child = builder.element(Resistance.TYPE, e -> {
                e.bind(Resistance.RESISTANCE, r); e.connect(Resistance.NEGATIVE, middle); e.connect(Resistance.POSITIVE, p);
            });
            builder.element(VoltageSource.TYPE, e -> {
                e.literal(VoltageSource.VOLTAGE, 0); e.connect(VoltageSource.NEGATIVE, n); e.connect(VoltageSource.POSITIVE, middle);
            });
            builder.element(VoltageSource.TYPE, e -> {
                e.connect(VoltageSource.NEGATIVE, ground); e.connect(VoltageSource.POSITIVE, n); e.literal(VoltageSource.VOLTAGE, 0);
            });
            builder.childObserver(6, "current", child, Resistance.CURRENT);
        });
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            DeviceRegistry registry = new DeviceRegistry(runtime);
            DeviceRegistration descriptor = registry.register("test:custom", type);
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
            assertSame(descriptor, compiled.registration());
            assertEquals(9, compiled.ports().get(0).portId());
            assertEquals(12, compiled.ports().get(1).portId());
            assertSame(type.terminal(2), compiled.ports().get(0).terminal());
            assertSame(type.terminal(5), compiled.ports().get(1).terminal());
            AtomicInteger dirty = new AtomicInteger();
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
                handle.attachTerminal(compiled.ports().get(0).terminal(), positive);
                handle.attachTerminal(compiled.ports().get(1).terminal(), negative);
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
