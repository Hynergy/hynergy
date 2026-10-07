package dev.hynergy.electrical.composite;

import dev.hynergy.electrical.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class GroundedSwitchedLogicGatesTest {
    @Test
    void invalidRelationRejectsPendingAndAppliedUpdatesWithoutPoisoningWorld() {
        var type = GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOT;
        try (var runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            try (var system = runtime.createSystem(20)) {
                var gate = create(system, type);
                var id = gate.id();
                assertThrows(IllegalArgumentException.class, () -> gate.setParameter(type.parameter(2), 1));
                var values = new ArrayList<Double>();
                gate.observe(type.observer(0), (status, value) -> {
                    assertEquals(ObservationStatus.AVAILABLE, status);
                    values.add(value);
                });
                system.tick();
                assertEquals(5 / 1.000001, values.getLast(), 1e-9);
                gate.setParameter(type.parameter(2), 0.5);
                assertThrows(IllegalArgumentException.class, () -> gate.setParameter(type.parameter(1), 0.5));
                gate.setParameter(type.parameter(3), 4);
                system.tick();
                assertEquals(id, gate.id());
                assertEquals(4 / 1.000001, values.getLast(), 1e-9);
            }
        }
    }

    private static final DeviceType[] TYPES = {
        GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOT,
        GroundedSwitchedLogicGates.GROUNDED_SWITCHED_AND,
        GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NAND,
        GroundedSwitchedLogicGates.GROUNDED_SWITCHED_OR,
        GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOR
    };

    private static Device create(ElectricalSystem system, DeviceType type) {
        var device = system.create(type);
        double[] parameters = {2.5, 1, 0, 5, 1e-6, 1e-6};
        for (int i = 0; i < parameters.length; i++) device.setParameter(type.parameter(i), parameters[i]);
        return device;
    }

    @Test
    void unattachedAndDanglingInputsRemainAvailable() {
        try (var runtime = ElectricalRuntime.create()) {
            for (var type : TYPES) runtime.register(type);
            try (var system = runtime.createSystem(20)) {
                for (int i = 0; i < TYPES.length; i++) {
                    var type = TYPES[i];
                    var gate = create(system, type);
                    var values = new ArrayList<Double>();
                    gate.observe(type.observer(0), (status, value) -> {
                        assertEquals(ObservationStatus.AVAILABLE, status);
                        values.add(value);
                    });
                    system.tick();
                    assertFalse(values.isEmpty());
                    double expected = i == 0 || i == 2 || i == 4 ? 5 / 1.000001 : 0;
                    assertEquals(expected, values.getLast(), 1e-9);
                    gate.attachTerminal(type.terminal(2), system.createWire());
                    system.tick();
                    assertEquals(expected, values.getLast(), 1e-9);
                }
            }
        }
    }

    @Test
    void lowGateReleasesSharedWireAndInvalidUpdatePreservesDevice() {
        var type = GroundedSwitchedLogicGates.GROUNDED_SWITCHED_AND;
        try (var runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            runtime.register(VoltageSupply.TYPE);
            try (var system = runtime.createSystem(20)) {
                var gate = create(system, type);
                var id = gate.id();
                var supply = system.create(VoltageSupply.TYPE);
                supply.setParameter(VoltageSupply.TYPE.parameter(0), 5);
                supply.setParameter(VoltageSupply.TYPE.parameter(1), 100);
                var wire = system.createWire();
                gate.attachTerminal(type.terminal(0), wire);
                supply.attachTerminal(VoltageSupply.TYPE.terminal(0), wire);
                var values = new ArrayList<Double>();
                gate.observe(type.observer(0), (status, value) -> {
                    assertEquals(ObservationStatus.AVAILABLE, status);
                    values.add(value);
                });
                system.tick();
                assertEquals(5 / 1.0001, values.getLast(), 1e-9);
                assertThrows(IllegalArgumentException.class, () -> gate.setParameter(type.parameter(5), 0));
                assertEquals(id, gate.id());
                supply.setParameter(VoltageSupply.TYPE.parameter(0), 4);
                system.tick();
                assertEquals(4 / 1.0001, values.getLast(), 1e-9);
            }
        }
    }
}
