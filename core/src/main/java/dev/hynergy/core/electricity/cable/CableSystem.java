package dev.hynergy.core.electricity.cable;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.electricity.device.runtime.DeviceComponent;
import dev.hynergy.core.electricity.device.runtime.DeviceWireConnections;
import dev.hynergy.core.electricity.port.ElectricalPortConnection;
import dev.hynergy.core.electricity.system.ElectricalSystemResource;
import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortService;
import dev.hynergy.core.port.connection.PortConnectionConsumer;
import dev.hynergy.core.port.layout.BlockPortDefinition;
import dev.hynergy.core.port.world.HytalePortWorldView;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.ElectricalSystem;
import dev.hynergy.electrical.Wire;
import dev.hynergy.electrical.WireId;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Vector3i;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Manages the ECS lifecycle of electrical cables.
 *
 * <p>Components originating from the current electrical system resolve their
 * retained native wires. Components saved by a previous system reconstruct new
 * wires and discover their connections again.</p>
 */
public final class CableSystem extends RefSystem<ChunkStore> {
    private final ElectricalRuntime runtime;
    private final ResourceType<ChunkStore, ElectricalSystemResource> electricalSystemResourceType;
    private final ComponentType<ChunkStore, CableComponent> wireComponentType;
    private final ComponentType<ChunkStore, DeviceComponent> deviceComponentType;
    private final PortService portService;
    private final PortDomain<ElectricalPortConnection> electricalPortDomain;
    private final Query<ChunkStore> query;

    private final ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoComponentType =
            BlockModule.BlockStateInfo.getComponentType();

    private final ThreadLocal<ConnectionScratch> connectionScratch =
            ThreadLocal.withInitial(ConnectionScratch::new);

    public CableSystem(
            ElectricalRuntime runtime,
            ResourceType<ChunkStore, ElectricalSystemResource> electricalSystemResourceType,
            ComponentType<ChunkStore, CableComponent> wireComponentType,
            ComponentType<ChunkStore, DeviceComponent> deviceComponentType,
            PortService portService,
            PortDomain<ElectricalPortConnection> electricalPortDomain
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.electricalSystemResourceType =
                Objects.requireNonNull(electricalSystemResourceType, "electricalSystemResourceType");
        this.wireComponentType = Objects.requireNonNull(wireComponentType, "wireComponentType");
        this.deviceComponentType = Objects.requireNonNull(deviceComponentType, "deviceComponentType");
        this.portService = Objects.requireNonNull(portService, "portService");
        this.electricalPortDomain = Objects.requireNonNull(electricalPortDomain, "electricalPortDomain");
        this.query = Query.and(wireComponentType, blockStateInfoComponentType);
    }

    @Override
    public void onEntityAdded(
            Ref<ChunkStore> ref,
            AddReason reason,
            Store<ChunkStore> store,
            CommandBuffer<ChunkStore> commandBuffer
    ) {
        store.assertThread();

        World world = store.getExternalData().getWorld();
        world.debugAssertInTickingThread();

        CableComponent component = commandBuffer.getComponent(ref, wireComponentType);
        BlockModule.BlockStateInfo blockStateInfo =
                commandBuffer.getComponent(ref, blockStateInfoComponentType);

        if (component == null || blockStateInfo == null) {
            throw new IllegalStateException(
                    "CableSystem matched an entity without its required components"
            );
        }

        BlockPortDefinition definition = resolvePortDefinition(blockStateInfo, commandBuffer);
        ElectricalSystem system = store.getResource(electricalSystemResourceType).getOrCreate(runtime, world);
        BindResult result = bindWire(component, system);

        if (result.created()) {
            try {
                connectNewWire(
                        Objects.requireNonNull(component.getWire(), "New cable is not runtime-bound"),
                        definition,
                        blockStateInfo,
                        store,
                        commandBuffer
                );
            } catch (RuntimeException | Error failure) {
                rollbackCreatedWire(component, failure);
                throw failure;
            }
        }

        if (result.persistenceChanged()) {
            blockStateInfo.markNeedsSaving(commandBuffer);
        }
    }

    @Override
    public void onEntityRemove(
            Ref<ChunkStore> ref,
            RemoveReason reason,
            Store<ChunkStore> store,
            CommandBuffer<ChunkStore> commandBuffer
    ) {
        store.assertThread();

        World world = store.getExternalData().getWorld();
        world.debugAssertInTickingThread();

        CableComponent component = commandBuffer.getComponent(ref, wireComponentType);
        if (component == null) {
            throw new IllegalStateException(
                    "CableSystem matched an entity without a CableComponent"
            );
        }

        if (reason == RemoveReason.UNLOAD) {
            unloadWire(component);
            return;
        }

        ElectricalSystem system = store.getResource(electricalSystemResourceType).getOrCreate(runtime, world);
        destroyWire(component, system);
    }

    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }

    private BlockPortDefinition resolvePortDefinition(
            BlockModule.BlockStateInfo blockStateInfo,
            CommandBuffer<ChunkStore> commandBuffer
    ) {
        Ref<ChunkStore> sectionRef = blockStateInfo.getSectionRef();
        if (!sectionRef.isValid()) {
            throw new IllegalStateException("Cable block section reference is no longer valid");
        }

        BlockSection blockSection = commandBuffer.getComponent(sectionRef, BlockSection.getComponentType());
        if (blockSection == null) {
            throw new IllegalStateException("Cable block section is unavailable");
        }

        int blockIndex = blockStateInfo.getIndex();
        int blockTypeIndex = blockSection.get(
                ChunkUtil.xFromIndex(blockIndex),
                ChunkUtil.yFromIndex(blockIndex),
                ChunkUtil.zFromIndex(blockIndex)
        );

        BlockPortDefinition definition = portService.blockPorts(blockTypeIndex);
        if (definition == null) {
            throw new IllegalStateException(
                    "Wire block type " + blockTypeIndex + " has no registered port definition"
            );
        }

        
        return definition;
    }

    private void connectNewWire(
            Wire sourceWire,
            BlockPortDefinition definition,
            BlockModule.BlockStateInfo blockStateInfo,
            Store<ChunkStore> store,
            CommandBuffer<ChunkStore> commandBuffer
    ) {
        if (definition.size() == 0) {
            return;
        }

        ConnectionScratch scratch = connectionScratch.get();
        if (!blockStateInfo.fillWorldPos(commandBuffer, scratch.sourcePosition)) {
            return;
        }

        ChunkStore chunkStore = store.getExternalData();
        HytalePortWorldView worldView =
                new HytalePortWorldView(chunkStore.getWorld(), portService::blockPorts);

        scratch.begin(sourceWire, chunkStore, commandBuffer, wireComponentType, deviceComponentType);
        try {
            for (int index = 0; index < definition.size(); index++) {
                portService.discovery().discover(
                        worldView,
                        scratch.sourcePosition.x,
                        scratch.sourcePosition.y,
                        scratch.sourcePosition.z,
                        definition.portAt(index).localId(),
                        electricalPortDomain,
                        scratch
                );
            }
        } finally {
            scratch.end();
        }
    }

    static BindResult bindWire(CableComponent component, ElectricalSystem system) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(system, "system");

        if (component.getWire() != null) {
            throw new IllegalStateException("CableComponent is already runtime-bound");
        }

        WireId persistedId = component.getWireId();
        boolean created = persistedId == null
                || !system.getUuid().equals(component.getSystemId());

        Wire wire = created
                ? system.createWire()
                : system.resolveWire(persistedId);

        if (created) {
            component.setWireId(wire.id());
            component.setSystemId(system.getUuid());
        }
        component.bindRuntime(wire);

        return new BindResult(created, created);
    }

    static void unloadWire(CableComponent component) {
        Objects.requireNonNull(component, "component").clearRuntime();
    }

    static void destroyWire(CableComponent component, ElectricalSystem system) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(system, "system");

        Wire wire = component.getWire();
        WireId persistedId = component.getWireId();
        if (wire == null && persistedId != null
                && system.getUuid().equals(component.getSystemId())) {
            wire = system.resolveWire(persistedId);
        }

        if (wire != null) {
            wire.destroy();
        }

        component.clearRuntime();
        component.setWireId(null);
        component.setSystemId(null);
    }

    private static void rollbackCreatedWire(CableComponent component, Throwable failure) {
        Wire wire = component.getWire();
        if (wire != null) {
            try {
                wire.destroy();
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }

        component.clearRuntime();
        component.setWireId(null);
        component.setSystemId(null);
    }

    record BindResult(boolean created, boolean persistenceChanged) {
    }

    private static final class ConnectionScratch
            implements PortConnectionConsumer<ElectricalPortConnection> {
        private final Vector3i sourcePosition = new Vector3i();
        private final LongOpenHashSet connectedWires = new LongOpenHashSet(8);
        private final DeviceWireConnections.AttachmentDedup attachedTerminals =
                new DeviceWireConnections.AttachmentDedup();

        private boolean inUse;
        private @Nullable Wire sourceWire;
        private @Nullable ChunkStore chunkStore;
        private @Nullable CommandBuffer<ChunkStore> commandBuffer;
        private @Nullable ComponentType<ChunkStore, CableComponent> wireComponentType;
        private @Nullable ComponentType<ChunkStore, DeviceComponent> deviceComponentType;

        void begin(
                Wire sourceWire,
                ChunkStore chunkStore,
                CommandBuffer<ChunkStore> commandBuffer,
                ComponentType<ChunkStore, CableComponent> wireComponentType,
                ComponentType<ChunkStore, DeviceComponent> deviceComponentType
        ) {
            if (inUse) {
                throw new IllegalStateException("Cable connection discovery is not reentrant");
            }

            inUse = true;
            this.sourceWire = sourceWire;
            this.chunkStore = chunkStore;
            this.commandBuffer = commandBuffer;
            this.wireComponentType = wireComponentType;
            this.deviceComponentType = deviceComponentType;
            connectedWires.clear();
            attachedTerminals.clear();
            connectedWires.add(sourceWire.id().packed());
        }

        void end() {
            connectedWires.clear();
            attachedTerminals.clear();
            sourceWire = null;
            chunkStore = null;
            commandBuffer = null;
            wireComponentType = null;
            deviceComponentType = null;
            inUse = false;
        }

        @Override
        public void accept(
                int targetX,
                int targetY,
                int targetZ,
                int targetPortId,
                ElectricalPortConnection connection,
                boolean sourceIsResolverFirst
        ) {
            if (connection != ElectricalPortConnection.DIRECT) {
                return;
            }

            ChunkStore chunkStore = Objects.requireNonNull(this.chunkStore, "connection chunkStore");
            CommandBuffer<ChunkStore> commandBuffer =
                    Objects.requireNonNull(this.commandBuffer, "connection commandBuffer");
            ComponentType<ChunkStore, CableComponent> wireComponentType =
                    Objects.requireNonNull(this.wireComponentType, "connection wireComponentType");
            ComponentType<ChunkStore, DeviceComponent> deviceComponentType =
                    Objects.requireNonNull(this.deviceComponentType, "connection deviceComponentType");
            Wire sourceWire = Objects.requireNonNull(this.sourceWire, "connection sourceWire");

            Ref<ChunkStore> targetSectionRef =
                    chunkStore.getChunkSectionReferenceAtBlock(targetX, targetY, targetZ);
            if (targetSectionRef == null || !targetSectionRef.isValid()) {
                return;
            }

            Ref<ChunkStore> targetRef = BlockModule.getBlockEntity(
                    commandBuffer, targetSectionRef, targetX, targetY, targetZ
            );
            if (targetRef == null) {
                return;
            }

            CableComponent targetCable = commandBuffer.getComponent(targetRef, wireComponentType);
            if (targetCable != null) {
                Wire targetWire = targetCable.getWire();
                if (targetWire != null && connectedWires.add(targetWire.id().packed())) {
                    sourceWire.connect(targetWire);
                }
            }

            DeviceComponent targetDevice = commandBuffer.getComponent(targetRef, deviceComponentType);
            DeviceWireConnections.attachPortIfNew(
                    targetDevice, targetPortId, sourceWire, attachedTerminals
            );
        }
    }
}
