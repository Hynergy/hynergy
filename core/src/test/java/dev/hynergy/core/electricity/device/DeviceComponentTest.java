package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalCodecs;
import dev.hynergy.core.port.BlockPortDefinition;
import dev.hynergy.electrical.*;
import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.bson.BsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceComponentTest {
    @Test
    void signedZeroOverrideIsPreservedUntilTheExactDefaultIsRestored() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            DeviceDescriptor descriptor = new DeviceDescriptor("test:voltage", VoltageSource.TYPE,
                    new MemberMapping(0), new MemberMapping(0, 1), new MemberMapping(0, 1));
            CompiledDeviceConfig compiled = new CompiledDeviceConfig(descriptor,
                    new int[]{0}, new int[]{0}, new double[]{0.0},
                    new int[0], new int[0], new int[0], BlockPortDefinition.of());
            DeviceComponent component = new DeviceComponent("test:voltage");
            java.util.ArrayList<Long> saved = new java.util.ArrayList<>();
            ElectricalDeviceSystem.bindDevice(component, compiled, system,
                    () -> saved.add(Double.doubleToLongBits(component.overrides().getOrDefault(0, 0.0))));
            component.setParameter(0, -0.0);
            assertTrue(component.overrides().contains(0));
            assertEquals(Long.MIN_VALUE, Double.doubleToLongBits(component.overrides().valueAt(0)));
            component.setParameter(0, 0.0);
            assertFalse(component.overrides().contains(0));
            assertEquals(java.util.List.of(Long.MIN_VALUE, 0L), saved);
        }
    }

    @Test
    void acceptedOverrideChangesMarkSavingButRejectedAndUnchangedValuesDoNot() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            DeviceComponent component = new DeviceComponent("test:device");
            java.util.ArrayList<Double> savedOverrides = new java.util.ArrayList<>();
            ElectricalDeviceSystem.bindDevice(component, compiled(4, 100.0, 6), system,
                    () -> savedOverrides.add(component.overrides().getOrDefault(4, 100.0)));

            component.setParameter(4, 470.0);
            assertEquals(java.util.List.of(470.0), savedOverrides);
            component.setParameter(4, 470.0);
            assertThrows(IllegalArgumentException.class, () -> component.setParameter(4, 0.0));
            assertEquals(java.util.List.of(470.0), savedOverrides);
            assertEquals(470.0, component.overrides().getOrDefault(4, -1.0));

            component.setParameter(4, 100.0);
            assertFalse(component.overrides().contains(4));
            assertEquals(java.util.List.of(470.0, 100.0), savedOverrides);

            ElectricalDeviceSystem.unloadDevice(component);
            assertThrows(IllegalStateException.class, () -> component.setParameter(4, 220.0));
            java.util.ArrayList<Double> reloadedSaves = new java.util.ArrayList<>();
            ElectricalDeviceSystem.bindDevice(component, compiled(4, 100.0, 6), system,
                    () -> reloadedSaves.add(component.overrides().getOrDefault(4, 100.0)));
            component.setParameter(4, 220.0);
            assertEquals(java.util.List.of(220.0), reloadedSaves);
            assertEquals(java.util.List.of(470.0, 100.0), savedOverrides);
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(system::tick);
        }
    }

    @Test
    void rejectedNativeConstraintLeavesOverrideAndWorldUsable() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            DeviceComponent component = new DeviceComponent("test:device");
            ElectricalDeviceSystem.bindDevice(component, compiled(4, 100.0, 6), system, () -> {
            });
            component.setParameter(4, 470.0);

            assertThrows(IllegalArgumentException.class, () -> component.setParameter(4, 0.0));
            assertEquals(470.0, component.overrides().getOrDefault(4, -1.0));
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(system::tick);
        }
    }

    @Test
    void deviceIdCodecRoundTripsExactIdentity() {
        DeviceId original = new DeviceId(17, -23);

        BsonValue encoded = ElectricalCodecs.DEVICE_ID.encode(original);
        DeviceId decoded = ElectricalCodecs.DEVICE_ID.decode(encoded);

        assertEquals(original, decoded);
    }

    @Test
    void componentSerializationRoundTripPreservesPersistentStateOnly() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            CompiledDeviceConfig compiled = compiled(4, 100.0, 6);
            Device resistance = system.create(Resistance.TYPE);
            DeviceComponent component = new DeviceComponent("test:device");
            component.setDeviceId(resistance.id());
            component.overrides().set(4, 470.0);
            component.bindRuntime(resistance, compiled, () -> {
            });

            BsonValue encoded = DeviceComponent.CODEC.encode(component);
            DeviceComponent decoded = DeviceComponent.CODEC.decode(encoded);

            assertEquals("test:device", decoded.getConfigId());
            assertEquals(resistance.id(), decoded.getDeviceId());
            assertEquals(1, decoded.overrides().size());
            assertEquals(4, decoded.overrides().stableIdAt(0));
            assertEquals(470.0, decoded.overrides().valueAt(0));
            assertNull(decoded.device());
            assertNull(decoded.compiledConfig());
        }
    }

    @Test
    void gameplayCloneKeepsConfigAndIndependentOverridesButClearsIdentityAndRuntime() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            CompiledDeviceConfig compiled = compiled(4, 100.0, 6);
            Device resistance = system.create(Resistance.TYPE);
            DeviceComponent component = new DeviceComponent("test:device");
            component.setDeviceId(resistance.id());
            component.overrides().set(4, 470.0);
            component.bindRuntime(resistance, compiled, () -> {
            });

            DeviceComponent cloned = component.clone();

            assertEquals("test:device", cloned.getConfigId());
            assertNull(cloned.getDeviceId());
            assertNull(cloned.device());
            assertNull(cloned.compiledConfig());
            assertEquals(470.0, cloned.overrides().getOrDefault(4, -1.0));

            cloned.overrides().set(4, 220.0);
            assertEquals(470.0, component.overrides().getOrDefault(4, -1.0));
        }
    }

    @Test
    void serializableClonePreservesIdentityAndIndependentOverridesButClearsRuntime() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            CompiledDeviceConfig compiled = compiled(4, 100.0, 6);
            Device resistance = system.create(Resistance.TYPE);
            DeviceComponent component = new DeviceComponent("test:device");
            component.setDeviceId(resistance.id());
            component.overrides().set(4, 470.0);
            component.bindRuntime(resistance, compiled, () -> {
            });

            DeviceComponent cloned = component.cloneSerializable();

            assertEquals("test:device", cloned.getConfigId());
            assertEquals(resistance.id(), cloned.getDeviceId());
            assertNull(cloned.device());
            assertNull(cloned.compiledConfig());
            assertEquals(470.0, cloned.overrides().getOrDefault(4, -1.0));

            cloned.overrides().set(4, 220.0);
            assertEquals(470.0, component.overrides().getOrDefault(4, -1.0));
        }
    }

    @Test
    void stableParameterApiMapsToNativeIndexAndRemovesRedundantDefaultOverride() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            CompiledDeviceConfig compiled = compiled(4, 100.0, 6);
            Device resistance = system.create(Resistance.TYPE);
            DeviceComponent component = new DeviceComponent("test:device");
            component.bindRuntime(resistance, compiled, () -> {
            });

            component.setParameter(4, 470.0);
            system.tick();

            assertTrue(component.overrides().contains(4));
            assertEquals(470.0, component.overrides().getOrDefault(4, -1.0));

            component.setParameter(4, 100.0);
            system.tick();

            assertFalse(component.overrides().contains(4));
        }
    }

    @Test
    void stableObserverApiMapsStableIdAndDoesNotRetainSubscription() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            CompiledDeviceConfig compiled = compiled(4, 100.0, 6);
            Device resistance = system.create(Resistance.TYPE);
            DeviceComponent component = new DeviceComponent("test:device");
            component.bindRuntime(resistance, compiled, () -> {
            });

            ObservationSubscription subscription = component.observe(6, (status, value) -> {
            });
            system.tick();

            assertTrue(subscription.isActive());
            assertSame(resistance, component.device());
        }
    }

    @Test
    void unknownStableIdsAndUnboundAccessFailBeforeNativeUse() {
        CompiledDeviceConfig compiled = compiled(4, 100.0, 6);
        DeviceComponent unbound = new DeviceComponent("test:device");

        assertThrows(IllegalStateException.class, () -> unbound.setParameter(4, 220.0));
        assertThrows(IllegalStateException.class, () -> unbound.observe(6, (status, value) -> {
        }));

        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            Device resistance = system.create(Resistance.TYPE);
            DeviceComponent component = new DeviceComponent("test:device");
            component.bindRuntime(resistance, compiled, () -> {
            });

            assertThrows(IllegalArgumentException.class, () -> component.setParameter(3, 220.0));
            assertThrows(IllegalArgumentException.class, () -> component.setParameter(4, Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> component.observe(5, (status, value) -> {
            }));
        }
    }

    private static CompiledDeviceConfig compiled(
            int stableParameterId,
            double defaultValue,
            int stableObserverId
    ) {
        int[] parameterMapping = new int[stableParameterId + 1];
        java.util.Arrays.fill(parameterMapping, MemberMapping.UNMAPPED);
        parameterMapping[stableParameterId] = 0;

        int[] observerMapping = new int[stableObserverId + 1];
        java.util.Arrays.fill(observerMapping, MemberMapping.UNMAPPED);
        observerMapping[stableObserverId] = 0;

        DeviceDescriptor descriptor = new DeviceDescriptor(
                "test:resistance",
                Resistance.TYPE,
                new MemberMapping(parameterMapping),
                new MemberMapping(0, 1),
                new MemberMapping(observerMapping)
        );

        return new CompiledDeviceConfig(
                descriptor,
                new int[]{stableParameterId},
                new int[]{0},
                new double[]{defaultValue},
                new int[0],
                new int[0],
                new int[0],
                BlockPortDefinition.of()
        );
    }
}
