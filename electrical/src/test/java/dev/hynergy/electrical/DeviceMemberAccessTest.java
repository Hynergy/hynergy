package dev.hynergy.electrical;

import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceMemberAccessTest {
    @Test void foreignMembersWithMatchingStableIdsCannotChangeLiveCircuit() {
        var other = DeviceType.define(b -> {
            var p = b.terminal(0, "positive"); var n = b.terminal(1, "negative");
            b.parameter(0, "resistance", ParameterConstraints.positiveFinite());
            b.voltageObserver(1, "current", p, n);
        });
        try (var runtime = ElectricalRuntime.create(); var system = runtime.createSystem(20)) {
            var source = VoltageSource.create(system, 5);
            var resistor = Resistance.create(system, 10);
            var p = system.createWire(); var n = system.createWire();
            source.attachPositive(p); source.attachNegative(n); resistor.attachPositive(p); resistor.attachNegative(n);
            var currents = new ArrayList<Double>();
            var subscription = resistor.observeCurrent((status, value) -> currents.add(value));
            system.tick(); assertEquals(0.5, currents.getLast(), 1e-9);
            var device = resistor.device();
            assertThrows(IllegalArgumentException.class, () -> device.setParameter(other.parameter(0), 20));
            assertThrows(IllegalArgumentException.class, () -> device.validateParameter(other.parameter(0), 20));
            assertThrows(IllegalArgumentException.class, () -> device.detachTerminal(other.terminal(1), n));
            assertThrows(IllegalArgumentException.class, () -> device.attachTerminal(other.terminal(0), n));
            assertThrows(IllegalArgumentException.class, () -> device.observe(other.observer(1), (status, value) -> fail()));
            system.tick(); assertEquals(0.5, currents.getLast(), 1e-9);
            assertTrue(subscription.isActive());
        }
    }

    @Test
    void invalidParameterIsRejectedBeforeEnqueueWithoutDiscardingValidCommands() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            VoltageSource source = VoltageSource.create(system, 5.0);
            Device resistance = Resistance.create(system, 10.0).device();
            Wire positive = system.createWire();
            Wire negative = system.createWire();
            source.attachPositive(positive);
            source.attachNegative(negative);
            resistance.attachTerminal(Resistance.TYPE.terminal(0), positive);
            resistance.attachTerminal(Resistance.TYPE.terminal(1), negative);
            ArrayList<Double> currents = new ArrayList<>();
            resistance.observe(Resistance.TYPE.observer(1), (status, value) -> currents.add(value));

            assertThrows(IllegalArgumentException.class, () -> resistance.setParameter(Resistance.TYPE.parameter(0), 0.0));
            assertThrows(IllegalArgumentException.class, () -> resistance.setParameter(Resistance.TYPE.parameter(1), 10.0));
            assertDoesNotThrow(system::tick);
            assertEquals(0.5, currents.getLast(), 1e-9);

            resistance.setParameter(Resistance.TYPE.parameter(0), 20.0);
            assertThrows(IllegalArgumentException.class, () -> resistance.setParameter(Resistance.TYPE.parameter(0), -1.0));
            assertDoesNotThrow(system::tick);
            assertEquals(0.25, currents.getLast(), 1e-9);
        }
    }


    @Test
    void rawParameterTerminalAndObserverAccessUseNativeIndexes() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
            ElectricalSystem system = runtime.createSystem(20)) {
            VoltageSource source = VoltageSource.create(system, 5.0);
            Device resistance = system.create(Resistance.TYPE);

            resistance.setParameter(Resistance.TYPE.parameter(0), 10.0);

            Wire positive = system.createWire();
            Wire negative = system.createWire();

            source.attachPositive(positive);
            source.attachNegative(negative);

            resistance.attachTerminal(Resistance.TYPE.terminal(0), positive);
            resistance.attachTerminal(Resistance.TYPE.terminal(1), negative);

            ArrayList<ObservationStatus> statuses = new ArrayList<>();
            ArrayList<Double> currents = new ArrayList<>();

            ObservationSubscription subscription = resistance.observe(Resistance.TYPE.observer(1),
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

            resistance.detachTerminal(Resistance.TYPE.terminal(1), negative);
            resistance.attachTerminal(Resistance.TYPE.terminal(1), negative);

            assertDoesNotThrow(system::tick);
        }
    }

    @Test
    void rawAccessPreservesOwnershipAndLivenessChecks() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
            ElectricalSystem first = runtime.createSystem(20);
            ElectricalSystem second = runtime.createSystem(20)) {
            Device resistance = Resistance.create(first, 10.0).device();
            Wire foreignWire = second.createWire();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> resistance.attachTerminal(Resistance.TYPE.terminal(0), foreignWire)
            );

            resistance.destroy();

            assertThrows(
                    IllegalStateException.class,
                    () -> resistance.setParameter(Resistance.TYPE.parameter(0), 20.0)
            );
        }
    }

    @Test
    void rawAccessRejectsUnboundDeviceThroughNormalDevicePath() {
        Device device = new Device();

        assertThrows(
                IllegalStateException.class,
                () -> device.setParameter(Resistance.RESISTANCE, 1.0)
        );
    }

}
