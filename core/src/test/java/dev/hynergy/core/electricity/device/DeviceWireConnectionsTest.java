package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.*;
import dev.hynergy.electrical.*;
import dev.hynergy.electrical.primitives.passive.Resistance;
import dev.hynergy.electrical.primitives.sources.VoltageSource;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceWireConnectionsTest {
    @Test
    void deviceFirstDiscoveryMapsTargetPortToNativeTerminal() {
        try (Fixture fixture = new Fixture()) {
            fixture.discoverWire(fixture.positive, -1);
            fixture.discoverWire(fixture.negative, 1);
            fixture.assertCurrent(0.5);
        }
    }

    @Test
    void wireFirstDiscoveryUsesTheSameAttachments() {
        try (Fixture fixture = new Fixture(false)) {
            var dedup = new DeviceWireConnections.AttachmentDedup();
            for (int index = 0; index < fixture.compiled.portCount(); index++) {
                int terminal = fixture.compiled.nativeTerminalIdAt(index);
                fixture.ports.discovery().discover(fixture, 0, 0, 0, fixture.compiled.portIdAt(index),
                        fixture.domain, (x, y, z, portId, connection, sourceFirst) ->
                                DeviceWireConnections.attachIfNew(fixture.resistance, terminal,
                                        x == -1 ? fixture.positive : fixture.negative, dedup));
            }
            fixture.assertCurrent(0.5);
        }
    }

    @Test
    void replacingWireRestoresTheExistingDeviceConnection() {
        try (Fixture fixture = new Fixture()) {
            fixture.discoverWire(fixture.positive, -1);
            fixture.discoverWire(fixture.negative, 1);
            fixture.assertCurrent(0.5);

            fixture.positive.destroy();
            Wire replacement = fixture.system.createWire();
            fixture.source.attachTerminal(0, replacement);
            fixture.discoverWire(replacement, -1);
            fixture.assertCurrent(0.5);
            fixture.source.setParameter(0, 8.0);
            fixture.assertCurrent(0.8);
        }
    }

    @Test
    void aliasesForOneTerminalAttachOnlyOnceAndDetachCompletely() {
        try (Fixture fixture = new Fixture()) {
            var dedup = new DeviceWireConnections.AttachmentDedup();
            assertTrue(DeviceWireConnections.attachPortIfNew(fixture.component, 7, fixture.positive, dedup));
            assertFalse(DeviceWireConnections.attachPortIfNew(fixture.component, 7, fixture.positive, dedup));
            assertFalse(DeviceWireConnections.attachPortIfNew(fixture.component, 19, fixture.positive, dedup));
            assertTrue(DeviceWireConnections.attachPortIfNew(fixture.component, 3, fixture.negative, dedup));
            fixture.assertCurrent(0.5);

            fixture.resistance.detachTerminal(0, fixture.positive);
            fixture.assertCurrent(0.0);
        }
    }

    @Test
    void discoveryAttachesDifferentDevicesWithTheSameTerminalIndex() {
        try (Fixture fixture = new Fixture()) {
            DeviceComponent second = new DeviceComponent("test:resistance");
            ElectricalDeviceSystem.bindDevice(second, fixture.compiled, fixture.system, () -> {
            });
            Device secondResistance = second.device();
            ArrayList<Double> currents = new ArrayList<>();
            secondResistance.observe(1, (status, value) -> currents.add(value));
            var dedup = new DeviceWireConnections.AttachmentDedup();
            for (DeviceComponent component : new DeviceComponent[]{fixture.component, second}) {
                assertTrue(DeviceWireConnections.attachPortIfNew(component, 7, fixture.positive, dedup));
                assertTrue(DeviceWireConnections.attachPortIfNew(component, 3, fixture.negative, dedup));
            }
            fixture.assertCurrent(0.5);
            assertEquals(0.5, currents.getLast(), 1e-9);
        }
    }

    @Test
    void unavailableNeighborHandlesDoNotConsumeDedupEntries() {
        try (Fixture fixture = new Fixture()) {
            var dedup = new DeviceWireConnections.AttachmentDedup();
            assertFalse(DeviceWireConnections.attachPortIfNew(null, 7, fixture.positive, dedup));
            assertFalse(DeviceWireConnections.attachPortIfNew(fixture.component, 7, null, dedup));
            assertFalse(DeviceWireConnections.attachPortIfNew(fixture.component, 99, fixture.positive, dedup));
            ElectricalDeviceSystem.unloadDevice(fixture.component);
            assertFalse(DeviceWireConnections.attachPortIfNew(fixture.component, 7, fixture.positive, dedup));
            assertFalse(ElectricalDeviceSystem.bindDevice(
                    fixture.component, fixture.compiled, fixture.system, () -> {
                    }
            ).created());
            assertTrue(DeviceWireConnections.attachPortIfNew(fixture.component, 7, fixture.positive, dedup));
            assertTrue(DeviceWireConnections.attachPortIfNew(fixture.component, 3, fixture.negative, dedup));
            fixture.assertCurrent(0.5);
        }
    }

    @Test
    void restoringRetainedDeviceKeepsItsTopologyWithoutDiscovery() {
        try (Fixture fixture = new Fixture()) {
            fixture.discoverWire(fixture.positive, -1);
            fixture.discoverWire(fixture.negative, 1);
            fixture.assertCurrent(0.5);
            DeviceId retained = fixture.component.getDeviceId();
            ElectricalDeviceSystem.unloadDevice(fixture.component);
            assertFalse(ElectricalDeviceSystem.bindDevice(
                    fixture.component, fixture.compiled, fixture.system, () -> {
                    }
            ).created());
            assertEquals(retained, fixture.component.getDeviceId());
            fixture.source.setParameter(0, 8.0);
            fixture.assertCurrent(0.8);
        }
    }

    @Test
    void restoringRetainedWireKeepsItsTopologyWithoutDiscovery() {
        try (Fixture fixture = new Fixture()) {
            fixture.discoverWire(fixture.positive, -1);
            fixture.discoverWire(fixture.negative, 1);
            fixture.assertCurrent(0.5);
            Wire restored = fixture.system.resolveWire(fixture.positive.id());
            assertEquals(fixture.positive.id(), restored.id());
            fixture.source.setParameter(0, 8.0);
            fixture.assertCurrent(0.8);
        }
    }

    private static final class Fixture implements AutoCloseable, PortWorldView {
        private final ElectricalRuntime runtime = ElectricalRuntime.create();
        private final ElectricalSystem system = runtime.createSystem(20);
        private final PortModule ports = new PortModule();
        private final PortDomain<ElectricalPortConnection> domain = ports.registerDomain("test:electrical");
        private final CompiledDeviceConfig compiled;
        private final DeviceComponent component = new DeviceComponent("test:resistance");
        private final Device resistance;
        private final Device source = system.create(VoltageSource.TYPE);
        private final Wire positive;
        private final Wire negative;
        private final ArrayList<Double> currents = new ArrayList<>();
        private final BlockPortDefinition leftWirePorts;
        private final BlockPortDefinition rightWirePorts;

        private Fixture() {
            this(true);
        }

        private Fixture(boolean deviceFirst) {
            var standard = ports.registerStandard("test:conductor", domain, ElectricalPortProfile.class,
                    (first, second, geometry) ->
                            first.normalX() == -second.normalX() ? ElectricalPortConnection.DIRECT : null);
            PortModuleTestAccess.freeze(ports);
            var descriptors = new DeviceDescriptorRegistry();
            descriptors.register("test:resistance", Resistance.TYPE,
                    new MemberMapping(-1, -1, -1, -1, 0),
                    new MemberMapping(-1, 1, -1, -1, 0), new MemberMapping(0, 1));
            compiled = new DeviceConfig("test:resistance", "test:resistance",
                    new DeviceParameterConfig[]{new DeviceParameterConfig(4, 10.0)},
                    new DevicePortConfig[]{
                            new DevicePortConfig(3, 1, null, new Vector3i(1, 0, 0)),
                            new DevicePortConfig(7, 4, null, new Vector3i(-1, 0, 0)),
                            new DevicePortConfig(19, 4, null, new Vector3i(-1, 0, 0))
                    }).compile(descriptors, standard);
            leftWirePorts = BlockPortDefinition.of(new PortDefinition<>(
                    12, PortOffset.ZERO, PortReach.single(1, 0, 0), standard, new ElectricalPortProfile(1, 0, 0)));
            rightWirePorts = BlockPortDefinition.of(new PortDefinition<>(
                    12, PortOffset.ZERO, PortReach.single(-1, 0, 0), standard, new ElectricalPortProfile(-1, 0, 0)));
            if (deviceFirst) {
                ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
                });
            }
            positive = system.createWire();
            negative = system.createWire();
            if (!deviceFirst) {
                ElectricalDeviceSystem.bindDevice(component, compiled, system, () -> {
                });
            }
            resistance = component.device();
            source.setParameter(0, 5.0);
            source.attachTerminal(0, positive);
            source.attachTerminal(1, negative);
            resistance.observe(1, (status, value) -> {
                assertEquals(ObservationStatus.AVAILABLE, status);
                currents.add(value);
            });
        }

        private void discoverWire(Wire wire, int x) {
            var dedup = new DeviceWireConnections.AttachmentDedup();
            ports.discovery().discover(this, x, 0, 0, 12, domain,
                    (tx, ty, tz, targetPortId, connection, sourceFirst) ->
                            DeviceWireConnections.attachPortIfNew(component, targetPortId, wire, dedup));
        }

        private void assertCurrent(double expected) {
            system.tick();
            assertFalse(currents.isEmpty());
            assertEquals(expected, currents.getLast(), 1e-9);
        }

        @Override
        public RotationTuple rotation(int x, int y, int z) {
            return RotationTuple.NONE;
        }

        @Override
        public BlockPortDefinition portsAt(int x, int y, int z) {
            if (y != 0 || z != 0) {
                return null;
            }
            return switch (x) {
                case -1 -> leftWirePorts;
                case 0 -> compiled.portDefinition();
                case 1 -> rightWirePorts;
                default -> null;
            };
        }

        @Override
        public void close() {
            system.close();
            runtime.close();
        }
    }
}
