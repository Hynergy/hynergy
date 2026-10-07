package dev.hynergy.core.electricity;

import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.electricity.device.ElectricalDeviceSystem;
import dev.hynergy.core.electricity.device.LightbulbSystem;
import dev.hynergy.core.electricity.wire.WireSystem;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

final class ElectricalTickSystemOrderingTest {
    @Test
    void tickRunsAfterWireAndDeviceLifecycleSystems() {
        Set<Dependency<ChunkStore>> dependencies =
                new ElectricalTickSystem(null, null).getDependencies();

        assertAfter(dependencies, WireSystem.class);
        assertAfter(dependencies, ElectricalDeviceSystem.class);
        assertAfter(dependencies, LightbulbSystem.class);
    }

    private static void assertAfter(
            Set<Dependency<ChunkStore>> dependencies,
            Class<?> expectedSystemClass
    ) {
        for (Dependency<ChunkStore> dependency : dependencies) {
            if (dependency instanceof SystemDependency<?, ?> systemDependency
                    && systemDependency.getSystemClass() == expectedSystemClass) {
                assertEquals(Order.AFTER, dependency.getOrder());
                return;
            }
        }

        fail("Missing dependency on " + expectedSystemClass.getName());
    }
}
