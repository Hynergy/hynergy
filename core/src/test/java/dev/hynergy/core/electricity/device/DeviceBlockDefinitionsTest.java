package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.joml.Vector3i;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceBlockDefinitionsTest {
    private ElectricalRuntime runtime;

    @BeforeEach
    void registerDefinition() {
        runtime = ElectricalRuntime.create();
        runtime.register(PrimitiveDeviceTypes.RESISTANCE);
    }

    @AfterEach
    void closeRuntime() {
        runtime.close();
    }

    @Test
    void rebuildCompilesSharedConfigOnceAndKeepsRuntimeAndPortDefinitionsAligned() {
        DeviceDescriptorRegistry descriptors = descriptors();
        descriptors.freeze();
        PortModule ports = new PortModule();
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor = conductorStandard(ports);
        DeviceBlockDefinitions.Publisher publisher = new DeviceBlockDefinitions.Publisher(
                descriptors,
                ports,
                conductor
        );
        DeviceConfig shared = config("test:shared", 100.0, 4);

        publisher.rebuild(List.of(
                new DeviceBlockDefinitions.Binding(2, shared),
                new DeviceBlockDefinitions.Binding(70, shared)
        ));

        CompiledDeviceConfig atTwo = publisher.get(2);
        CompiledDeviceConfig atSeven = publisher.get(70);
        assertSame(atTwo, atSeven);
        assertSame(atTwo.portDefinition(), ports.blockPorts(2));
        assertSame(atSeven.portDefinition(), ports.blockPorts(70));

        DeviceConfig replacement = config("test:replacement", 220.0, 9);
        publisher.rebuild(List.of(new DeviceBlockDefinitions.Binding(70, replacement)));

        assertNull(publisher.get(2));
        assertNull(ports.blockPorts(2));
        assertSame(publisher.get(70).portDefinition(), ports.blockPorts(70));
        assertEquals(220.0, publisher.get(70).parameters().get(0).defaultValue());
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

    @Test
    void nativeInvalidDefaultLeavesPublishedConfigurationsAndPortsIntact() {
        DeviceDescriptorRegistry descriptors = descriptors();
        descriptors.freeze();
        PortModule ports = new PortModule();
        var publisher = new DeviceBlockDefinitions.Publisher(descriptors, ports, conductorStandard(ports));
        publisher.rebuild(List.of(new DeviceBlockDefinitions.Binding(2, config("test:old", 100.0, 4))));
        CompiledDeviceConfig previous = publisher.get(2);
        assertThrows(IllegalArgumentException.class, () -> publisher.rebuild(List.of(
                new DeviceBlockDefinitions.Binding(70, config("test:new", 220.0, 9)),
                new DeviceBlockDefinitions.Binding(3, config("test:invalid", 0.0, 7)))));
        assertSame(previous, publisher.get(2));
        assertSame(previous.portDefinition(), ports.blockPorts(2));
        assertNull(publisher.get(70));
        assertNull(publisher.get(3));
        assertNull(ports.blockPorts(70));
        assertNull(ports.blockPorts(3));
        publisher.rebuild(List.of(new DeviceBlockDefinitions.Binding(70, config("test:new", 220.0, 9))));
        assertNull(publisher.get(2));
        assertEquals(220.0, publisher.get(70).parameter(0).defaultValue());
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
