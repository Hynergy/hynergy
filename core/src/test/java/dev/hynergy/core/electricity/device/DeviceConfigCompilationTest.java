package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.*;
import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import org.joml.Vector3i;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceConfigCompilationTest {
    private ElectricalRuntime runtime;
    private DeviceType type;

    @BeforeEach
    void registerDefinition() {
        runtime = ElectricalRuntime.create();
        type = DeviceType.create(builder -> {
            int positive = builder.addTerminal();
            int negative = builder.addTerminal();
            int first = builder.addParameter(dev.hynergy.electrical.DeviceDefinitionBuilder.Bound.inclusive(1.0), null, true);
            int second = builder.addParameter(dev.hynergy.electrical.DeviceDefinitionBuilder.Bound.inclusive(1.0), null, true);
            builder.beginElement(PrimitiveDeviceTypes.RESISTANCE)
                   .elementTerminal(positive).elementTerminal(negative).elementParameter(first).endElement();
            builder.beginElement(PrimitiveDeviceTypes.RESISTANCE)
                   .elementTerminal(positive).elementTerminal(negative).elementParameter(second).endElement();
            builder.addChildObserver(0, 1);
            builder.addChildObserver(1, 1);
        });
        runtime.register(type);
    }

    @AfterEach
    void closeRuntime() {
        runtime.close();
    }

    @Test
    void compileMapsStableIdsOnceAndPreservesStableGaps() {
        DeviceDescriptorRegistry registry = registry(
                new MemberMapping(-1, 0, -1, 1),
                new MemberMapping(-1, 0, 1),
                new MemberMapping(0, -1, 1)
        );
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor = conductorStandard();
        DeviceConfig config = new DeviceConfig(
                "test:device",
                "test:resistance",
                new DeviceParameterConfig[]{
                        new DeviceParameterConfig(3, 470.0),
                        new DeviceParameterConfig(1, 100.0)
                },
                new DevicePortConfig[]{
                        new DevicePortConfig(7, 2, new Vector3i(1, 2, 3), new Vector3i(0, 1, 0)),
                        new DevicePortConfig(2, 1, null, new Vector3i(1, 0, 0))
                }
        );

        CompiledDeviceConfig compiled = config.compile(registry, conductor);

        assertSame(registry.require("test:resistance"), compiled.descriptor());
        assertEquals(2, compiled.parameters().size());
        assertEquals(3, compiled.parameters().get(0).stableId());
        assertEquals(1, compiled.parameters().get(0).nativeId());
        assertEquals(470.0, compiled.parameters().get(0).defaultValue());
        assertEquals(1, compiled.parameters().get(1).stableId());
        assertEquals(0, compiled.parameters().get(1).nativeId());
        assertEquals(100.0, compiled.parameters().get(1).defaultValue());
        assertEquals(-1, compiled.nativeParameterId(2));
        assertEquals(1, compiled.nativeParameterId(3));
        assertEquals(1, compiled.nativeObserverId(2));

        assertEquals(2, compiled.ports().size());
        assertEquals(7, compiled.ports().get(0).portId());
        assertEquals(1, compiled.ports().get(0).nativeTerminalId());
        assertEquals(2, compiled.ports().get(1).portId());
        assertEquals(0, compiled.ports().get(1).nativeTerminalId());

        BlockPortDefinition ports = compiled.portDefinition();
        assertEquals(2, ports.size());
        assertEquals(7, ports.portAt(0).localId());
        assertEquals(new PortOffset(1, 2, 3), ports.portAt(0).anchor());
        assertSame(conductor, ports.portAt(0).standard());
        assertEquals(new ElectricalPortProfile(0, 1, 0), ports.portAt(0).profile());
        assertEquals(PortOffset.ZERO, ports.portAt(1).anchor());
    }

    @Test
    void duplicateParameterDefaultsAreRejected() {
        DeviceDescriptorRegistry registry = registry(
                new MemberMapping(0),
                new MemberMapping(),
                new MemberMapping()
        );
        DeviceConfig config = new DeviceConfig(
                "test:device",
                "test:resistance",
                new DeviceParameterConfig[]{
                        new DeviceParameterConfig(0, 100.0),
                        new DeviceParameterConfig(0, 200.0)
                },
                new DevicePortConfig[0]
        );

        assertThrows(IllegalArgumentException.class, () -> config.compile(registry, conductorStandard()));
    }

    @Test
    void duplicateLocalPortIdsAreRejectedEvenWhenTerminalsDiffer() {
        DeviceDescriptorRegistry registry = registry(
                new MemberMapping(),
                new MemberMapping(0, 1),
                new MemberMapping()
        );
        DeviceConfig config = new DeviceConfig(
                "test:device",
                "test:resistance",
                new DeviceParameterConfig[0],
                new DevicePortConfig[]{
                        new DevicePortConfig(4, 0, null, new Vector3i(1, 0, 0)),
                        new DevicePortConfig(4, 1, null, new Vector3i(-1, 0, 0))
                }
        );

        assertThrows(IllegalArgumentException.class, () -> config.compile(registry, conductorStandard()));
    }

    @Test
    void unknownDescriptorIsRejected() {
        DeviceConfig config = new DeviceConfig(
                "test:device",
                "test:missing",
                new DeviceParameterConfig[0],
                new DevicePortConfig[0]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> config.compile(new DeviceDescriptorRegistry(), conductorStandard())
        );
    }

    @Test
    void unmappedParameterIsRejectedWithoutReinterpretingStableId() {
        DeviceDescriptorRegistry registry = registry(
                new MemberMapping(0, -1, 1),
                new MemberMapping(),
                new MemberMapping()
        );
        DeviceConfig config = new DeviceConfig(
                "test:device",
                "test:resistance",
                new DeviceParameterConfig[]{new DeviceParameterConfig(1, 10.0)},
                new DevicePortConfig[0]
        );

        assertThrows(IllegalArgumentException.class, () -> config.compile(registry, conductorStandard()));
    }

    @Test
    void unmappedTerminalIsRejectedWithoutReinterpretingStableId() {
        DeviceDescriptorRegistry registry = registry(
                new MemberMapping(),
                new MemberMapping(0, -1, 1),
                new MemberMapping()
        );
        DeviceConfig config = new DeviceConfig(
                "test:device",
                "test:resistance",
                new DeviceParameterConfig[0],
                new DevicePortConfig[]{
                        new DevicePortConfig(0, 1, null, new Vector3i(0, 0, 1))
                }
        );

        assertThrows(IllegalArgumentException.class, () -> config.compile(registry, conductorStandard()));
    }

    @Test
    void nonFiniteParameterDefaultsAreRejectedDuringCompilation() {
        DeviceDescriptorRegistry registry = registry(
                new MemberMapping(0),
                new MemberMapping(),
                new MemberMapping()
        );
        DeviceConfig config = new DeviceConfig(
                "test:device",
                "test:resistance",
                new DeviceParameterConfig[]{new DeviceParameterConfig(0, Double.NaN)},
                new DevicePortConfig[0]
        );

        assertThrows(IllegalArgumentException.class, () -> config.compile(registry, conductorStandard()));
    }

    @Test
    void emptyPhysicalPortListIsValid() {
        DeviceDescriptorRegistry registry = registry(
                new MemberMapping(),
                new MemberMapping(),
                new MemberMapping()
        );
        DeviceConfig config = new DeviceConfig(
                "test:device",
                "test:resistance",
                new DeviceParameterConfig[0],
                new DevicePortConfig[0]
        );

        CompiledDeviceConfig compiled = config.compile(registry, conductorStandard());

        assertEquals(0, compiled.ports().size());
        assertEquals(0, compiled.portDefinition().size());
    }

    @Test
    void compiledBindingsRemainImmutableWhenSourceListsChange() {
        DeviceDescriptorRegistry registry = registry(new MemberMapping(0), new MemberMapping(), new MemberMapping());
        var parameters = new java.util.ArrayList<CompiledDeviceConfig.ParameterBinding>();
        parameters.add(new CompiledDeviceConfig.ParameterBinding(0, 0, 100.0));
        CompiledDeviceConfig compiled = new CompiledDeviceConfig(registry.require("test:resistance"),
                parameters, java.util.List.of(), BlockPortDefinition.of());
        parameters.clear();
        assertEquals(100.0, compiled.parameter(0).defaultValue());
        assertThrows(UnsupportedOperationException.class, () -> compiled.parameters().clear());
        assertNull(compiled.parameter(1));
    }

    private DeviceDescriptorRegistry registry(
            MemberMapping parameters,
            MemberMapping terminals,
            MemberMapping observers
    ) {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        registry.register("test:resistance", type, parameters, terminals, observers);
        return registry;
    }

    private static PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard() {
        PortModule ports = new PortModule();
        PortDomain<ElectricalPortConnection> domain = ports.registerDomain("test:electrical");
        return ports.registerStandard(
                "test:conductor",
                domain,
                ElectricalPortProfile.class,
                (first, second, geometry) -> ElectricalPortConnection.DIRECT
        );
    }
}
