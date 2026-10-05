package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalSystemResource;
import dev.hynergy.core.electricity.wire.WireComponent;
import dev.hynergy.core.port.HytalePortWorldView;
import dev.hynergy.core.port.PortConnectionConsumer;
import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.electrical.Device;
import dev.hynergy.electrical.DeviceId;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.ElectricalSystem;
import org.joml.Vector3i;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Owns the ECS lifecycle of generic electrical devices.
 *
 * <p>Persisted {@link DeviceId}s decide whether an entity creates a new native
 * device or resolves an existing one. Restored devices synchronize configured
 * parameter intent but deliberately do not rediscover native topology.</p>
 */
public final class ElectricalDeviceSystem extends RefSystem<ChunkStore> {
    private final ElectricalRuntime runtime;
    private final ResourceType<ChunkStore, ElectricalSystemResource> electricalSystemResourceType;
    private final ComponentType<ChunkStore, DeviceComponent> deviceComponentType;
    private final ComponentType<ChunkStore, WireComponent> wireComponentType;
    private final RuntimeDeviceDefinitions runtimeDefinitions;
    private final PortModule portModule;
    private final PortDomain<ElectricalPortConnection> electricalPortDomain;
    private final Query<ChunkStore> query;

    private final ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoComponentType =
            BlockModule.BlockStateInfo.getComponentType();

    private final ThreadLocal<AttachmentScratch> attachmentScratch =
            ThreadLocal.withInitial(AttachmentScratch::new);

    public ElectricalDeviceSystem(
            ElectricalRuntime runtime,
            ResourceType<ChunkStore, ElectricalSystemResource> electricalSystemResourceType,
            ComponentType<ChunkStore, DeviceComponent> deviceComponentType,
            ComponentType<ChunkStore, WireComponent> wireComponentType,
            RuntimeDeviceDefinitions runtimeDefinitions,
            PortModule portModule,
            PortDomain<ElectricalPortConnection> electricalPortDomain
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.electricalSystemResourceType =
                Objects.requireNonNull(electricalSystemResourceType, "electricalSystemResourceType");
        this.deviceComponentType = Objects.requireNonNull(deviceComponentType, "deviceComponentType");
        this.wireComponentType = Objects.requireNonNull(wireComponentType, "wireComponentType");
        this.runtimeDefinitions = Objects.requireNonNull(runtimeDefinitions, "runtimeDefinitions");
        this.portModule = Objects.requireNonNull(portModule, "portModule");
        this.electricalPortDomain = Objects.requireNonNull(electricalPortDomain, "electricalPortDomain");
        this.query = Query.and(deviceComponentType, blockStateInfoComponentType);
    }

    @Override
    public void onEntityAdded(
            @NonNull Ref<ChunkStore> ref,
            @NonNull AddReason reason,
            Store<ChunkStore> store,
            CommandBuffer<ChunkStore> commandBuffer
    ) {
        store.assertThread();

        World world = store.getExternalData().getWorld();
        world.debugAssertInTickingThread();

        DeviceComponent component = commandBuffer.getComponent(ref, deviceComponentType);
        BlockModule.BlockStateInfo blockStateInfo =
                commandBuffer.getComponent(ref, blockStateInfoComponentType);

        if (component == null || blockStateInfo == null) {
            throw new IllegalStateException(
                    "ElectricalDeviceSystem matched an entity without its required components"
            );
        }

        CompiledDeviceConfig compiled = resolveCompiledConfig(blockStateInfo, commandBuffer);
        ElectricalSystem system = store.getResource(electricalSystemResourceType).getOrCreate(runtime, world);
        BindResult result = bindDevice(component, compiled, system, blockStateInfo::markNeedsSaving);

        if (result.created()) {
            try {
                connectNewDevice(component.device(), compiled, blockStateInfo, store, commandBuffer);
            } catch (RuntimeException | Error failure) {
                rollbackCreatedDevice(component, failure);
                throw failure;
            }
        }

        if (result.persistenceChanged()) {
            blockStateInfo.markNeedsSaving(commandBuffer);
        }
    }

    @Override
    public void onEntityRemove(
            @NonNull Ref<ChunkStore> ref,
            @NonNull RemoveReason reason,
            Store<ChunkStore> store,
            CommandBuffer<ChunkStore> commandBuffer
    ) {
        store.assertThread();

        World world = store.getExternalData().getWorld();
        world.debugAssertInTickingThread();

        DeviceComponent component = commandBuffer.getComponent(ref, deviceComponentType);
        if (component == null) {
            throw new IllegalStateException(
                    "ElectricalDeviceSystem matched an entity without a DeviceComponent"
            );
        }

        if (reason == RemoveReason.UNLOAD) {
            unloadDevice(component);
            return;
        }

        CompiledDeviceConfig compiled = component.compiledConfig();
        if (compiled == null && component.getDeviceId() != null) {
            BlockModule.BlockStateInfo blockStateInfo =
                    commandBuffer.getComponent(ref, blockStateInfoComponentType);
            if (blockStateInfo == null) {
                throw new IllegalStateException(
                        "Cannot resolve a persisted electrical device without BlockStateInfo"
                );
            }
            compiled = resolveCompiledConfig(blockStateInfo, commandBuffer);
        }

        ElectricalSystem system = store.getResource(electricalSystemResourceType).getOrCreate(runtime, world);
        destroyDevice(component, compiled, system);
    }

    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }

    private CompiledDeviceConfig resolveCompiledConfig(
            BlockModule.BlockStateInfo blockStateInfo,
            CommandBuffer<ChunkStore> commandBuffer
    ) {
        Ref<ChunkStore> sectionRef = blockStateInfo.getSectionRef();
        if (!sectionRef.isValid()) {
            throw new IllegalStateException("Device block section reference is no longer valid");
        }

        BlockSection blockSection = commandBuffer.getComponent(sectionRef, BlockSection.getComponentType());
        if (blockSection == null) {
            throw new IllegalStateException("Device block section is unavailable");
        }

        int blockIndex = blockStateInfo.getIndex();
        int blockTypeIndex = blockSection.get(
                ChunkUtil.xFromIndex(blockIndex),
                ChunkUtil.yFromIndex(blockIndex),
                ChunkUtil.zFromIndex(blockIndex)
        );

        CompiledDeviceConfig compiled = runtimeDefinitions.get(blockTypeIndex);
        if (compiled == null) {
            throw new IllegalStateException(
                    "Block type " + blockTypeIndex + " has no compiled electrical device definition"
            );
        }
        return compiled;
    }

    private void connectNewDevice(
            @Nullable Device device,
            CompiledDeviceConfig compiled,
            BlockModule.BlockStateInfo blockStateInfo,
            Store<ChunkStore> store,
            CommandBuffer<ChunkStore> commandBuffer
    ) {
        if (device == null) {
            throw new IllegalStateException("New DeviceComponent is not runtime-bound");
        }
        if (compiled.portCount() == 0) {
            return;
        }

        AttachmentScratch scratch = attachmentScratch.get();
        if (!blockStateInfo.fillWorldPos(commandBuffer, scratch.sourcePosition)) {
            return;
        }

        ChunkStore chunkStore = store.getExternalData();
        HytalePortWorldView worldView =
                new HytalePortWorldView(chunkStore.getWorld(), portModule::blockPorts);
        scratch.begin(device, chunkStore, commandBuffer, wireComponentType);

        try {
            for (int index = 0; index < compiled.portCount(); index++) {
                scratch.nativeTerminalId = compiled.nativeTerminalIdAt(index);
                portModule.discovery().discover(
                        worldView,
                        scratch.sourcePosition.x,
                        scratch.sourcePosition.y,
                        scratch.sourcePosition.z,
                        compiled.portIdAt(index),
                        electricalPortDomain,
                        scratch
                );
            }
        } finally {
            scratch.end();
        }
    }

    static BindResult bindDevice(
            DeviceComponent component,
            CompiledDeviceConfig compiled,
            ElectricalSystem system,
            Runnable markNeedsSaving
    ) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(compiled, "compiled");
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(markNeedsSaving, "markNeedsSaving");

        if (component.device() != null || component.compiledConfig() != null) {
            throw new IllegalStateException("DeviceComponent is already runtime-bound");
        }

        DeviceId persistedId = component.getDeviceId();
        boolean created = persistedId == null;
        Device device = created
                ? system.create(compiled.descriptor().type())
                : system.resolveDevice(persistedId, compiled.descriptor().type());

        boolean migrated;
        try {
            migrated = synchronizeParameters(component, compiled, device);
        } catch (RuntimeException | Error failure) {
            if (created) {
                try {
                    device.destroy();
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }

        if (created) {
            component.setDeviceId(device.id());
        }
        component.bindRuntime(device, compiled, markNeedsSaving);
        return new BindResult(created, created || migrated);
    }

    static boolean synchronizeParameters(
            DeviceComponent component,
            CompiledDeviceConfig compiled,
            Device device
    ) {
        ParameterOverrides overrides = component.overrides();

        // Check the complete intent before queuing any writes to a restored device.
        // A later invalid value must not leave earlier values queued for application.
        for (int index = 0; index < compiled.parameterCount(); index++) {
            int stableId = compiled.stableParameterIdAt(index);
            double value = overrides.getOrDefault(stableId, compiled.parameterDefaultAt(index));
            device.validateParameter(compiled.nativeParameterIdAt(index), value);
        }
        for (int index = 0; index < overrides.size(); index++) {
            int stableId = overrides.stableIdAt(index);
            if (compiled.findParameterDefault(stableId) >= 0) {
                continue;
            }
            int nativeId = compiled.nativeParameterId(stableId);
            if (nativeId != MemberMapping.UNMAPPED) {
                device.validateParameter(nativeId, overrides.valueAt(index));
            }
        }

        for (int index = 0; index < compiled.parameterCount(); index++) {
            int stableId = compiled.stableParameterIdAt(index);
            double value = overrides.getOrDefault(stableId, compiled.parameterDefaultAt(index));
            device.setParameter(compiled.nativeParameterIdAt(index), value);
        }

        for (int index = 0; index < overrides.size(); index++) {
            int stableId = overrides.stableIdAt(index);
            if (compiled.findParameterDefault(stableId) >= 0) {
                continue;
            }

            int nativeId = compiled.nativeParameterId(stableId);
            if (nativeId != MemberMapping.UNMAPPED) {
                device.setParameter(nativeId, overrides.valueAt(index));
            }
        }

        boolean migrated = false;
        for (int index = overrides.size() - 1; index >= 0; index--) {
            int stableId = overrides.stableIdAt(index);
            if (compiled.nativeParameterId(stableId) == MemberMapping.UNMAPPED) {
                overrides.remove(stableId);
                migrated = true;
            }
        }
        return migrated;
    }

    static void unloadDevice(DeviceComponent component) {
        Objects.requireNonNull(component, "component").clearRuntime();
    }

    static void destroyDevice(
            DeviceComponent component,
            @Nullable CompiledDeviceConfig compiled,
            ElectricalSystem system
    ) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(system, "system");

        Device device = component.device();
        DeviceId id = component.getDeviceId();

        if (device == null && id != null) {
            if (compiled == null) {
                throw new IllegalStateException(
                        "A compiled device definition is required to resolve a persisted device before removal"
                );
            }
            device = system.resolveDevice(id, compiled.descriptor().type());
        }

        if (device != null) {
            device.destroy();
        }

        component.clearRuntime();
        component.setDeviceId(null);
    }

    private static void rollbackCreatedDevice(DeviceComponent component, Throwable failure) {
        Device device = component.device();
        if (device != null) {
            try {
                device.destroy();
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
        component.clearRuntime();
        component.setDeviceId(null);
    }

    record BindResult(boolean created, boolean persistenceChanged) {
    }

    private static final class AttachmentScratch
            implements PortConnectionConsumer<ElectricalPortConnection> {
        private final Vector3i sourcePosition = new Vector3i();
        private final DeviceWireConnections.AttachmentDedup dedup = new DeviceWireConnections.AttachmentDedup();

        private boolean inUse;
        private @Nullable Device device;
        private @Nullable ChunkStore chunkStore;
        private @Nullable CommandBuffer<ChunkStore> commandBuffer;
        private @Nullable ComponentType<ChunkStore, WireComponent> wireComponentType;
        private int nativeTerminalId;

        void begin(
                Device device,
                ChunkStore chunkStore,
                CommandBuffer<ChunkStore> commandBuffer,
                ComponentType<ChunkStore, WireComponent> wireComponentType
        ) {
            if (inUse) {
                throw new IllegalStateException("Electrical device attachment discovery is not reentrant");
            }
            inUse = true;
            this.device = device;
            this.chunkStore = chunkStore;
            this.commandBuffer = commandBuffer;
            this.wireComponentType = wireComponentType;
            dedup.clear();
        }

        void end() {
            dedup.clear();
            device = null;
            chunkStore = null;
            commandBuffer = null;
            wireComponentType = null;
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

            ChunkStore chunkStore = Objects.requireNonNull(this.chunkStore, "attachment chunkStore");
            CommandBuffer<ChunkStore> commandBuffer =
                    Objects.requireNonNull(this.commandBuffer, "attachment commandBuffer");
            ComponentType<ChunkStore, WireComponent> wireComponentType =
                    Objects.requireNonNull(this.wireComponentType, "attachment wireComponentType");
            Device device = Objects.requireNonNull(this.device, "attachment device");

            Ref<ChunkStore> targetSectionRef =
                    chunkStore.getChunkSectionReferenceAtBlock(targetX, targetY, targetZ);
            if (targetSectionRef == null || !targetSectionRef.isValid()) {
                return;
            }

            Ref<ChunkStore> targetRef = BlockModule.getBlockEntity(
                    commandBuffer,
                    targetSectionRef,
                    targetX,
                    targetY,
                    targetZ
            );
            if (targetRef == null) {
                return;
            }

            WireComponent targetComponent = commandBuffer.getComponent(targetRef, wireComponentType);
            if (targetComponent == null) {
                return;
            }

            DeviceWireConnections.attachIfNew(device, nativeTerminalId, targetComponent.getWire(), dedup);
        }
    }
}
