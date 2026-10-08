package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistryTest {
    @Test
    void nativeDependencyFailureIdentifiesAssetAndDependencyPath() {
        var invalid = DeviceType.define(b -> {
            var p = b.terminal(2, "positive");
            var n = b.terminal(5, "negative");
            b.element(PrimitiveDeviceTypes.DIODE, e -> {
                e.connect(PrimitiveDeviceTypes.DIODE.terminal(0), p);
                e.connect(PrimitiveDeviceTypes.DIODE.terminal(1), n);
                e.literal(PrimitiveDeviceTypes.DIODE.parameter(0), 1);
                e.literal(PrimitiveDeviceTypes.DIODE.parameter(1), 2);
            });
        });
        var root = DeviceType.define(b -> {
            var p = b.terminal(2, "positive");
            var n = b.terminal(5, "negative");
            b.element(invalid, e -> {
                e.connect(invalid.terminal(2), p);
                e.connect(invalid.terminal(5), n);
            });
        });
        try (var runtime = ElectricalRuntime.create()) {
            var registry = new DeviceRegistry(runtime);
            var failure = assertThrows(IllegalArgumentException.class, () -> registry.register("test:root", root));
            assertTrue(failure.getMessage().contains("test:root"));
            assertTrue(failure.getMessage().contains("root -> element[0]"));
            assertNotNull(failure.getCause());
            assertTrue(failure.getMessage().contains("commandIndex="));
            assertTrue(failure.getMessage().contains("byteOffset="));
            assertTrue(failure.getMessage().contains("code="));
            assertNull(registry.get("test:root"));
            assertDoesNotThrow(() -> registry.register("test:valid", PrimitiveDeviceTypes.RESISTANCE));
        }
    }

    @Test
    void registrationSharesBindingAndFreezesAtSetupBoundary() {
        try (var runtime = ElectricalRuntime.create()) {
            var registry = new DeviceRegistry(runtime);
            var first = registry.register("test:z", PrimitiveDeviceTypes.RESISTANCE);
            var second = registry.register("test:a", PrimitiveDeviceTypes.RESISTANCE);
            assertSame(first.binding(), second.binding());
            assertSame(first, registry.require("test:z"));
            assertThrows(IllegalStateException.class, () -> registry.register("test:z", PrimitiveDeviceTypes.RESISTANCE));
            for (String id : new String[]{"", "a", ":a", "a:", "a:b:c", " :a", "a: "})
                assertThrows(IllegalArgumentException.class, () -> registry.register(id, PrimitiveDeviceTypes.RESISTANCE));
            var event = new AssetEditorRequestDataSetEvent(null, "DeviceTypes", new String[0]);
            registry.populateDataSet(event);
            assertArrayEquals(new String[]{"test:a", "test:z"}, event.getResults());
            registry.freeze();
            assertTrue(registry.isFrozen());
            assertThrows(IllegalStateException.class, () -> registry.register("test:b", PrimitiveDeviceTypes.RESISTANCE));
            assertThrows(IllegalStateException.class, registry::freeze);
            assertNull(registry.get("test:missing"));
            assertThrows(IllegalArgumentException.class, () -> registry.require("test:missing"));
        }
    }

    @Test
    void failedNativeRegistrationDoesNotPublishAssetId() {
        var child = DeviceTestTypes.resistor(4, 2, 5, 6);
        var parent = DeviceType.define(b -> {
            var p = b.terminal(0, "positive");
            var n = b.terminal(1, "negative");
            b.element(child, e -> {
                e.connect(child.terminal(2), p);
                e.connect(child.terminal(5), n);
                e.literal(child.parameter(4), 20);
            });
            b.element(PrimitiveDeviceTypes.DIODE, e -> {
                e.connect(PrimitiveDeviceTypes.DIODE.terminal(0), p);
                e.connect(PrimitiveDeviceTypes.DIODE.terminal(1), n);
                e.literal(PrimitiveDeviceTypes.DIODE.parameter(0), 1);
                e.literal(PrimitiveDeviceTypes.DIODE.parameter(1), 2);
            });
        });
        try (var runtime = ElectricalRuntime.create()) {
            var registry = new DeviceRegistry(runtime);
            assertThrows(IllegalArgumentException.class, () -> registry.register("test:failed", parent));
            assertNull(registry.get("test:failed"));
            var event = new AssetEditorRequestDataSetEvent(null, "DeviceTypes", new String[0]);
            registry.populateDataSet(event);
            assertArrayEquals(new String[0], event.getResults());
            var binding = registry.register("test:child", child).binding();
            assertSame(binding, runtime.register(child));
            binding.validateParameter(child.parameter(4), 20);
            try (var system = runtime.createSystem(20)) {
                var device = system.create(child);
                device.setParameter(child.parameter(4), 20);
                assertDoesNotThrow(system::tick);
            }
        }
    }
}
