package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceDescriptorRegistryTest {
    @Test
    void dataSetContainsSortedIdsAndReturnsIndependentResults() {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        MemberMapping empty = new MemberMapping();
        registry.register("test:z", Resistance.TYPE, empty, empty, empty);
        registry.register("test:a", VoltageSource.TYPE, empty, empty, empty);

        var event = new AssetEditorRequestDataSetEvent(
                null, "DeviceTypes", null
        );
        registry.populateDataSet(event);
        assertArrayEquals(new String[]{"test:a", "test:z"}, event.getResults());
        event.getResults()[0] = "modified";
        registry.freeze();
        registry.populateDataSet(event);
        assertArrayEquals(new String[]{"test:a", "test:z"}, event.getResults());
    }

    @Test
    void dataSetIncludesRegistrationsMadeAfterAnEarlierRequest() {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        var event = new AssetEditorRequestDataSetEvent(null, "DeviceTypes", null);
        registry.populateDataSet(event);
        assertArrayEquals(new String[0], event.getResults());

        MemberMapping empty = new MemberMapping();
        registry.register("test:resistance", Resistance.TYPE, empty, empty, empty);
        registry.populateDataSet(event);
        assertArrayEquals(new String[]{"test:resistance"}, event.getResults());
    }

    @Test
    void registrationPreservesTypedDescriptorAndMappings() {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        MemberMapping parameters = new MemberMapping(0);
        MemberMapping terminals = new MemberMapping(0, 1);
        MemberMapping observers = new MemberMapping(0, 1);

        DeviceDescriptor descriptor = registry.register(
                "test:resistance",
                Resistance.TYPE,
                parameters,
                terminals,
                observers
        );

        assertSame(descriptor, registry.get("test:resistance"));
        assertSame(descriptor, registry.require("test:resistance"));
        assertSame(Resistance.TYPE, descriptor.type());
        assertSame(parameters, descriptor.parameters());
        assertSame(terminals, descriptor.terminals());
        assertSame(observers, descriptor.observers());
    }

    @Test
    void duplicateDescriptorIdIsRejected() {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        MemberMapping empty = new MemberMapping();
        registry.register("test:device", Resistance.TYPE, empty, empty, empty);

        assertThrows(
                IllegalStateException.class,
                () -> registry.register("test:device", VoltageSource.TYPE, empty, empty, empty)
        );
    }

    @Test
    void blankDescriptorIdIsRejected() {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        MemberMapping empty = new MemberMapping();

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register("  ", Resistance.TYPE, empty, empty, empty)
        );
    }

    @Test
    void unknownDescriptorKeysAreDistinguishedFromRegisteredDescriptors() {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();

        assertNull(registry.get("test:missing"));
        assertThrows(IllegalArgumentException.class, () -> registry.require("test:missing"));
    }

    @Test
    void freezeMakesRegistryImmutableButKeepsLookupsAvailable() {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        MemberMapping empty = new MemberMapping();
        DeviceDescriptor descriptor =
                registry.register("test:resistance", Resistance.TYPE, empty, empty, empty);

        assertFalse(registry.isFrozen());
        registry.freeze();
        assertTrue(registry.isFrozen());
        assertSame(descriptor, registry.require("test:resistance"));

        assertThrows(
                IllegalStateException.class,
                () -> registry.register("test:other", VoltageSource.TYPE, empty, empty, empty)
        );
        assertThrows(IllegalStateException.class, registry::freeze);
    }
}
