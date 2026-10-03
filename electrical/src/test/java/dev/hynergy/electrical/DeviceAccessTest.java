package dev.hynergy.electrical;

import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceAccessTest {
    @Test
    void customDefinitionBoundsAreValidatedBeforeItsDeviceIsApplied() {
        DeviceType<TestDevice> type = DeviceType.create(TestDevice::new, builder -> {
            int positive = builder.addTerminal();
            int negative = builder.addTerminal();
            int parameter = builder.addParameter(DeviceDefinitionBuilder.Bound.inclusive(10.0),
                    DeviceDefinitionBuilder.Bound.exclusive(100.0), true);
            builder.beginElement(Resistance.TYPE)
                    .elementTerminal(positive).elementTerminal(negative)
                    .elementParameter(parameter).endElement();
        });
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            try (ElectricalSystem system = runtime.createSystem(20)) {
                TestDevice device = system.create(type);
                DeviceAccess.setParameter(device, 0, 10.0);
                assertThrows(IllegalArgumentException.class, () -> DeviceAccess.setParameter(device, 0, 9.0));
                assertThrows(IllegalArgumentException.class, () -> DeviceAccess.setParameter(device, 0, 100.0));
                assertDoesNotThrow(system::tick);
            }
        }
    }

    @Test
    void invalidParameterIsRejectedBeforeEnqueueWithoutDiscardingValidCommands() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            VoltageSource source = VoltageSource.create(system, 5.0);
            Resistance resistance = Resistance.create(system, 10.0);
            Wire positive = system.createWire();
            Wire negative = system.createWire();
            source.attachPositive(positive);
            source.attachNegative(negative);
            resistance.attachPositive(positive);
            resistance.attachNegative(negative);
            ArrayList<Double> currents = new ArrayList<>();
            resistance.observeCurrent((status, value) -> currents.add(value));

            assertThrows(IllegalArgumentException.class, () -> resistance.setResistance(0.0));
            assertThrows(IllegalArgumentException.class, () -> DeviceAccess.setParameter(resistance, 1, 10.0));
            assertDoesNotThrow(system::tick);
            assertEquals(0.5, currents.getLast(), 1e-9);

            resistance.setResistance(20.0);
            assertThrows(IllegalArgumentException.class, () -> resistance.setResistance(-1.0));
            assertDoesNotThrow(system::tick);
            assertEquals(0.25, currents.getLast(), 1e-9);
        }
    }


    @Test
    void rawParameterTerminalAndObserverAccessUseNativeIndexes() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
            ElectricalSystem system = runtime.createSystem(20)) {
            VoltageSource source = VoltageSource.create(system, 5.0);
            Resistance resistance = system.create(Resistance.TYPE);

            DeviceAccess.setParameter(resistance, 0, 10.0);

            Wire positive = system.createWire();
            Wire negative = system.createWire();

            source.attachPositive(positive);
            source.attachNegative(negative);

            DeviceAccess.attachTerminal(resistance, 0, positive);
            DeviceAccess.attachTerminal(resistance, 1, negative);

            ArrayList<ObservationStatus> statuses = new ArrayList<>();
            ArrayList<Double> currents = new ArrayList<>();

            ObservationSubscription subscription = DeviceAccess.observe(
                    resistance,
                    1,
                    (status, value) -> {
                        statuses.add(status);
                        currents.add(value);
                    }
            );

            assertTrue(subscription.isActive());

            system.tick();

            assertEquals(1, statuses.size());
            assertEquals(ObservationStatus.AVAILABLE, statuses.getFirst());
            assertEquals(0.5, currents.getFirst(), 1e-9);

            DeviceAccess.detachTerminal(resistance, 1, negative);
            DeviceAccess.attachTerminal(resistance, 1, negative);

            assertDoesNotThrow(system::tick);
        }
    }

    @Test
    void rawAccessPreservesOwnershipAndLivenessChecks() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
            ElectricalSystem first = runtime.createSystem(20);
            ElectricalSystem second = runtime.createSystem(20)) {
            Resistance resistance = Resistance.create(first, 10.0);
            Wire foreignWire = second.createWire();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> DeviceAccess.attachTerminal(resistance, 0, foreignWire)
            );

            resistance.destroy();

            assertThrows(
                    IllegalStateException.class,
                    () -> DeviceAccess.setParameter(resistance, 0, 20.0)
            );
        }
    }

    @Test
    void rawAccessRejectsUnboundDeviceThroughNormalDevicePath() {
        TestDevice device = new TestDevice();

        assertThrows(
                IllegalStateException.class,
                () -> DeviceAccess.setParameter(device, 0, 1.0)
        );
    }

    private static final class TestDevice extends Device {
    }
}
