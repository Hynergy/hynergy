package dev.hynergy.electrical;

import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

final class PrimitiveObservationTest {
    private static final ObservationListener NOOP_LISTENER = (status, value) -> {
    };

    @Test
    void semanticResistanceObserversPublishExpectedValues() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
            ElectricalSystem system = runtime.createSystem(20)) {
            VoltageSource source = VoltageSource.create(system, 5.0);

            Resistance resistance = Resistance.create(system, 10.0);

            connectSourceAndResistance(system, source, resistance);

            ArrayList<ObservationStatus> voltageStatuses = new ArrayList<>();

            ArrayList<Double> voltages = new ArrayList<>();

            ArrayList<ObservationStatus> currentStatuses = new ArrayList<>();

            ArrayList<Double> currents = new ArrayList<>();

            resistance.observeVoltage((status, value) -> {
                voltageStatuses.add(status);
                voltages.add(value);
            });

            resistance.observeCurrent((status, value) -> {
                currentStatuses.add(status);
                currents.add(value);
            });

            system.tick();

            assertEquals(1, voltageStatuses.size());
            assertEquals(ObservationStatus.AVAILABLE, voltageStatuses.getFirst());
            assertEquals(5.0, voltages.getFirst(), 1e-9);

            assertEquals(1, currentStatuses.size());
            assertEquals(ObservationStatus.AVAILABLE, currentStatuses.getFirst());
            assertEquals(0.5, currents.getFirst(), 1e-9);

            system.tick();

            assertEquals(1, voltages.size());
            assertEquals(1, currents.size());

            source.setVoltage(7.0);

            system.tick();

            assertEquals(2, voltages.size());
            assertEquals(7.0, voltages.get(1), 1e-9);

            assertEquals(2, currents.size());
            assertEquals(0.7, currents.get(1), 1e-9);
        }
    }

    @Test
    void semanticObserverRejectsNullListener() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
            ElectricalSystem system = runtime.createSystem(20)) {
            Resistance resistance = new Resistance(system.create(Resistance.TYPE));

            assertThrows(NullPointerException.class, () -> resistance.observeVoltage(null));
            ObservationSubscription subscription = assertDoesNotThrow(() -> resistance.observeVoltage(NOOP_LISTENER));

            assertTrue(subscription.isActive());
        }
    }

    private static void connectSourceAndResistance(
        ElectricalSystem system,
        VoltageSource source,
        Resistance resistance
    ) {
        Wire positive = system.createWire();
        Wire negative = system.createWire();

        source.attachPositive(positive);
        source.attachNegative(negative);

        resistance.attachPositive(positive);
        resistance.attachNegative(negative);
    }
}
