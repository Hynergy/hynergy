package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.BlockPortDefinition;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.core.port.PortOffset;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.ParameterConstraints;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import org.joml.Vector3i;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceConfigCompilationTest {
    private ElectricalRuntime runtime;
    private DeviceType type;
    private DeviceRegistry registry;

    @BeforeEach
    void setup() {
        runtime = ElectricalRuntime.create();
        type = DeviceType.define(b -> {
            var p = b.terminal(1, "positive");
            var n = b.terminal(2, "negative");
            var first = b.parameter(1, "first", ParameterConstraints.positiveFinite());
            var second = b.parameter(3, "second", ParameterConstraints.positiveFinite());
            var r1 = b.element(PrimitiveDeviceTypes.RESISTANCE, e -> {
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(0), p);
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(1), n);
                e.bind(PrimitiveDeviceTypes.RESISTANCE.parameter(0), first);
            });
            var r2 = b.element(PrimitiveDeviceTypes.RESISTANCE, e -> {
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(0), p);
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(1), n);
                e.bind(PrimitiveDeviceTypes.RESISTANCE.parameter(0), second);
            });
            b.childObserver(0, "first_current", r1, PrimitiveDeviceTypes.RESISTANCE.observer(1));
            b.childObserver(2, "second_current", r2, PrimitiveDeviceTypes.RESISTANCE.observer(1));
        });
        registry = new DeviceRegistry(runtime);
        registry.register("test:resistance", type);
    }

    @AfterEach
    void close() {
        runtime.close();
    }

    static PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard() {
        var ports = new PortModule();
        var domain = ports.<ElectricalPortConnection>registerDomain("test:electrical");
        return ports.registerStandard("test:conductor", domain, ElectricalPortProfile.class, (a, b, g) -> ElectricalPortConnection.DIRECT);
    }

    DeviceConfig config(DeviceParameterConfig[] parameters, DevicePortConfig[] ports) {
        return new DeviceConfig("test:device", "test:resistance", parameters, ports);
    }

    @Test
    void compileResolvesSparseMembersAndPreservesPhysicalPorts() {
        var compiled = config(new DeviceParameterConfig[]{new DeviceParameterConfig(3, 470), new DeviceParameterConfig(1, 100)},
                new DevicePortConfig[]{new DevicePortConfig(7, 2, new Vector3i(1, 2, 3), new Vector3i(0, 1, 0)),
                        new DevicePortConfig(2, 1, null, new Vector3i(1, 0, 0))}).compile(registry, conductorStandard());
        assertSame(registry.require("test:resistance"), compiled.registration());
        assertSame(type.parameter(3), compiled.parameters().get(0).parameter());
        assertEquals(470, compiled.parameters().get(0).defaultValue());
        assertSame(type.parameter(1), compiled.parameters().get(1).parameter());
        assertEquals(100, compiled.parameters().get(1).defaultValue());
        assertNull(compiled.declaredParameter(2));
        assertSame(type.terminal(2), compiled.ports().get(0).terminal());
        assertEquals(7, compiled.ports().get(0).portId());
        assertEquals(2, compiled.ports().get(1).portId());
        assertEquals(new PortOffset(1, 2, 3), compiled.portDefinition().portAt(0).anchor());
        assertEquals(new ElectricalPortProfile(0, 1, 0), compiled.portDefinition().portAt(0).profile());
        assertEquals(PortOffset.ZERO, compiled.portDefinition().portAt(1).anchor());
    }

    @Test
    void invalidConfigsDoNotCreateDevicesOrPoisonRuntime() {
        for (double v : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> config(new DeviceParameterConfig[]{new DeviceParameterConfig(1, v)}, null).compile(registry, conductorStandard()));
        assertThrows(IllegalArgumentException.class, () -> config(new DeviceParameterConfig[]{new DeviceParameterConfig(2, 10)}, null).compile(registry, conductorStandard()));
        assertThrows(IllegalArgumentException.class, () -> config(new DeviceParameterConfig[]{new DeviceParameterConfig(1, 10), new DeviceParameterConfig(1, 20)}, null).compile(registry, conductorStandard()));
        assertThrows(IllegalArgumentException.class, () -> config(null, new DevicePortConfig[]{new DevicePortConfig(1, 0, null, new Vector3i(1, 0, 0))}).compile(registry, conductorStandard()));
        assertThrows(IllegalArgumentException.class, () -> config(null, new DevicePortConfig[]{new DevicePortConfig(1, 1, null, new Vector3i(1, 0, 0)), new DevicePortConfig(1, 2, null, new Vector3i(-1, 0, 0))}).compile(registry, conductorStandard()));
        assertThrows(IllegalArgumentException.class, () -> new DeviceConfig("test:x", "test:unknown", null, null).compile(registry, conductorStandard()));
        assertDoesNotThrow(() -> config(new DeviceParameterConfig[]{new DeviceParameterConfig(1, 100)}, null)
                .compile(registry, conductorStandard()));
        try (var system = runtime.createSystem(20)) {
            assertEquals(1, system.create(type).id().value());
        }
    }

    @Test
    void multiplePhysicalPortsCanShareOneTerminalAndDefaultsCanBeOmitted() {
        var compiled = config(null, new DevicePortConfig[]{new DevicePortConfig(9, 1, null, new Vector3i(1, 0, 0)), new DevicePortConfig(12, 1, null, new Vector3i(-1, 0, 0))}).compile(registry, conductorStandard());
        assertTrue(compiled.parameters().isEmpty());
        assertSame(compiled.ports().get(0).terminal(), compiled.ports().get(1).terminal());
        assertSame(type.parameter(1), compiled.declaredParameter(1));
        assertEquals(0, config(null, null).compile(registry, conductorStandard()).portDefinition().size());
    }

    @Test
    void compiledBindingsRemainImmutableWhenSourceListsChange() {
        var parameters = new java.util.ArrayList<CompiledDeviceConfig.ParameterBinding>();
        parameters.add(new CompiledDeviceConfig.ParameterBinding(type.parameter(1), 100));
        var compiled = new CompiledDeviceConfig(registry.require("test:resistance"), parameters, java.util.List.of(), BlockPortDefinition.of());
        parameters.clear();
        assertEquals(100, compiled.parameter(1).defaultValue());
        assertThrows(UnsupportedOperationException.class, () -> compiled.parameters().clear());
    }
}
