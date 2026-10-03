package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.*;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceConfigCompilationTest {
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
        assertEquals(2, compiled.parameterCount());
        assertEquals(3, compiled.stableParameterIdAt(0));
        assertEquals(1, compiled.nativeParameterIdAt(0));
        assertEquals(470.0, compiled.parameterDefaultAt(0));
        assertEquals(1, compiled.stableParameterIdAt(1));
        assertEquals(0, compiled.nativeParameterIdAt(1));
        assertEquals(100.0, compiled.parameterDefaultAt(1));
        assertEquals(-1, compiled.nativeParameterId(2));
        assertEquals(1, compiled.nativeParameterId(3));
        assertEquals(1, compiled.nativeObserverId(2));

        assertEquals(2, compiled.portCount());
        assertEquals(7, compiled.portIdAt(0));
        assertEquals(2, compiled.stableTerminalIdAt(0));
        assertEquals(1, compiled.nativeTerminalIdAt(0));
        assertEquals(2, compiled.portIdAt(1));
        assertEquals(1, compiled.stableTerminalIdAt(1));
        assertEquals(0, compiled.nativeTerminalIdAt(1));

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

        assertEquals(0, compiled.portCount());
        assertEquals(0, compiled.portDefinition().size());
    }

    private static DeviceDescriptorRegistry registry(
            MemberMapping parameters,
            MemberMapping terminals,
            MemberMapping observers
    ) {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        registry.register("test:resistance", Resistance.TYPE, parameters, terminals, observers);
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
