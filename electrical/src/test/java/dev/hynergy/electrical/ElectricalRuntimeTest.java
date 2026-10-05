package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElectricalRuntimeTest {
    @Test void requestCloseRejectsRegistrationAndCreationUntilActiveSystemsClose() {
        var type = resistorComposite(PrimitiveDeviceTypes.RESISTANCE);
        var runtime = ElectricalRuntime.create();
        var binding = runtime.register(type);
        var system = runtime.createSystem(20);
        try {
            runtime.requestClose();
            assertThrows(IllegalStateException.class, () -> runtime.register(type));
            assertThrows(IllegalStateException.class, () -> runtime.register(PrimitiveDeviceTypes.RESISTANCE));
            assertThrows(IllegalStateException.class, () -> runtime.createSystem(20));
            assertThrows(IllegalStateException.class, runtime::close);
            assertDoesNotThrow(() -> system.create(type));
            assertDoesNotThrow(system::tick);
            system.close();
            assertThrows(IllegalStateException.class, () -> runtime.register(type));
            assertThrows(IllegalStateException.class, () -> runtime.createSystem(20));
            assertSame(type, binding.type());
            assertEquals(2, type.terminalCount());
            assertDoesNotThrow(runtime::requestClose);
        } finally { system.close(); runtime.close(); }
    }
    static DeviceType resistorComposite(DeviceType resistance) {
        return DeviceType.define(b -> {
            var p = b.terminal(0, "positive"); var n = b.terminal(1, "negative");
            b.element(resistance, e -> {
                e.connect(resistance.terminal(0), p); e.connect(resistance.terminal(1), n);
                e.literal(resistance.parameter(0), 1000);
            });
        });
    }
    static DeviceType forwardingComposite(DeviceType child) {
        return DeviceType.define(b -> {
            var p = b.terminal(0, "positive"); var n = b.terminal(1, "negative");
            b.element(child, e -> { e.connect(child.terminal(0), p); e.connect(child.terminal(1), n); });
        });
    }
    @Test void registrationResolvesCustomChildBeforeParent() {
        var child = resistorComposite(PrimitiveDeviceTypes.RESISTANCE);
        var parent = forwardingComposite(child);
        try (var runtime = ElectricalRuntime.create()) {
            var binding = runtime.register(parent);
            assertNotEquals(binding.definition().id(), runtime.requireDefinition(child).id());
            assertSame(binding, runtime.register(parent));
        }
    }
    @Test void failedRootRegistrationKeepsSuccessfulDependencies() {
        var child = DeviceTypeCompilationTest.resistor();
        var parent = DeviceTypeCompilationTest.failingParent(child);
        try (var runtime = ElectricalRuntime.create()) {
            assertThrows(IllegalArgumentException.class, () -> runtime.register(parent));
            assertSame(runtime.requireBinding(child), runtime.register(child));
        }
    }
    @Test void unregisteredTypesCannotBeAddedWhileSystemsAreActive() {
        var type = resistorComposite(PrimitiveDeviceTypes.RESISTANCE);
        try (var runtime = ElectricalRuntime.create()) {
            try (var system = runtime.createSystem(20)) {
                assertThrows(IllegalStateException.class, () -> runtime.register(type));
                assertThrows(IllegalStateException.class, () -> system.create(type));
            }
            assertDoesNotThrow(() -> runtime.register(type));
        }
    }
    @Test void declarationCanBeReusedAfterRuntimeCloses() {
        var type = resistorComposite(PrimitiveDeviceTypes.RESISTANCE);
        RegisteredDeviceType old;
        try (var runtime = ElectricalRuntime.create()) { old = runtime.register(type); }
        assertEquals(2, type.terminalCount());
        try (var runtime = ElectricalRuntime.create()) {
            var binding = runtime.register(type);
            assertNotSame(old, binding);
            try (var system = runtime.createSystem(20)) { assertNotNull(system.create(type)); }
        }
    }
    @Test void primitiveTypeKeepsItsDefinitionId() {
        try (var runtime = ElectricalRuntime.create()) {
            var binding = runtime.register(PrimitiveDeviceTypes.RESISTANCE);
            assertEquals(1, binding.definition().id());
            assertSame(binding, runtime.register(PrimitiveDeviceTypes.RESISTANCE));
        }
    }
}
