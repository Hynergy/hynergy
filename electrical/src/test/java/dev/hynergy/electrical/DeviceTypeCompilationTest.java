package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class DeviceTypeCompilationTest {
    @Test void observersDistinguishTwoInstancesOfTheSameChildDeclaration() {
        var type = DeviceType.define(b -> {
            var output = b.node(); var ground = b.ground();
            b.element(PrimitiveDeviceTypes.VOLTAGE_SOURCE, e -> {
                e.connect(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(0), output);
                e.connect(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(1), ground);
                e.literal(PrimitiveDeviceTypes.VOLTAGE_SOURCE.parameter(0), 10);
            });
            var first = b.element(PrimitiveDeviceTypes.RESISTANCE, e -> {
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(0), output);
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(1), ground);
                e.literal(PrimitiveDeviceTypes.RESISTANCE.parameter(0), 1000);
            });
            var second = b.element(PrimitiveDeviceTypes.RESISTANCE, e -> {
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(0), output);
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(1), ground);
                e.literal(PrimitiveDeviceTypes.RESISTANCE.parameter(0), 2000);
            });
            b.childObserver(6, "first", first, PrimitiveDeviceTypes.RESISTANCE.observer(1));
            b.childObserver(7, "second", second, PrimitiveDeviceTypes.RESISTANCE.observer(1));
        });
        try (var runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            try (var system = runtime.createSystem(20)) {
                var device = system.create(type);
                var first = new ArrayList<Double>(); var second = new ArrayList<Double>();
                device.observe(type.observer(6), (status, value) -> first.add(value));
                device.observe(type.observer(7), (status, value) -> second.add(value));
                system.tick(); assertEquals(0.01, first.getLast(), 1e-9); assertEquals(0.005, second.getLast(), 1e-9);
            }
        }
    }
    @Test void nestedElementCallbacksKeepChildInstanceObserversDistinct() {
        var type = DeviceType.define(b -> {
            var output = b.node(); var ground = b.ground();
            var source = b.element(PrimitiveDeviceTypes.VOLTAGE_SOURCE, e -> {
                e.connect(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(0), output);
                e.connect(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(1), ground);
                e.literal(PrimitiveDeviceTypes.VOLTAGE_SOURCE.parameter(0), 10);
                var load = b.element(PrimitiveDeviceTypes.RESISTANCE, r -> {
                    r.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(0), output);
                    r.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(1), ground);
                    r.literal(PrimitiveDeviceTypes.RESISTANCE.parameter(0), 1000);
                });
                b.childObserver(0, "load_current", load, PrimitiveDeviceTypes.RESISTANCE.observer(1));
            });
            b.childObserver(1, "source_current", source, PrimitiveDeviceTypes.VOLTAGE_SOURCE.observer(1));
        });
        try (var runtime = ElectricalRuntime.create()) {
            runtime.register(type);
            try (var system = runtime.createSystem(20)) {
                var device = system.create(type);
                var load = new ArrayList<Double>(); var source = new ArrayList<Double>();
                device.observe(type.observer(0), (status, value) -> load.add(value));
                device.observe(type.observer(1), (status, value) -> source.add(value));
                system.tick(); assertEquals(0.01, load.getLast(), 1e-9); assertEquals(-0.01, source.getLast(), 1e-9);
            }
        }
    }
    static DeviceType resistor() {
        return DeviceType.define(b -> {
            var positive = b.terminal(2, "positive");
            var middle = b.node();
            var ground = b.ground();
            var negative = b.terminal(5, "negative");
            var resistance = b.parameter(4, "resistance", ParameterConstraints.positiveFinite());
            var r = b.element(PrimitiveDeviceTypes.RESISTANCE, e -> {
                e.bind(PrimitiveDeviceTypes.RESISTANCE.parameter(0), resistance);
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(1), middle);
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(0), positive);
            });
            b.element(PrimitiveDeviceTypes.VOLTAGE_SOURCE, e -> {
                e.literal(PrimitiveDeviceTypes.VOLTAGE_SOURCE.parameter(0), 0);
                e.connect(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(1), negative);
                e.connect(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(0), middle);
            });
            b.element(PrimitiveDeviceTypes.VOLTAGE_SOURCE, e -> {
                e.connect(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(1), ground);
                e.connect(PrimitiveDeviceTypes.VOLTAGE_SOURCE.terminal(0), negative);
                e.literal(PrimitiveDeviceTypes.VOLTAGE_SOURCE.parameter(0), 0);
            });
            b.childObserver(6, "current", r, PrimitiveDeviceTypes.RESISTANCE.observer(1));
        });
    }
    public static DeviceType failingParent(DeviceType child) {
        return DeviceType.define(b -> {
            var p = b.terminal(2, "positive"); var n = b.terminal(5, "negative");
            b.element(child, e -> {
                e.connect(child.terminal(2), p); e.connect(child.terminal(5), n);
                e.literal(child.parameter(4), 20);
            });
            b.element(PrimitiveDeviceTypes.DIODE, e -> {
                e.connect(PrimitiveDeviceTypes.DIODE.terminal(0), p); e.connect(PrimitiveDeviceTypes.DIODE.terminal(1), n);
                e.literal(PrimitiveDeviceTypes.DIODE.parameter(0), 1);
                e.literal(PrimitiveDeviceTypes.DIODE.parameter(1), 2);
            });
        });
    }
    @Test void sparseMembersAndInterleavedNodesBindInNativeOrder() {
        var type = resistor();
        try (var runtime = ElectricalRuntime.create()) {
            var binding = runtime.register(type);
            assertSame(binding, runtime.register(type));
            assertSame(type, binding.type());
            binding.validateParameter(type.parameter(4), 20);
            try (var system = runtime.createSystem(20)) {
                var device = system.create(type);
                device.setParameter(type.parameter(4), 20);
                var source = dev.hynergy.electrical.primitives.sources.VoltageSource.create(system, 5);
                var p = system.createWire(); var n = system.createWire();
                source.attachPositive(p); source.attachNegative(n);
                device.attachTerminal(type.terminal(2), p); device.attachTerminal(type.terminal(5), n);
                var current = new ArrayList<Double>();
                device.observe(type.observer(6), (status, value) -> { assertEquals(ObservationStatus.AVAILABLE, status); current.add(value); });
                system.tick(); assertEquals(0.25, current.getLast(), 1e-9);
            }
        }
    }
    @Test void failedParentPreservesSuccessfulDependencyBinding() {
        var child = resistor(); var parent = failingParent(child);
        try (var runtime = ElectricalRuntime.create()) {
            assertThrows(IllegalArgumentException.class, () -> runtime.register(parent));
            assertThrows(IllegalStateException.class, () -> runtime.requireBinding(parent));
            var binding = runtime.register(child);
            assertSame(binding, runtime.register(child));
            binding.validateParameter(child.parameter(4), 20);
        }
    }
}
