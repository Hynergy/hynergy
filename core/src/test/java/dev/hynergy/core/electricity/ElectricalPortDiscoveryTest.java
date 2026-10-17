package dev.hynergy.core.electricity;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import dev.hynergy.core.electricity.port.ElectricalPortConnection;
import dev.hynergy.core.electricity.port.ElectricalPortProfile;
import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortService;
import dev.hynergy.core.port.PortServiceTestAccess;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.core.port.layout.BlockPortDefinition;
import dev.hynergy.core.port.layout.PortDefinition;
import dev.hynergy.core.port.layout.PortOffset;
import dev.hynergy.core.port.layout.PortReach;
import dev.hynergy.core.port.world.PortWorldView;
import org.joml.Vector3i;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ElectricalPortDiscoveryTest {

    @ParameterizedTest(name = "target normal ({0}, {1}, {2}): {3} connections")
    @CsvSource({"-1, 0, 0, 1", "1, 0, 0, 0", "0, 0, 1, 0"})
    void onlyOpposingConductorFacesConnectDirectly(int normalX, int normalY, int normalZ, int expectedCount) {
        Fixture fixture = Fixture.create(
                1, 0, 0, RotationTuple.NONE,
                normalX, normalY, normalZ, RotationTuple.NONE
        );

        assertEquals(expectedCount == 0 ? List.of() : List.of(ElectricalPortConnection.DIRECT), fixture.discover());
    }

    @Test
    void rotatedConductorUsesRotatedFace() {
        RotationTuple rotation = RotationTuple.of(Rotation.Ninety, Rotation.None);
        Vector3i offset = new Vector3i(1, 0, 0);
        rotation.applyRotationTo(offset);

        Fixture fixture = Fixture.create(
                1, 0, 0, rotation,
                -1, 0, 0, rotation,
                offset.x(), offset.y(), offset.z()
        );

        assertEquals(List.of(ElectricalPortConnection.DIRECT), fixture.discover());
    }

    @Test
    void pluginStandardCanRegisterAdapterToBuiltInConductorWithoutChangingElectricalModule() {
        PortService ports = new PortService();

        PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor =
                registerConductorStandard(ports);

        PortDomain<ElectricalPortConnection> domain = conductor.domain();

        record PluginProfile(int kind) {
        }

        PortStandard<PluginProfile, ElectricalPortConnection> plugin =
                ports.registerStandard(
                        "plugin:connector",
                        domain,
                        PluginProfile.class,
                        (first, second, geometry) -> null
                );

        ports.registerAdapter(
                plugin,
                conductor,
                (pluginProfile, conductorProfile, geometry) ->
                        ElectricalPortConnection.DIRECT
        );
        PortServiceTestAccess.freeze(ports);
        ports.setBlockPorts(1, BlockPortDefinition.of(
                new PortDefinition<>(
                        0,
                        PortOffset.ZERO,
                        PortReach.single(1, 0, 0),
                        plugin,
                        new PluginProfile(1)
                )
        ));
        ports.setBlockPorts(
                2,
                BlockPortDefinition.of(
                        conductorPort(conductor, 1, -1, 0, 0)
                )
        );
        TestWorld world = new TestWorld(ports);
        world.put(0, 0, 0, 1, RotationTuple.NONE);
        world.put(1, 0, 0, 2, RotationTuple.NONE);
        List<ElectricalPortConnection> results = new ArrayList<>();

        ports.discovery().discover(
                world, 0, 0, 0, 0, domain,
                (x, y, z, portId, result, sourceFirst) -> results.add(result)
        );

        assertEquals(List.of(ElectricalPortConnection.DIRECT), results);
    }

    @Test
    void profileRequiresOneUnitCardinalNormal() {
        assertThrows(IllegalArgumentException.class, () -> new ElectricalPortProfile(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ElectricalPortProfile(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ElectricalPortProfile(2, 0, 0));
    }


    private record Fixture(PortService ports, PortDomain<ElectricalPortConnection> domain, TestWorld world) {

        static Fixture create(
                int sourceNormalX,
                int sourceNormalY,
                int sourceNormalZ,
                RotationTuple sourceRotation,
                int targetNormalX,
                int targetNormalY,
                int targetNormalZ,
                RotationTuple targetRotation
        ) {
            return create(
                    sourceNormalX, sourceNormalY, sourceNormalZ, sourceRotation,
                    targetNormalX, targetNormalY, targetNormalZ, targetRotation,
                    1, 0, 0
            );
        }

        static Fixture create(
                int sourceNormalX,
                int sourceNormalY,
                int sourceNormalZ,
                RotationTuple sourceRotation,
                int targetNormalX,
                int targetNormalY,
                int targetNormalZ,
                RotationTuple targetRotation,
                int targetX,
                int targetY,
                int targetZ
        ) {
            PortService ports = new PortService();

            PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor =
                    registerConductorStandard(ports);

            PortServiceTestAccess.freeze(ports);

            ports.setBlockPorts(
                    1,
                    BlockPortDefinition.of(
                            conductorPort(
                                    conductor,
                                    0,
                                    sourceNormalX,
                                    sourceNormalY,
                                    sourceNormalZ
                            )
                    )
            );

            ports.setBlockPorts(
                    2,
                    BlockPortDefinition.of(
                            conductorPort(
                                    conductor,
                                    1,
                                    targetNormalX,
                                    targetNormalY,
                                    targetNormalZ
                            )
                    )
            );

            TestWorld world = new TestWorld(ports);
            world.put(0, 0, 0, 1, sourceRotation);
            world.put(targetX, targetY, targetZ, 2, targetRotation);

            return new Fixture(
                    ports,
                    conductor.domain(),
                    world
            );
        }

        List<ElectricalPortConnection> discover() {
            List<ElectricalPortConnection> results = new ArrayList<>();
            ports.discovery().discover(
                    world, 0, 0, 0, 0, domain,
                    (x, y, z, targetPortId, result, sourceFirst) -> results.add(result)
            );
            return results;
        }
    }

    private static PortStandard<ElectricalPortProfile, ElectricalPortConnection>
    registerConductorStandard(PortService ports) {
        PortDomain<ElectricalPortConnection> domain =
                ports.registerDomain("test:electrical");

        return ports.registerStandard(
                "test:electrical/conductor",
                domain,
                ElectricalPortProfile.class,
                ElectricityPlugin::resolveConductorConnection
        );
    }

    private static PortDefinition<ElectricalPortProfile, ElectricalPortConnection>
    conductorPort(
            PortStandard<ElectricalPortProfile, ElectricalPortConnection> standard,
            int localId,
            int normalX,
            int normalY,
            int normalZ
    ) {
        return new PortDefinition<>(
                localId,
                PortOffset.ZERO,
                PortReach.single(normalX, normalY, normalZ),
                standard,
                new ElectricalPortProfile(normalX, normalY, normalZ)
        );
    }

    private static final class TestWorld implements PortWorldView {
        private final PortService module;

        private final Map<Position, Integer> ids = new HashMap<>();
        private final Map<Position, RotationTuple> rotations = new HashMap<>();

        TestWorld(PortService module) {
            this.module = module;
        }

        void put(int x, int y, int z, int blockTypeId, RotationTuple rotation) {
            Position position = new Position(x, y, z);
            ids.put(position, blockTypeId);
            rotations.put(position, rotation);
        }

        @Override
        public @Nullable BlockPortDefinition portsAt(int x, int y, int z) {
            return module.blockPorts(ids.getOrDefault(new Position(x, y, z), -1));
        }

        @Override
        public RotationTuple rotation(int x, int y, int z) {
            return rotations.getOrDefault(new Position(x, y, z), RotationTuple.NONE);
        }
    }

    private record Position(int x, int y, int z) {
    }
}
