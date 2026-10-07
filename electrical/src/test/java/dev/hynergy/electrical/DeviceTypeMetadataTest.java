package dev.hynergy.electrical;

import dev.hynergy.electrical.primitives.controlled.VoltageControlledConductance;
import dev.hynergy.electrical.primitives.controlled.VoltageControlledSwitch;
import dev.hynergy.electrical.primitives.logic.*;
import dev.hynergy.electrical.primitives.passive.*;
import dev.hynergy.electrical.primitives.sources.CurrentSource;
import dev.hynergy.electrical.primitives.sources.VoltageControlledCurrentSource;
import dev.hynergy.electrical.primitives.sources.VoltageControlledVoltageSource;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceTypeMetadataTest {
    @Test
    void primitiveDeclaredConstraintsMatchNativeParameterValidation() {
        try (var runtime = ElectricalRuntime.create()) {
            for (var type : PrimitiveDeviceTypes.ALL) {
                var binding = runtime.register(type);
                for (var parameter : type.parameters()) {
                    for (double value : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                            -Double.MAX_VALUE, -1, -0.0, 0, Double.MIN_VALUE, 1, Double.MAX_VALUE}) {
                        boolean declaredAccepts;
                        try {
                            parameter.constraints().validate(value);
                            declaredAccepts = true;
                        } catch (IllegalArgumentException failure) {
                            declaredAccepts = false;
                        }
                        boolean nativeAccepts;
                        try {
                            runtime.validateParameter(binding.definition(), binding.parameterIndex(parameter), value);
                            nativeAccepts = true;
                        } catch (IllegalArgumentException failure) {
                            nativeAccepts = false;
                        }
                        assertEquals(nativeAccepts, declaredAccepts, "Primitive " + type.primitiveId() + " parameter " + parameter.name() + " value " + value);
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "primitive {0}: native identity and member boundaries")
    @MethodSource("primitiveDefinitions")
    void primitiveCatalogMatchesNativeIdentityAndMemberBoundaries(int definitionId, DeviceType type, double[] values) {
        try (var runtime = ElectricalRuntime.create()) {
            var binding = runtime.register(type);
            assertEquals(definitionId, binding.definition().id());
            assertSame(binding, runtime.register(type));
        }
        try (ElectricalEngine engine = ElectricalEngine.create();
             ElectricalWorld world = engine.createWorld(20)) {
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

            for (int index = 0; index < values.length; index++) {
                world.setDeviceParameter(deviceId, generation, index, values[index]);
            }
            int wireId = world.addWire();
            WireId wire = new WireId(wireId, world.wireGeneration(wireId));
            world.attachTerminal(wire, deviceId, generation, type.terminalCount() - 1);
            assertDoesNotThrow(world::applyCommands);
            world.attachTerminal(wire, deviceId, generation, type.terminalCount());
            assertThrows(IllegalStateException.class, world::applyCommands);
        }
    }

    private static Stream<Arguments> primitiveDefinitions() {
        return Stream.of(
                Arguments.of(1, Resistance.TYPE, new double[]{10}),
                Arguments.of(2, Conductance.TYPE, new double[]{0.1}),
                Arguments.of(3, VoltageSource.TYPE, new double[]{5}),
                Arguments.of(4, CurrentSource.TYPE, new double[]{0.1}),
                Arguments.of(5, VoltageControlledCurrentSource.TYPE, new double[]{1}),
                Arguments.of(6, VoltageControlledVoltageSource.TYPE, new double[]{1}),
                Arguments.of(7, Capacitor.TYPE, new double[]{0.001}),
                Arguments.of(8, Inductor.TYPE, new double[]{0.1}),
                Arguments.of(9, VoltageControlledSwitch.TYPE, new double[]{2, 1, 0}),
                Arguments.of(10, VoltageControlledConductance.TYPE, new double[]{2, 1, 0, 1}),
                Arguments.of(11, TickDelay.TYPE, new double[]{}),
                Arguments.of(12, Diode.TYPE, new double[]{1, 0}),
                Arguments.of(13, Not.TYPE, new double[]{2, 1, 0}),
                Arguments.of(14, And.TYPE, new double[]{2, 1, 0}),
                Arguments.of(15, Nand.TYPE, new double[]{2, 1, 0}),
                Arguments.of(16, Or.TYPE, new double[]{2, 1, 0}),
                Arguments.of(17, Nor.TYPE, new double[]{2, 1, 0}),
                Arguments.of(18, SchmittBuffer.TYPE, new double[]{1, 2, 1, 0})
        );
    }

    @Test
    void compositeMetadataIsImmutableAcrossRuntimes() {
        var builds = new AtomicInteger();
        var type = DeviceType.define(b -> {
            builds.incrementAndGet();
            var terminal = b.terminal(0, "positive");
            var ground = b.ground();
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
        assertEquals(1, type.parameterCount());
        assertEquals(1, type.terminalCount());
        assertEquals(2, type.observerCount());
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
            var binding = runtime.register(type);
            assertNotSame(old, binding);
            binding.validateParameter(type.parameter(0), 10);
            assertEquals(1, builds.get());
            try (var system = runtime.createSystem(20)) {
                assertSame(binding, system.create(type).binding());
            }
        }
    }

    @Test
    void primitiveMetadataAndConstraintsExistWithoutRegistration() {
        var type = PrimitiveDeviceTypes.RESISTANCE;
        assertEquals(2, type.terminalCount());
        assertEquals(1, type.parameterCount());
        assertEquals(2, type.observerCount());
        assertThrows(IllegalArgumentException.class, () -> type.parameter(0).constraints().validate(0));
        type.parameter(0).constraints().validate(10);
    }
}
