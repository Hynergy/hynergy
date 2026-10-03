package dev.hynergy.core.electricity.device;

import dev.hynergy.core.port.BlockPortDefinition;
import dev.hynergy.electrical.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

final class ElectricalDeviceSystemTest {
    @Test
    void invalidRestoreDoesNotQueueEarlierValuesOrMigrateOverrides() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            TestVoltageSource source = createVoltageSource(system, 5.0);
            TestResistance resistance = system.create(TestResistance.TYPE);
            DeviceAccess.setParameter(resistance, 0, 10.0);
            connect(system, source, resistance);
            ArrayList<Double> currents = new ArrayList<>();
            DeviceAccess.observe(resistance, 1, (status, value) -> currents.add(value));
            system.tick();
            assertEquals(0.5, currents.getLast(), 1e-9);

            DeviceComponent component = new DeviceComponent("test:device");
            component.setDeviceId(resistance.id());
            component.overrides().set(9, 123.0);
            CompiledDeviceConfig valid = compiled(4, 20.0);
            CompiledDeviceConfig invalid = new CompiledDeviceConfig(valid.descriptor(),
                    new int[]{4, 5}, new int[]{0, 1}, new double[]{20.0, 1.0},
                    new int[0], new int[0], new int[0], BlockPortDefinition.of());
            assertThrows(IllegalArgumentException.class,
                    () -> ElectricalDeviceSystem.bindDevice(component, invalid, system, () -> {
                    }));
            assertNull(component.device());
            assertTrue(component.overrides().contains(9));
            assertEquals(resistance.id(), component.getDeviceId());
            assertDoesNotThrow(system::tick);
            assertEquals(0.5, currents.getLast(), 1e-9);
        }
    }

    @Test
    void invalidConfiguredDefaultDoesNotLeaveADeviceOrPoisonTheWorld() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            DeviceComponent component = new DeviceComponent("test:device");
            assertThrows(IllegalArgumentException.class,
                    () -> ElectricalDeviceSystem.bindDevice(component, compiled(4, 0.0), system, () -> {
                    }));
            assertNull(component.device());
            assertNull(component.getDeviceId());
            assertDoesNotThrow(system::tick);
        }
    }

    @Test
    void createPersistsIdentityAndReloadResolvesWithoutAllocatingAnotherNativeDevice() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            DeviceComponent component = new DeviceComponent("test:device");
            CompiledDeviceConfig compiled = compiled(4, 10.0);

            ElectricalDeviceSystem.BindResult created =
                    ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
                    });
            DeviceId persisted = Objects.requireNonNull(component.getDeviceId());
            Device firstHandle = Objects.requireNonNull(component.device());

            assertTrue(created.created());
            assertTrue(created.persistenceChanged());
            assertEquals(firstHandle.id(), persisted);

            ElectricalDeviceSystem.unloadDevice(component);
            assertEquals(persisted, component.getDeviceId());
            assertNull(component.device());
            assertNull(component.compiledConfig());

            ElectricalDeviceSystem.BindResult restored =
                    ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
                    });

            assertFalse(restored.created());
            assertFalse(restored.persistenceChanged());
            assertEquals(persisted, Objects.requireNonNull(component.device()).id());

            TestResistance next = system.create(TestResistance.TYPE);
            assertEquals(persisted.value() + 1, next.id().value());
        }
    }

    @Test
    void permanentRemovalResolvesUnloadedDeviceDestroysNativeStateAndClearsIdentity() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            DeviceComponent component = new DeviceComponent("test:device");
            CompiledDeviceConfig compiled = compiled(4, 10.0);
            ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
            });
            DeviceId removedId = Objects.requireNonNull(component.getDeviceId());

            ElectricalDeviceSystem.unloadDevice(component);
            ElectricalDeviceSystem.destroyDevice(component, compiled, system);

            assertNull(component.getDeviceId());
            assertNull(component.device());
            assertNull(component.compiledConfig());
            assertThrows(
                    IllegalStateException.class,
                    () -> system.resolveDevice(removedId, TestResistance.TYPE)
            );
        }
    }

    @Test
    void synchronizationAppliesConfiguredDefaultWhenNoOverrideExists() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            TestVoltageSource source = createVoltageSource(system, 5.0);
            DeviceComponent component = new DeviceComponent("test:device");
            CompiledDeviceConfig compiled = compiled(4, 10.0);

            ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
            });
            TestResistance resistance = (TestResistance) Objects.requireNonNull(component.device());
            ArrayList<Double> currents = new ArrayList<>();
            DeviceAccess.observe(resistance, 1, (status, value) -> currents.add(value));

            connect(system, source, resistance);
            system.tick();

            assertEquals(0.5, currents.getFirst(), 1e-9);
        }
    }

    @Test
    void synchronizationUsesOverrideInsteadOfDefaultAndIssuesOneEffectiveParameterValue() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            TestVoltageSource source = createVoltageSource(system, 5.0);
            DeviceComponent component = new DeviceComponent("test:device");
            component.overrides().set(4, 20.0);
            CompiledDeviceConfig compiled = compiled(4, 10.0);

            ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
            });
            TestResistance resistance = (TestResistance) Objects.requireNonNull(component.device());

            ArrayList<ObservationStatus> statuses = new ArrayList<>();
            ArrayList<Double> currents = new ArrayList<>();
            DeviceAccess.observe(resistance, 1, (status, value) -> {
                statuses.add(status);
                currents.add(value);
            });

            connect(system, source, resistance);
            system.tick();

            assertEquals(1, statuses.size());
            assertEquals(ObservationStatus.AVAILABLE, statuses.getFirst());
            assertEquals(0.25, currents.getFirst(), 1e-9);
        }
    }

    @Test
    void mappedPersistedOverrideWithoutCurrentConfigDefaultRemainsValidAndIsApplied() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            TestVoltageSource source = createVoltageSource(system, 5.0);
            DeviceComponent component = new DeviceComponent("test:device");
            component.overrides().set(7, 20.0);
            CompiledDeviceConfig compiled = compiledWithoutDefault(7, 0);

            ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
            });
            TestResistance resistance = (TestResistance) Objects.requireNonNull(component.device());
            ArrayList<Double> currents = new ArrayList<>();
            DeviceAccess.observe(resistance, 1, (status, value) -> currents.add(value));

            connect(system, source, resistance);
            system.tick();

            assertTrue(component.overrides().contains(7));
            assertEquals(0.25, currents.getFirst(), 1e-9);
        }
    }

    @Test
    void retiredOverrideIsRemovedDuringRestoreAndMarksPersistenceDirty() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            TestResistance existing = system.create(TestResistance.TYPE);
            DeviceComponent component = new DeviceComponent("test:device");
            component.setDeviceId(existing.id());
            component.overrides().set(9, 123.0);
            CompiledDeviceConfig compiled = compiled(4, 10.0);

            ElectricalDeviceSystem.BindResult result =
                    ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
                    });

            assertFalse(result.created());
            assertTrue(result.persistenceChanged());
            assertFalse(component.overrides().contains(9));
            assertEquals(existing.id(), component.getDeviceId());
        }
    }

    @Test
    void duplicatePhysicalDiscoveriesForSameTerminalAndWireAttachOnlyOnce() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            TestResistance resistance = system.create(TestResistance.TYPE);
            Wire wire = system.createWire();
            ElectricalDeviceSystem.AttachmentDedup dedup =
                    new ElectricalDeviceSystem.AttachmentDedup();

            assertTrue(ElectricalDeviceSystem.attachIfNew(resistance, 0, wire, dedup));
            assertFalse(ElectricalDeviceSystem.attachIfNew(resistance, 0, wire, dedup));
            assertDoesNotThrow(system::tick);

            DeviceAccess.detachTerminal(resistance, 0, wire);
            assertDoesNotThrow(system::tick);
        }
    }

    @Test
    void unloadedNeighborWireIsSkippedWithoutConsumingDedupEntry() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            TestResistance resistance = system.create(TestResistance.TYPE);
            Wire wire = system.createWire();
            ElectricalDeviceSystem.AttachmentDedup dedup =
                    new ElectricalDeviceSystem.AttachmentDedup();

            assertFalse(ElectricalDeviceSystem.attachIfNew(resistance, 0, null, dedup));
            assertTrue(ElectricalDeviceSystem.attachIfNew(resistance, 0, wire, dedup));
            assertDoesNotThrow(system::tick);
        }
    }

    private static CompiledDeviceConfig compiled(
            int stableParameterId,
            double defaultValue
    ) {
        DeviceDescriptor<TestResistance> descriptor = new DeviceDescriptor<>(
                "test:resistance",
                TestResistance.TYPE,
                mappingAt(stableParameterId, 0),
                new MemberMapping(0, 1),
                new MemberMapping(0, 1)
        );

        int nativeParameterId = descriptor.parameters().nativeIndex(stableParameterId);
        return new CompiledDeviceConfig(
                descriptor,
                new int[]{stableParameterId},
                new int[]{nativeParameterId},
                new double[]{defaultValue},
                new int[0],
                new int[0],
                new int[0],
                BlockPortDefinition.of()
        );
    }

    private static CompiledDeviceConfig compiledWithoutDefault(int stableParameterId, int nativeParameterId) {
        DeviceDescriptor<TestResistance> descriptor = new DeviceDescriptor<>(
                "test:resistance",
                TestResistance.TYPE,
                mappingAt(stableParameterId, nativeParameterId),
                new MemberMapping(0, 1),
                new MemberMapping(0, 1)
        );
        return new CompiledDeviceConfig(
                descriptor,
                new int[0],
                new int[0],
                new double[0],
                new int[0],
                new int[0],
                new int[0],
                BlockPortDefinition.of()
        );
    }

    private static MemberMapping mappingAt(int stableId, int nativeId) {
        int[] mapping = new int[stableId + 1];
        Arrays.fill(mapping, MemberMapping.UNMAPPED);
        mapping[stableId] = nativeId;
        return new MemberMapping(mapping);
    }

    private static TestVoltageSource createVoltageSource(ElectricalSystem system, double voltage) {
        TestVoltageSource source = system.create(TestVoltageSource.TYPE);
        DeviceAccess.setParameter(source, 0, voltage);
        return source;
    }

    private static void connect(
            ElectricalSystem system,
            TestVoltageSource source,
            TestResistance resistance
    ) {
        Wire positive = system.createWire();
        Wire negative = system.createWire();
        DeviceAccess.attachTerminal(source, 0, positive);
        DeviceAccess.attachTerminal(source, 1, negative);
        DeviceAccess.attachTerminal(resistance, 0, positive);
        DeviceAccess.attachTerminal(resistance, 1, negative);
    }

    private static final class TestResistance extends Device {
        private static final DeviceType<TestResistance> TYPE = DeviceType.primitive(1, TestResistance::new);
    }

    private static final class TestVoltageSource extends Device {
        private static final DeviceType<TestVoltageSource> TYPE = DeviceType.primitive(3, TestVoltageSource::new);
    }
}
