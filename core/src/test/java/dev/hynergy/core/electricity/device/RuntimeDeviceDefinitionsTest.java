package dev.hynergy.core.electricity.device;

import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RuntimeDeviceDefinitionsTest {
    @Test
    void mappingGrowsReplacesAndClearsWithoutAffectingOtherIndexes() {
        RuntimeDeviceDefinitions definitions = new RuntimeDeviceDefinitions();
        CompiledDeviceConfig first = compiled(100.0);
        CompiledDeviceConfig replacement = compiled(220.0);
        CompiledDeviceConfig far = compiled(470.0);

        assertNull(definitions.get(-1));
        assertNull(definitions.get(40));

        definitions.set(0, first);
        definitions.set(40, far);

        assertSame(first, definitions.get(0));
        assertSame(far, definitions.get(40));

        definitions.set(0, replacement);
        assertSame(replacement, definitions.get(0));
        assertSame(far, definitions.get(40));

        definitions.clear(0);
        assertNull(definitions.get(0));
        assertSame(far, definitions.get(40));

        definitions.clear(400);
        assertSame(far, definitions.get(40));
    }

    @Test
    void negativeRuntimeIndexesAreRejectedForMutation() {
        RuntimeDeviceDefinitions definitions = new RuntimeDeviceDefinitions();
        CompiledDeviceConfig compiled = compiled(100.0);

        assertThrows(IllegalArgumentException.class, () -> definitions.set(-1, compiled));
        assertThrows(IllegalArgumentException.class, () -> definitions.clear(-1));
    }

    private static CompiledDeviceConfig compiled(double defaultValue) {
        DeviceDescriptorRegistry registry = new DeviceDescriptorRegistry();
        registry.register(
                "test:resistance",
                Resistance.TYPE,
                new MemberMapping(0),
                new MemberMapping(),
                new MemberMapping()
        );
        PortModule ports = new PortModule();
        PortDomain<ElectricalPortConnection> domain = ports.registerDomain("test:electrical");
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor = ports.registerStandard(
                "test:conductor",
                domain,
                ElectricalPortProfile.class,
                (first, second, geometry) -> ElectricalPortConnection.DIRECT
        );
        return new DeviceConfig(
                "test:device",
                "test:resistance",
                new DeviceParameterConfig[]{new DeviceParameterConfig(0, defaultValue)},
                new DevicePortConfig[0]
        ).compile(registry, conductor);
    }
}
