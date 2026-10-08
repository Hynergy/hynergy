package dev.hynergy.electrical;

import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DeviceDefinitionTest {

    @Test
    void zeroDefinitionIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DeviceDefinition(0));
    }

    @Test
    void definitionIdUsesFullUnsignedRange() {
        DeviceDefinition definition = new DeviceDefinition(-1);

        assertEquals(-1, definition.id());
        assertEquals(0xffff_ffffL, Integer.toUnsignedLong(definition.id()));
    }

    @Test
    void groundNodesShareNodeSequenceAndRegisterWithoutAddingTerminals() {
        try (ElectricalEngine engine = ElectricalEngine.create();
             DeviceDefinitionBuilder builder = new DeviceDefinitionBuilder()) {
            int first = builder.addTerminal();
            int internal = builder.addNode();
            int ground = builder.addGroundNode();
            int second = builder.addTerminal();
            int otherGround = builder.addGroundNode();
            assertEquals(0, first);
            assertEquals(1, internal);
            assertEquals(2, ground);
            assertEquals(3, second);
            assertEquals(4, otherGround);
            for (int[] pair : new int[][]{{first, internal}, {internal, ground}, {second, otherGround}}) {
                builder.beginElement(new DeviceDefinition(1))
                       .elementTerminal(pair[0]).elementTerminal(pair[1])
                       .elementLiteral(1000.0).endElement();
            }
            assertEquals(8, builder.commandCount());
            engine.registerDefinition(builder);
        }
    }

    @Test
    void groundNodeRejectsClosedBuilderAndOpenChild() {
        DeviceDefinitionBuilder closed = new DeviceDefinitionBuilder();
        closed.close();
        assertThrows(IllegalStateException.class, closed::addGroundNode);
        try (DeviceDefinitionBuilder builder = new DeviceDefinitionBuilder()) {
            builder.beginElement(new DeviceDefinition(1));
            assertThrows(IllegalStateException.class, builder::addGroundNode);
        }
    }

    @Test
    void resetClearsGroundCommandsAndNodeCount() {
        try (ElectricalEngine engine = ElectricalEngine.create();
             DeviceDefinitionBuilder builder = new DeviceDefinitionBuilder()) {
            assertEquals(0, builder.addGroundNode());
            assertThrows(IllegalArgumentException.class, () -> engine.registerDefinition(builder));
            builder.reset();
            assertEquals(0, builder.commandCount());
            int output = builder.addTerminal();
            int ground = builder.addGroundNode();
            assertEquals(0, output);
            assertEquals(1, ground);
            builder.beginElement(new DeviceDefinition(1))
                   .elementTerminal(output).elementTerminal(ground)
                   .elementLiteral(1000.0).endElement();
            engine.registerDefinition(builder);
        }
    }

    @Test
    void groundedJavaCompositePublishesNativeVoltageAndCurrents() {
        for (boolean exposed : new boolean[]{true, false}) {
            DeviceType type = DeviceType.define(builder -> {
                var output = exposed ? builder.terminal(0, "output") : builder.node();
                var ground = builder.ground();
                builder.voltageObserver(0, "voltage", output, ground);
                var source = builder.element(VoltageSource.TYPE, e -> {
                    e.connect(VoltageSource.TYPE.terminal(0), output); e.connect(VoltageSource.TYPE.terminal(1), ground);
                    e.literal(VoltageSource.TYPE.parameter(0), 10);
                });
                builder.childObserver(1, "source_current", source, VoltageSource.TYPE.observer(1));
                var load = builder.element(Resistance.TYPE, e -> {
                    e.connect(Resistance.TYPE.terminal(0), output); e.connect(Resistance.TYPE.terminal(1), ground);
                    e.literal(Resistance.TYPE.parameter(0), 1000);
                });
                builder.childObserver(2, "load_current", load, Resistance.TYPE.observer(1));
            });
            try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
                runtime.register(type);
                try (ElectricalSystem system = runtime.createSystem(20)) {
                    Device device = system.create(type);
                    ArrayList<Double> voltage = new ArrayList<>();
                    ArrayList<Double> sourceCurrent = new ArrayList<>();
                    ArrayList<Double> loadCurrent = new ArrayList<>();
                    device.observe(type.observer(0), (status, value) -> {
                        assertEquals(ObservationStatus.AVAILABLE, status);
                        voltage.add(value);
                    });
                    device.observe(type.observer(1), (status, value) -> {
                        assertEquals(ObservationStatus.AVAILABLE, status);
                        sourceCurrent.add(value);
                    });
                    device.observe(type.observer(2), (status, value) -> {
                        assertEquals(ObservationStatus.AVAILABLE, status);
                        loadCurrent.add(value);
                    });
                    system.tick();
                    assertEquals(1, voltage.size());
                    assertEquals(10.0, voltage.getFirst(), 1e-9);
                    assertEquals(1, sourceCurrent.size());
                    assertEquals(-0.01, sourceCurrent.getFirst(), 1e-9);
                    assertEquals(1, loadCurrent.size());
                    assertEquals(0.01, loadCurrent.getFirst(), 1e-9);
                }
            }
        }
    }

}
