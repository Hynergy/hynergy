package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceBlockDefinitionsTest {
    @Test
    void rebuildCompilesSharedConfigOnceAndKeepsRuntimeAndPortDefinitionsAligned() {
        DeviceDescriptorRegistry descriptors = descriptors();
        descriptors.freeze();
        RuntimeDeviceDefinitions runtimeDefinitions = new RuntimeDeviceDefinitions();
        PortModule ports = new PortModule();
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor = conductorStandard(ports);
        DeviceBlockDefinitions.Publisher publisher = new DeviceBlockDefinitions.Publisher(
                descriptors,
                runtimeDefinitions,
                ports,
                conductor
        );
        DeviceConfig shared = config("test:shared", 100.0, 4);

        publisher.rebuild(List.of(
                new DeviceBlockDefinitions.Binding(2, shared),
                new DeviceBlockDefinitions.Binding(7, shared)
        ));

        CompiledDeviceConfig atTwo = runtimeDefinitions.get(2);
        CompiledDeviceConfig atSeven = runtimeDefinitions.get(7);
        assertSame(atTwo, atSeven);
        assertSame(atTwo.portDefinition(), ports.blockPorts(2));
        assertSame(atSeven.portDefinition(), ports.blockPorts(7));

        DeviceConfig replacement = config("test:replacement", 220.0, 9);
        publisher.rebuild(List.of(new DeviceBlockDefinitions.Binding(7, replacement)));

        assertNull(runtimeDefinitions.get(2));
        assertNull(ports.blockPorts(2));
        assertSame(runtimeDefinitions.get(7).portDefinition(), ports.blockPorts(7));
        assertEquals(220.0, runtimeDefinitions.get(7).parameterDefaultAt(0));
    }

    @Test
    void rebuildPublishesAllReplacementsBeforeClearingAnyStaleIndexes() {
        DeviceDescriptorRegistry descriptors = descriptors();
        descriptors.freeze();
        PortModule ports = new PortModule();
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor = conductorStandard(ports);
        RecordingSink sink = new RecordingSink();
        DeviceBlockDefinitions.Publisher publisher =
                new DeviceBlockDefinitions.Publisher(descriptors, conductor, sink);

        publisher.rebuild(List.of(
                new DeviceBlockDefinitions.Binding(1, config("test:first", 100.0, 1)),
                new DeviceBlockDefinitions.Binding(3, config("test:second", 220.0, 3))
        ));

        sink.events.clear();
        publisher.rebuild(List.of(
                new DeviceBlockDefinitions.Binding(8, config("test:replacement", 470.0, 8))
        ));

        assertEquals("set:8", sink.events.getFirst());
        assertTrue(sink.events.subList(1, sink.events.size()).contains("clear:1"));
        assertTrue(sink.events.subList(1, sink.events.size()).contains("clear:3"));
    }

    @Test
    void publishingRequiresFrozenDescriptors() {
        DeviceDescriptorRegistry descriptors = descriptors();
        PortModule ports = new PortModule();
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor = conductorStandard(ports);
        DeviceBlockDefinitions.Publisher publisher = new DeviceBlockDefinitions.Publisher(
                descriptors,
                new RuntimeDeviceDefinitions(),
                ports,
                conductor
        );

        assertThrows(
                IllegalStateException.class,
                () -> publisher.rebuild(List.of(
                        new DeviceBlockDefinitions.Binding(0, config("test:device", 100.0, 0))
                ))
        );
    }

    private static DeviceDescriptorRegistry descriptors() {
        DeviceDescriptorRegistry descriptors = new DeviceDescriptorRegistry();
        descriptors.register(
                "test:resistance",
                Resistance.TYPE,
                new MemberMapping(0),
                new MemberMapping(0, 1),
                new MemberMapping(0, 1)
        );
        return descriptors;
    }

    private static DeviceConfig config(String id, double defaultValue, int portId) {
        return new DeviceConfig(
                id,
                "test:resistance",
                new DeviceParameterConfig[]{new DeviceParameterConfig(0, defaultValue)},
                new DevicePortConfig[]{
                        new DevicePortConfig(portId, 0, null, new Vector3i(1, 0, 0))
                }
        );
    }

    private static PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard(
            PortModule ports
    ) {
        PortDomain<ElectricalPortConnection> domain = ports.registerDomain("test:electrical");
        return ports.registerStandard(
                "test:conductor",
                domain,
                ElectricalPortProfile.class,
                (first, second, geometry) -> ElectricalPortConnection.DIRECT
        );
    }

    private static final class RecordingSink implements DeviceBlockDefinitions.Sink {
        private final ArrayList<String> events = new ArrayList<>();

        @Override
        public void set(int blockTypeIndex, CompiledDeviceConfig compiled) {
            events.add("set:" + blockTypeIndex);
        }

        @Override
        public void clear(int blockTypeIndex) {
            events.add("clear:" + blockTypeIndex);
        }
    }
}
