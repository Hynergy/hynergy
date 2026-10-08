package dev.hynergy.electrical;

import dev.hynergy.electrical.primitives.passive.Resistance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ElectricalSystemDeviceResolutionTest {
    @Test void closedSystemAndRuntimeRejectResolutionAndRetainedHandleAccess() {
        var runtime = ElectricalRuntime.create();
        var system = runtime.createSystem(20);
        try {
            var original = Resistance.create(system, 10);
            var id = original.id();
            system.close(); runtime.close();
            assertThrows(IllegalStateException.class, () -> system.resolveDevice(id, Resistance.TYPE));
            assertThrows(IllegalStateException.class, () -> original.device().setParameter(Resistance.RESISTANCE, 20));
            assertEquals(1, Resistance.TYPE.parameterCount());
        } finally { system.close(); runtime.close(); }
    }
    @Test
    void resolutionCannotSelectDifferentConstraintsForAnExistingDevice() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            Resistance original = Resistance.create(system, 10.0);
            assertThrows(IllegalArgumentException.class,
                    () -> system.resolveDevice(original.id(),
                            dev.hynergy.electrical.primitives.sources.VoltageSource.TYPE));
            system.tick();
            assertThrows(IllegalArgumentException.class,
                    () -> system.resolveDevice(original.id(),
                            dev.hynergy.electrical.primitives.sources.VoltageSource.TYPE));
            Device restored = system.resolveDevice(original.id(), Resistance.TYPE);
            assertThrows(IllegalArgumentException.class, () -> restored.setParameter(Resistance.TYPE.parameter(0), 0.0));
            restored.setParameter(Resistance.TYPE.parameter(0), 20.0);
            assertDoesNotThrow(system::tick);
        }
    }


    @Test
    void resolvesExactIdentityWithoutAddingAnotherNativeDevice() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
            ElectricalSystem system = runtime.createSystem(20)) {
            Resistance original = Resistance.create(system, 10.0);
            DeviceId id = original.id();

            Device resolved = system.resolveDevice(id, Resistance.TYPE);

            assertNotSame(original, resolved);
            assertEquals(id, resolved.id());

            Resistance next = Resistance.create(system, 20.0);

            assertEquals(id.value() + 1, next.id().value());

            original.destroy();

            assertThrows(
                    IllegalStateException.class,
                    () -> resolved.setParameter(Resistance.TYPE.parameter(0), 30.0)
            );
        }
    }

    @Test
    void staleGenerationCannotResolveReusedDeviceId() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
            ElectricalSystem system = runtime.createSystem(20)) {
            Resistance original = Resistance.create(system, 10.0);
            DeviceId stale = original.id();

            system.tick();

            original.destroy();
            system.tick();

            Resistance replacement = Resistance.create(system, 20.0);
            DeviceId current = replacement.id();

            assertEquals(stale.value(), current.value());
            assertNotEquals(stale.generation(), current.generation());

            assertThrows(
                    IllegalStateException.class,
                    () -> system.resolveDevice(stale, Resistance.TYPE)
            );

            Device resolved = assertDoesNotThrow(
                    () -> system.resolveDevice(current, Resistance.TYPE)
            );

            assertEquals(current, resolved.id());
        }
    }
}
