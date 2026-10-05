package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

final class DeviceTypeMetadataTest {
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
                DeviceType type = DeviceType.primitive(definitionId);
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
                DeviceType type = DeviceType.primitive(definitionId);
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

    @Test
    void registrationCapturesOnlyExternalMembersAndExecutesBuilderOnce() {
        AtomicInteger builds = new AtomicInteger();
        DeviceType type = DeviceType.create(builder -> {
            builds.incrementAndGet();
            int terminal = builder.addTerminal();
            int ground = builder.addGroundNode();
            int parameter = builder.addParameter(DeviceDefinitionBuilder.Bound.inclusive(10.0), null, true);
            builder.beginElement(PrimitiveDeviceTypes.RESISTANCE)
                    .elementTerminal(terminal).elementTerminal(ground)
                    .elementParameter(parameter).endElement();
            builder.addVoltageObserver(terminal, ground);
            builder.addChildObserver(0, 1);
        });
        assertThrows(IllegalStateException.class, type::metadata);
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            runtime.register(type);
            assertEquals(1, builds.get());
            assertEquals(1, type.terminalCount());
            assertEquals(1, type.parameterCount());
            assertEquals(2, type.observerCount());
            assertThrows(IllegalArgumentException.class, () -> type.validateParameter(0, 9.0));
            assertDoesNotThrow(() -> type.validateParameter(0, 10.0));
            try (ElectricalSystem system = runtime.createSystem(20)) {
                Device device = system.create(type);
                Wire wire = system.createWire();
                assertThrows(IllegalArgumentException.class, () -> device.attachTerminal(1, wire));
                assertThrows(IllegalStateException.class, () -> device.observe(2, (status, value) -> {}));
                device.setParameter(0, 10.0);
                device.attachTerminal(0, wire);
                assertDoesNotThrow(system::tick);
            }
        }
        assertThrows(IllegalStateException.class, type::metadata);
        assertThrows(IllegalStateException.class, () -> type.validateParameter(0, 10.0));
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            assertEquals(2, builds.get());
            assertEquals(1, type.terminalCount());
        }
    }

    @Test
    void primitiveMetadataRetainsItsShapeAcrossRuntimeLifetimes() {
        DeviceType type = PrimitiveDeviceTypes.RESISTANCE;
        assertEquals(2, type.terminalCount());
        assertEquals(1, type.parameterCount());
        assertEquals(2, type.observerCount());
        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            assertThrows(IllegalArgumentException.class, () -> type.validateParameter(0, 0.0));
            assertDoesNotThrow(() -> type.validateParameter(0, 10.0));
        }
        assertEquals(2, type.terminalCount());
        assertThrows(IllegalStateException.class, () -> type.validateParameter(0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.primitive(999));
    }
}
