package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

final class DeviceTypeMetadataTest {
    @Test void primitiveDeclaredConstraintsMatchNativeParameterValidation() {
        try (var runtime = ElectricalRuntime.create()) {
            for (var type : PrimitiveDeviceTypes.ALL) {
                var binding = runtime.register(type);
                for (var parameter : type.parameters()) {
                    for (double value : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                            -Double.MAX_VALUE, -1, -0.0, 0, Double.MIN_VALUE, 1, Double.MAX_VALUE}) {
                        boolean declaredAccepts;
                        try { parameter.constraints().validate(value); declaredAccepts = true; }
                        catch (IllegalArgumentException failure) { declaredAccepts = false; }
                        boolean nativeAccepts;
                        try { runtime.validateParameter(binding.definition(), binding.parameterIndex(parameter), value); nativeAccepts = true; }
                        catch (IllegalArgumentException failure) { nativeAccepts = false; }
                        assertEquals(nativeAccepts, declaredAccepts, "Primitive " + type.primitiveId() + " parameter " + parameter.name() + " value " + value);
                    }
                }
            }
        }
    }
    @Test
    void primitiveCatalogMatchesNativeTerminalBoundaries() {
        double[][] values = {
                {10.0}, {0.1}, {5.0}, {0.1}, {1.0}, {1.0}, {0.001}, {0.1},
                {2.0, 1.0, 0.0}, {2.0, 1.0, 0.0, 1.0}, {}, {1.0, 0.0},
                {2.0, 1.0, 0.0}, {2.0, 1.0, 0.0}, {2.0, 1.0, 0.0},
                {2.0, 1.0, 0.0}, {2.0, 1.0, 0.0}, {1.0, 2.0, 1.0, 0.0}
        };
        try (ElectricalEngine engine = ElectricalEngine.create()) {
            for (int definitionId = 1; definitionId <= 18; definitionId++) {
                DeviceType type = PrimitiveDeviceTypes.ALL.get(definitionId - 1);
                try (ElectricalWorld world = engine.createWorld(20)) {
                    int deviceId = world.addDevice(new DeviceDefinition(definitionId));
                    int generation = world.deviceGeneration(deviceId);
                    double[] parameters = values[definitionId - 1];
                    for (int index = 0; index < parameters.length; index++) {
                        world.setDeviceParameter(deviceId, generation, index, parameters[index]);
                    }
                    int wireId = world.addWire();
                    WireId wire = new WireId(wireId, world.wireGeneration(wireId));
                    world.attachTerminal(wire, deviceId, generation, type.terminalCount() - 1);
                    assertDoesNotThrow(world::applyCommands);
                    world.attachTerminal(wire, deviceId, generation, type.terminalCount());
                    assertThrows(IllegalStateException.class, world::applyCommands);
                }
            }
        }
    }

    @Test
    void primitiveCatalogMatchesNativeParameterAndObserverBoundaries() {
        try (ElectricalEngine engine = ElectricalEngine.create();
             ElectricalWorld world = engine.createWorld(20)) {
            for (int definitionId = 1; definitionId <= 18; definitionId++) {
                DeviceType type = PrimitiveDeviceTypes.ALL.get(definitionId - 1);
                DeviceDefinition definition = new DeviceDefinition(definitionId);
                if (type.parameterCount() > 0) {
                    assertDoesNotThrow(() -> engine.validateParameter(definition, type.parameterCount() - 1, 1.0));
                }
                assertThrows(IllegalArgumentException.class,
                        () -> engine.validateParameter(definition, type.parameterCount(), 1.0));
                int deviceId = world.addDevice(definition);
                int generation = world.deviceGeneration(deviceId);
                int subscription = world.subscribeObserver(deviceId, generation, type.observerCount() - 1);
                assertThrows(ElectricalWorld.SubscriptionOperationException.class,
                        () -> world.subscribeObserver(deviceId, generation, type.observerCount()));
                world.unsubscribe(subscription);
            }
        }
    }

    @Test void compositeMetadataIsImmutableAcrossRuntimes() {
        var builds = new AtomicInteger();
        var type = DeviceType.define(b -> {
            builds.incrementAndGet();
            var terminal = b.terminal(0, "positive"); var ground = b.ground();
            var parameter = b.parameter(0, "resistance", new ParameterConstraints(
                ParameterConstraints.Bound.inclusive(10), null, true, false, null, null));
            var child = b.element(PrimitiveDeviceTypes.RESISTANCE, e -> {
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(0), terminal);
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(1), ground);
                e.bind(PrimitiveDeviceTypes.RESISTANCE.parameter(0), parameter);
            });
            b.voltageObserver(0, "voltage", terminal, ground);
            b.childObserver(1, "current", child, PrimitiveDeviceTypes.RESISTANCE.observer(1));
        });
        assertEquals(1, type.parameterCount()); assertEquals(1, type.terminalCount()); assertEquals(2, type.observerCount());
        RegisteredDeviceType old;
        try (var runtime = ElectricalRuntime.create()) {
            old = runtime.register(type);
            assertSame(old, runtime.register(type));
            assertThrows(IllegalArgumentException.class, () -> old.validateParameter(type.parameter(0), 9));
            old.validateParameter(type.parameter(0), 10);
        }
        assertThrows(IllegalStateException.class, () -> old.validateParameter(type.parameter(0), 10));
        assertEquals(1, type.terminalCount());
        try (var runtime = ElectricalRuntime.create()) {
            runtime.register(type).validateParameter(type.parameter(0), 10);
            assertEquals(1, builds.get());
        }
    }
    @Test void primitiveMetadataAndConstraintsExistWithoutRegistration() {
        var type = PrimitiveDeviceTypes.RESISTANCE;
        assertEquals(2, type.terminalCount()); assertEquals(1, type.parameterCount()); assertEquals(2, type.observerCount());
        assertThrows(IllegalArgumentException.class, () -> type.parameter(0).constraints().validate(0));
        type.parameter(0).constraints().validate(10);
    }
}
