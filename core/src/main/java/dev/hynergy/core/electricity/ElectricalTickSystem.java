package dev.hynergy.core.electricity;

import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.electricity.device.ElectricalDeviceSystem;
import dev.hynergy.core.electricity.wire.WireSystem;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.ElectricalSystem;

import java.util.Set;

final class ElectricalTickSystem extends TickingSystem<ChunkStore> {
    private static final Set<Dependency<ChunkStore>> DEPENDENCIES = Set.of(
            new SystemDependency<>(Order.AFTER, WireSystem.class),
            new SystemDependency<>(Order.AFTER, ElectricalDeviceSystem.class)
    );

    private final ElectricalRuntime runtime;
    private final ResourceType<ChunkStore, ElectricalSystemResource> resourceType;

    public ElectricalTickSystem(
            ElectricalRuntime runtime,
            ResourceType<ChunkStore, ElectricalSystemResource> resourceType
    ) {
        this.runtime = runtime;
        this.resourceType = resourceType;
    }

    @Override
    public Set<Dependency<ChunkStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public void tick(float v, int i, Store<ChunkStore> store) {
        store.assertThread();

        World world = store.getExternalData().getWorld();

        world.debugAssertInTickingThread();

        ElectricalSystemResource resource = store.getResource(resourceType);
        ElectricalSystem system = resource.getOrCreate(runtime, world);

        system.tick();
    }
}
