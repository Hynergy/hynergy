package dev.hynergy.core.electricity.device;

import dev.hynergy.core.port.BlockPortDefinition;
import dev.hynergy.electrical.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

final class ElectricalDeviceSystemTest {
    private static final DeviceType TYPE = DeviceTestTypes.resistor(4, 0, 1, 1);
    private static final DeviceType TYPE7 = DeviceTestTypes.resistor(7, 0, 1, 1);

    private static ElectricalSystem prepareSystem(ElectricalRuntime runtime) {
        runtime.register(TYPE);
        runtime.register(TYPE7);
        return runtime.createSystem(20);
    }

    @Test
    void invalidRestoreDoesNotQueueEarlierValuesOrMigrateOverrides() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = prepareSystem(runtime)) {
            Device source = createVoltageSource(system, 5.0);
            Device resistance = system.create(TYPE);
            resistance.setParameter(TYPE.parameter(4), 10.0);
            connect(system, source, resistance, TYPE);
            ArrayList<Double> currents = new ArrayList<>();
            resistance.observe(TYPE.observer(1), (status, value) -> currents.add(value));
            system.tick();
            assertEquals(0.5, currents.getLast(), 1e-9);

            DeviceComponent component = new DeviceComponent("test:device");
            component.setDeviceId(resistance.id());
            component.overrides().set(9, 123.0);
            CompiledDeviceConfig valid = compiled(runtime, 4, 20.0);
            CompiledDeviceConfig invalid = new CompiledDeviceConfig(valid.registration(),
                    java.util.List.of(new CompiledDeviceConfig.ParameterBinding(TYPE.parameter(4), 20.0), new CompiledDeviceConfig.ParameterBinding(PrimitiveDeviceTypes.VOLTAGE_SOURCE.parameter(0), 1.0)),
                    java.util.List.of(), BlockPortDefinition.of());
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
             ElectricalSystem system = prepareSystem(runtime)) {
            DeviceComponent component = new DeviceComponent("test:device");
            assertThrows(IllegalArgumentException.class,
                    () -> ElectricalDeviceSystem.bindDevice(component, compiled(runtime, 4, 0.0), system, () -> {
                    }));
            assertNull(component.device());
            assertNull(component.getDeviceId());
            assertDoesNotThrow(system::tick);
        }
    }

    @Test
    void createPersistsIdentityAndReloadResolvesWithoutAllocatingAnotherNativeDevice() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = prepareSystem(runtime)) {
            DeviceComponent component = new DeviceComponent("test:device");
            CompiledDeviceConfig compiled = compiled(runtime, 4, 10.0);

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

            Device next = system.create(TYPE);
            assertEquals(persisted.value() + 1, next.id().value());
        }
    }

    @Test
    void permanentRemovalResolvesUnloadedDeviceDestroysNativeStateAndClearsIdentity() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = prepareSystem(runtime)) {
            DeviceComponent component = new DeviceComponent("test:device");
            CompiledDeviceConfig compiled = compiled(runtime, 4, 10.0);
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
                    () -> system.resolveDevice(removedId, TYPE)
            );
        }
    }

    @Test
    void synchronizationAppliesConfiguredDefaultWhenNoOverrideExists() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = prepareSystem(runtime)) {
            Device source = createVoltageSource(system, 5.0);
            DeviceComponent component = new DeviceComponent("test:device");
            CompiledDeviceConfig compiled = compiled(runtime, 4, 10.0);

            ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
            });
            Device resistance = Objects.requireNonNull(component.device());
            ArrayList<Double> currents = new ArrayList<>();
            resistance.observe(TYPE.observer(1), (status, value) -> currents.add(value));

            connect(system, source, resistance, TYPE);
            system.tick();

            assertEquals(0.5, currents.getFirst(), 1e-9);
        }
    }

    @Test
    void synchronizationUsesOverrideInsteadOfDefaultAndIssuesOneEffectiveParameterValue() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = prepareSystem(runtime)) {
            Device source = createVoltageSource(system, 5.0);
            DeviceComponent component = new DeviceComponent("test:device");
            component.overrides().set(4, 20.0);
            CompiledDeviceConfig compiled = compiled(runtime, 4, 10.0);

            ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
            });
            Device resistance = Objects.requireNonNull(component.device());

            ArrayList<ObservationStatus> statuses = new ArrayList<>();
            ArrayList<Double> currents = new ArrayList<>();
            resistance.observe(TYPE.observer(1), (status, value) -> {
                statuses.add(status);
                currents.add(value);
            });

            connect(system, source, resistance, TYPE);
            system.tick();

            assertEquals(1, statuses.size());
            assertEquals(ObservationStatus.AVAILABLE, statuses.getFirst());
            assertEquals(0.25, currents.getFirst(), 1e-9);
        }
    }

    @Test
    void mappedPersistedOverrideWithoutCurrentConfigDefaultRemainsValidAndIsApplied() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = prepareSystem(runtime)) {
            Device source = createVoltageSource(system, 5.0);
            DeviceComponent component = new DeviceComponent("test:device");
            component.overrides().set(7, 20.0);
            CompiledDeviceConfig compiled = compiledWithoutDefault(runtime);

            ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
            });
            Device resistance = Objects.requireNonNull(component.device());
            ArrayList<Double> currents = new ArrayList<>();
            resistance.observe(TYPE7.observer(1), (status, value) -> currents.add(value));

            connect(system, source, resistance, TYPE7);
            system.tick();

            assertTrue(component.overrides().contains(7));
            assertEquals(0.25, currents.getFirst(), 1e-9);
        }
    }

    @Test
    void retiredOverrideIsRemovedDuringRestoreAndMarksPersistenceDirty() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = prepareSystem(runtime)) {
            Device existing = system.create(TYPE);
            DeviceComponent component = new DeviceComponent("test:device");
            component.setDeviceId(existing.id());
            component.overrides().set(9, 123.0);
            CompiledDeviceConfig compiled = compiled(runtime, 4, 10.0);

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
             ElectricalSystem system = prepareSystem(runtime)) {
            Device resistance = system.create(TYPE);
            Wire wire = system.createWire();
            DeviceWireConnections.AttachmentDedup dedup =
                    new DeviceWireConnections.AttachmentDedup();

            assertTrue(DeviceWireConnections.attachIfNew(resistance, TYPE.terminal(0), wire, dedup));
            assertFalse(DeviceWireConnections.attachIfNew(resistance, TYPE.terminal(0), wire, dedup));
            assertDoesNotThrow(system::tick);

            resistance.detachTerminal(TYPE.terminal(0), wire);
            assertDoesNotThrow(system::tick);
        }
    }

    @Test
    void unloadedNeighborWireIsSkippedWithoutConsumingDedupEntry() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = prepareSystem(runtime)) {
            Device resistance = system.create(TYPE);
            Wire wire = system.createWire();
            DeviceWireConnections.AttachmentDedup dedup =
                    new DeviceWireConnections.AttachmentDedup();

            assertFalse(DeviceWireConnections.attachIfNew(resistance, TYPE.terminal(0), null, dedup));
            assertTrue(DeviceWireConnections.attachIfNew(resistance, TYPE.terminal(0), wire, dedup));
            assertDoesNotThrow(system::tick);
        }
    }

    private static CompiledDeviceConfig compiled(ElectricalRuntime runtime, int stableParameterId, double defaultValue) {
        var registration = new DeviceRegistry(runtime).register("test:device", TYPE);
        return new CompiledDeviceConfig(registration,
                java.util.List.of(new CompiledDeviceConfig.ParameterBinding(TYPE.parameter(stableParameterId), defaultValue)),
                java.util.List.of(), BlockPortDefinition.of());
    }

    private static CompiledDeviceConfig compiledWithoutDefault(ElectricalRuntime runtime) {
        return new CompiledDeviceConfig(new DeviceRegistry(runtime).register("test:device", TYPE7),
                java.util.List.of(), java.util.List.of(), BlockPortDefinition.of());
    }

    private static Device createVoltageSource(ElectricalSystem system, double voltage) {
        Device source = system.create(PrimitiveDeviceTypes.VOLTAGE_SOURCE);
        source.setParameter(PrimitiveDeviceTypes.VOLTAGE_SOURCE.parameter(0), voltage);
        return source;
    }

    private static void connect(ElectricalSystem system, Device source, Device resistance, DeviceType type) {
        Wire positive = system.createWire();
        Wire negative = system.createWire();
        source.attachTerminal(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(0), positive);
        source.attachTerminal(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(1), negative);
        resistance.attachTerminal(type.terminal(0), positive);
        resistance.attachTerminal(type.terminal(1), negative);
    }

}
