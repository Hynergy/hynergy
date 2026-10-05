package dev.hynergy.electrical;

import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceHandleTest {
    @Test
    void primitiveDefinitionsCreateHandlesWithoutConvenienceWrappers() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            Device handle = system.create(PrimitiveDeviceTypes.RESISTANCE);
            assertSame(PrimitiveDeviceTypes.RESISTANCE, Resistance.TYPE);
            assertEquals(Device.class, handle.getClass());
            handle.requireDefinition(PrimitiveDeviceTypes.RESISTANCE);
            handle.setParameter(Resistance.TYPE.parameter(0), 20.0);
            assertThrows(IllegalArgumentException.class,
                    () -> handle.requireDefinition(PrimitiveDeviceTypes.VOLTAGE_SOURCE));
            handle.destroy();
            assertThrows(IllegalStateException.class, () -> new Resistance(handle));
        }
    }

    @Test
    void customDefinitionCreatesIndependentHandlesWithoutJavaConstructors() {
        DeviceType type = DeviceType.define(builder -> {
            var positive = builder.terminal(0, "positive");
            var negative = builder.terminal(1, "negative");
            var parameter = builder.parameter(0, "resistance", new ParameterConstraints(
                ParameterConstraints.Bound.inclusive(10.0), ParameterConstraints.Bound.exclusive(100.0), true, false, null, null));
            builder.element(Resistance.TYPE, e -> {
                e.connect(Resistance.TYPE.terminal(0), positive);
                e.connect(Resistance.TYPE.terminal(1), negative);
                e.bind(Resistance.TYPE.parameter(0), parameter);
            });
        });
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            RegisteredDeviceType definition = runtime.register(type);
            assertSame(definition, runtime.register(type));
            try (ElectricalSystem system = runtime.createSystem(20)) {
                Device first = system.create(type);
                Device second = system.create(type);
                assertEquals(Device.class, first.getClass());
                assertNotSame(first, second);
                assertEquals(first.id().value() + 1, second.id().value());
                first.setParameter(type.parameter(0), 10.0);
                second.setParameter(type.parameter(0), 20.0);
                assertThrows(IllegalArgumentException.class, () -> first.setParameter(type.parameter(0), 9.0));
                assertThrows(IllegalArgumentException.class, () -> second.setParameter(type.parameter(0), 100.0));
                assertDoesNotThrow(system::tick);
            }
        }
    }

    @Test
    void compositionUsesTheSuppliedHandleAndChecksItsDefinition() {
        try (ElectricalRuntime runtime = ElectricalRuntime.create();
             ElectricalSystem system = runtime.createSystem(20)) {
            Device handle = system.create(Resistance.TYPE);
            Resistance resistance = new Resistance(handle);
            assertSame(handle, resistance.device());
            assertEquals(handle.id(), resistance.id());
            Device sourceHandle = system.create(VoltageSource.TYPE);
            assertThrows(IllegalArgumentException.class, () -> new Resistance(sourceHandle));
            VoltageSource source = new VoltageSource(sourceHandle);
            source.setVoltage(5.0);
            resistance.setResistance(10.0);
            Wire positive = system.createWire();
            Wire negative = system.createWire();
            source.attachPositive(positive);
            source.attachNegative(negative);
            resistance.attachPositive(positive);
            resistance.attachNegative(negative);
            ArrayList<Double> currents = new ArrayList<>();
            ObservationSubscription subscription = resistance.observeCurrent((status, value) -> currents.add(value));
            system.tick();
            assertEquals(0.5, currents.getLast(), 1e-9);
            handle.validateParameter(Resistance.TYPE.parameter(0), 20.0);
            system.tick();
            assertEquals(0.5, currents.getLast(), 1e-9);
            handle.setParameter(Resistance.TYPE.parameter(0), 20.0);
            system.tick();
            assertEquals(0.25, currents.getLast(), 1e-9);
            resistance.destroy();
            assertFalse(subscription.isActive());
            assertThrows(IllegalStateException.class, () -> handle.setParameter(Resistance.TYPE.parameter(0), 30.0));
        }
    }
}
