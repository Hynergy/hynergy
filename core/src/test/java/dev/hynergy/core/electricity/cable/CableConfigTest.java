package dev.hynergy.core.electricity.cable;

import com.hypixel.hytale.codec.ExtraInfo;
import dev.hynergy.core.electricity.port.ElectricalPortConnection;
import dev.hynergy.core.electricity.port.ElectricalPortProfile;
import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.core.port.layout.BlockPortDefinition;
import dev.hynergy.core.port.layout.PortDefinition;
import dev.hynergy.core.port.layout.PortOffset;
import dev.hynergy.core.port.layout.PortReach;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CableConfigTest {

    @Test
    void buildPortDefinitionCompilesIdsAnchorsReachAndProfile() {
        CableConfig config = CableConfig.CODEC.decode(
                BsonDocument.parse("""
                        {
                          "Ports": [
                            {
                              "Id": 3,
                              "Normal": { "X": 1, "Y": 0, "Z": 0 }
                            },
                            {
                              "Id": 7,
                              "Anchor": { "X": 0, "Y": 1, "Z": 0 },
                              "Normal": { "X": 0, "Y": 1, "Z": 0 }
                            }
                          ]
                        }
                        """),
                ExtraInfo.THREAD_LOCAL.get()
        );
        PortModule ports = new PortModule();
        PortDomain<ElectricalPortConnection> domain = ports.registerDomain("test:electrical");
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductor = ports.registerStandard(
                "test:conductor",
                domain,
                ElectricalPortProfile.class,
                (first, second, geometry) -> ElectricalPortConnection.DIRECT
        );

        BlockPortDefinition definition = config.buildPortDefinition(conductor);
        PortDefinition<?, ?> first = definition.port(3);
        PortDefinition<?, ?> second = definition.port(7);

        assertNotNull(first);
        assertEquals(PortOffset.ZERO, first.anchor());
        assertEquals(PortReach.single(1, 0, 0), first.reach());
        assertSame(conductor, first.standard());
        assertEquals(new ElectricalPortProfile(1, 0, 0), first.profile());

        assertNotNull(second);
        assertEquals(new PortOffset(0, 1, 0), second.anchor());
        assertEquals(PortReach.single(0, 1, 0), second.reach());
        assertSame(conductor, second.standard());
        assertEquals(new ElectricalPortProfile(0, 1, 0), second.profile());
    }
}
