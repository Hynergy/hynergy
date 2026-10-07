package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.ObservationStatus;
import dev.hynergy.electrical.composite.GroundedSwitchedLogicGates;
import dev.hynergy.electrical.composite.ResistiveLoad;
import dev.hynergy.electrical.composite.VoltageSupply;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class GroundedSwitchedLogicGateAssetTest {
    @ParameterizedTest
    @CsvSource({
            "And, 8, 5, Switched", "Or, 14, 5, Switched", "Nand, 7, 5, Switched",
            "Nor, 1, 5, Switched", "Not, 1, 5, Switched",
            "And, 8, 9, Switched", "Or, 14, 9, Switched", "Nand, 7, 9, Switched",
            "Nor, 1, 9, Switched", "Not, 1, 9, Switched",
            "And, 8, 5, ''", "Or, 14, 5, ''", "Nand, 7, 5, ''",
            "Nor, 1, 5, ''", "Not, 1, 5, ''"
    })
    void configuredGateDrivesLampWithoutExternalSupply(
            String asset, int truthTable, double voltage, String configPrefix
    ) throws IOException {
        var type = switch (asset) {
            case "And" -> GroundedSwitchedLogicGates.GROUNDED_SWITCHED_AND;
            case "Or" -> GroundedSwitchedLogicGates.GROUNDED_SWITCHED_OR;
            case "Nand" -> GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NAND;
            case "Nor" -> GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOR;
            case "Not" -> GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOT;
            default -> throw new IllegalArgumentException(asset);
        };
        int inputCount = asset.equals("Not") ? 1 : 2;
        try (var runtime = ElectricalRuntime.create()) {
            var registry = new DeviceRegistry(runtime);
            registry.register("hynergy:grounded_switched_" + asset.toLowerCase(java.util.Locale.ROOT), type);
            registry.register("hynergy:voltage_supply", VoltageSupply.TYPE);
            registry.register("hynergy:resistive_load", ResistiveLoad.RESISTIVE_LOAD);
            var ports = new PortModule();
            var domain = ports.<ElectricalPortConnection>registerDomain("test:electrical");
            var standard = ports.registerStandard("test:conductor", domain, ElectricalPortProfile.class,
                    (first, second, geometry) -> ElectricalPortConnection.DIRECT);
            var codec = DeviceConfig.createCodec(registry);
            var gateAsset = readJson("Server/Hynergy/Electricity/Devices/" + configPrefix + asset + ".json");
            assertEquals(inputCount + 1, gateAsset.getArray("Ports").size());
            assertEquals(inputCount + 1, type.terminalCount());
            var supplyParameter = gateAsset.getArray("Parameters").stream()
                                           .map(value -> value.asDocument())
                                           .filter(parameter -> parameter.getInt32("Id").getValue() == 3)
                                           .findFirst().orElseThrow();
            assertEquals(5.0, supplyParameter.getNumber("Default").doubleValue());
            supplyParameter.put("Default", new BsonDouble(voltage));
            var gateConfig = codec.decode(gateAsset).compile(registry, standard);
            var supplyConfig = codec.decode(readJson("Server/Hynergy/Electricity/Devices/PowerSupply.json"))
                                    .compile(registry, standard);
            var lampConfig = codec.decode(readJson("Server/Hynergy/Electricity/Devices/Lightbulb.json"))
                                  .compile(registry, standard);
            try (var system = runtime.createSystem(20)) {
                var gate = new DeviceComponent(asset);
                var lamp = new DeviceComponent("Lightbulb");
                ElectricalDeviceSystem.bindDevice(gate, gateConfig, system, () -> {
                });
                ElectricalDeviceSystem.bindDevice(lamp, lampConfig, system, () -> {
                });
                var sources = new ArrayList<DeviceComponent>();
                for (int terminal : inputCount == 1 ? new int[]{0, 2} : new int[]{0, 2, 3}) {
                    var wire = system.createWire();
                    assertTrue(DeviceWireConnections.attachPortIfNew(gate, terminal, wire,
                            new DeviceWireConnections.AttachmentDedup()));
                    var peer = terminal == 0 ? lamp : new DeviceComponent("PowerSupply");
                    if (terminal != 0) {
                        ElectricalDeviceSystem.bindDevice(peer, supplyConfig, system, () -> {
                        });
                        sources.add(peer);
                    }
                    assertTrue(DeviceWireConnections.attachPortIfNew(peer, 0, wire,
                            new DeviceWireConnections.AttachmentDedup()));
                }
                var statuses = new ArrayList<ObservationStatus>();
                var currents = new ArrayList<Double>();
                lamp.observe(1, (status, value) -> {
                    statuses.add(status);
                    currents.add(value);
                });
                for (int inputs : inputCount == 1 ? new int[]{0, 1, 0} : new int[]{0, 1, 2, 3, 2, 1, 0}) {
                    sources.get(0).device().setParameter(VoltageSupply.TYPE.parameter(0), (inputs & 1) == 0 ? 0 : 5);
                    if (inputCount == 2) {
                        sources.get(1)
                               .device()
                               .setParameter(VoltageSupply.TYPE.parameter(0), (inputs & 2) == 0 ? 0 : 5);
                    }
                    system.tick();
                    assertFalse(statuses.isEmpty(), "Lamp must publish a result for inputs " + inputs);
                    assertEquals(ObservationStatus.AVAILABLE, statuses.getLast(), "Inputs " + inputs);
                    assertEquals((truthTable & (1 << inputs)) != 0 ? voltage / 101.0001 : 0.0, currents.getLast(), 1e-8,
                            "Inputs " + inputs);
                }
            }
        }
    }

    @Test
    void switchedItemsUseNewConfigsAndRestoreRetainedConnections() throws IOException {
        for (var name : new String[]{"Not", "And", "Nand", "Or", "Nor"}) {
            var item = readJson("Server/Item/Items/Hynergy/Electricity/Logic/Hynergy_Switched" + name + ".json");
            assertEquals("Switched" + name, item.getDocument("BlockType")
                                                .getDocument("BlockEntity")
                                                .getDocument("Components")
                                                .getDocument("HynergyDevice")
                                                .getString("Config")
                                                .getValue());
            var config = readJson("Server/Hynergy/Electricity/Devices/Switched" + name + ".json");
            assertEquals(6, config.getArray("Parameters").size());
            assertEquals(0, config.getArray("Parameters").get(2).asDocument().getNumber("Default").doubleValue());
            assertEquals(1e-6, config.getArray("Parameters").get(4).asDocument().getNumber("Default").doubleValue());
            assertEquals(1e-6, config.getArray("Parameters").get(5).asDocument().getNumber("Default").doubleValue());
        }
        try (var runtime = ElectricalRuntime.create()) {
            var registry = new DeviceRegistry(runtime);
            registry.register("hynergy:grounded_switched_and", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_AND);
            registry.register("hynergy:grounded_switched_not", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOT);
            registry.register("hynergy:voltage_supply", VoltageSupply.TYPE);
            var ports = new PortModule();
            var domain = ports.<ElectricalPortConnection>registerDomain("test:electrical");
            var standard = ports.registerStandard("test:conductor", domain, ElectricalPortProfile.class,
                    (first, second, geometry) -> ElectricalPortConnection.DIRECT);
            var codec = DeviceConfig.createCodec(registry);
            var compiled = codec.decode(readJson("Server/Hynergy/Electricity/Devices/SwitchedAnd.json"))
                                .compile(registry, standard);
            var wrong = codec.decode(readJson("Server/Hynergy/Electricity/Devices/SwitchedNot.json"))
                             .compile(registry, standard);
            try (var system = runtime.createSystem(20)) {
                var component = new DeviceComponent("SwitchedAnd");
                ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
                });
                var retained = component.device();
                var supply = system.create(VoltageSupply.TYPE);
                supply.setParameter(VoltageSupply.TYPE.parameter(0), 5);
                supply.setParameter(VoltageSupply.TYPE.parameter(1), 100);
                var wire = system.createWire();
                assertTrue(DeviceWireConnections.attachPortIfNew(component, 0, wire, new DeviceWireConnections.AttachmentDedup()));
                supply.attachTerminal(VoltageSupply.TYPE.terminal(0), wire);
                var values = new ArrayList<Double>();
                component.observe(0, (status, value) -> {
                    assertEquals(ObservationStatus.AVAILABLE, status);
                    values.add(value);
                });
                system.tick();
                assertEquals(5 / 1.0001, values.getLast(), 1e-9);
                var restored = DeviceComponent.CODEC.decode(DeviceComponent.CODEC.encode(component));
                ElectricalDeviceSystem.unloadDevice(component);
                assertThrows(IllegalArgumentException.class, () -> ElectricalDeviceSystem.bindDevice(restored, wrong, system, () -> {
                }));
                assertNull(restored.device());
                assertEquals(retained.id(), restored.getDeviceId());
                restored.overrides().set(5, 0);
                assertThrows(IllegalArgumentException.class, () -> ElectricalDeviceSystem.bindDevice(restored, compiled, system, () -> {
                }));
                restored.overrides().set(5, 1e-6);
                restored.overrides().set(1, 0.5);
                restored.overrides().set(2, 0.75);
                assertThrows(IllegalArgumentException.class,
                        () -> ElectricalDeviceSystem.bindDevice(restored, compiled, system, () -> {
                        }));
                // A rejected restore must not queue a maximum conductance that invalidates this change.
                assertDoesNotThrow(() -> retained.setParameter(
                        GroundedSwitchedLogicGates.GROUNDED_SWITCHED_AND.parameter(2), 0.75));
                retained.setParameter(GroundedSwitchedLogicGates.GROUNDED_SWITCHED_AND.parameter(2), 0);
                restored.overrides().remove(1);
                restored.overrides().remove(2);
                ElectricalDeviceSystem.bindDevice(restored, compiled, system, () -> {
                });
                assertEquals(retained.id(), restored.device().id());
                var restoredValues = new ArrayList<Double>();
                restored.observe(0, (status, value) -> {
                    assertEquals(ObservationStatus.AVAILABLE, status);
                    restoredValues.add(value);
                });
                supply.setParameter(VoltageSupply.TYPE.parameter(0), 4);
                system.tick();
                assertEquals(4 / 1.0001, restoredValues.getLast(), 1e-9);
            }
        }
    }

    private static BsonDocument readJson(String path) throws IOException {
        try (var input = GroundedSwitchedLogicGateAssetTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return BsonDocument.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
