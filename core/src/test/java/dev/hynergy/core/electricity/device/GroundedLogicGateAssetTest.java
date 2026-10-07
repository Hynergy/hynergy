package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.ObservationStatus;
import dev.hynergy.electrical.composite.GroundedLogicGates;
import dev.hynergy.electrical.composite.ResistiveLoad;
import dev.hynergy.electrical.composite.VoltageSupply;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class GroundedLogicGateAssetTest {
    @ParameterizedTest
    @CsvSource({
            "And, 8, 5", "Or, 14, 5", "Nand, 7, 5", "Nor, 1, 5", "Not, 1, 5",
            "And, 8, 9", "Or, 14, 9", "Nand, 7, 9", "Nor, 1, 9", "Not, 1, 9"
    })
    void configuredGateDrivesLampWithoutExternalSupply(String asset, int truthTable, double voltage)
            throws IOException {
        var type = switch (asset) {
            case "And" -> GroundedLogicGates.GROUNDED_AND;
            case "Or" -> GroundedLogicGates.GROUNDED_OR;
            case "Nand" -> GroundedLogicGates.GROUNDED_NAND;
            case "Nor" -> GroundedLogicGates.GROUNDED_NOR;
            case "Not" -> GroundedLogicGates.GROUNDED_NOT;
            default -> throw new IllegalArgumentException(asset);
        };
        int inputCount = asset.equals("Not") ? 1 : 2;
        try (var runtime = ElectricalRuntime.create()) {
            var registry = new DeviceRegistry(runtime);
            registry.register("hynergy:grounded_" + asset.toLowerCase(java.util.Locale.ROOT), type);
            registry.register("hynergy:voltage_supply", VoltageSupply.TYPE);
            registry.register("hynergy:resistive_load", ResistiveLoad.RESISTIVE_LOAD);
            var ports = new PortModule();
            var domain = ports.<ElectricalPortConnection>registerDomain("test:electrical");
            var standard = ports.registerStandard("test:conductor", domain, ElectricalPortProfile.class,
                    (first, second, geometry) -> ElectricalPortConnection.DIRECT);
            var codec = DeviceConfig.createCodec(registry);
            var gateAsset = readJson("Server/Hynergy/Electricity/Devices/" + asset + ".json");
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
                ElectricalDeviceSystem.bindDevice(gate, gateConfig, system, () -> {});
                ElectricalDeviceSystem.bindDevice(lamp, lampConfig, system, () -> {});
                var sources = new ArrayList<DeviceComponent>();
                for (int terminal : inputCount == 1 ? new int[]{0, 2} : new int[]{0, 2, 3}) {
                    var wire = system.createWire();
                    assertTrue(DeviceWireConnections.attachPortIfNew(gate, terminal, wire,
                            new DeviceWireConnections.AttachmentDedup()));
                    var peer = terminal == 0 ? lamp : new DeviceComponent("PowerSupply");
                    if (terminal != 0) {
                        ElectricalDeviceSystem.bindDevice(peer, supplyConfig, system, () -> {});
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
                        sources.get(1).device().setParameter(VoltageSupply.TYPE.parameter(0), (inputs & 2) == 0 ? 0 : 5);
                    }
                    system.tick();
                    assertFalse(statuses.isEmpty(), "Lamp must publish a result for inputs " + inputs);
                    assertEquals(ObservationStatus.AVAILABLE, statuses.getLast(), "Inputs " + inputs);
                    assertEquals((truthTable & (1 << inputs)) != 0 ? voltage / 101.0 : 0.0, currents.getLast(), 1e-8,
                            "Inputs " + inputs);
                }
            }
        }
    }

    private static BsonDocument readJson(String path) throws IOException {
        try (var input = GroundedLogicGateAssetTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return BsonDocument.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
