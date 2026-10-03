package dev.hynergy.core.electricity;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.plugin.registry.AssetRegistry;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.HynergyModule;
import dev.hynergy.core.electricity.device.*;
import dev.hynergy.core.electricity.wire.WireBlockPortDefinitions;
import dev.hynergy.core.electricity.wire.WireComponent;
import dev.hynergy.core.electricity.wire.WireConfig;
import dev.hynergy.core.electricity.wire.WireSystem;
import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortGeometry;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.electrical.Device;
import dev.hynergy.electrical.DeviceDefinition;
import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.joml.Vector3i;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ElectricityModule extends HynergyModule {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    static final DeviceDescriptor<Resistance> RESISTANCE_DESCRIPTOR = new DeviceDescriptor<>(
            "hynergy:resistance",
            Resistance.TYPE,
            new MemberMapping(0),
            new MemberMapping(0, 1),
            new MemberMapping(0, 1)
    );

    private final PortModule portModule;
    private final ComponentRegistryProxy<ChunkStore> chunkStoreRegistry;
    private final AssetRegistry assetRegistry;
    private final EventRegistry eventRegistry;
    private final DeviceDescriptorRegistry deviceDescriptors = new DeviceDescriptorRegistry();
    private final RuntimeDeviceDefinitions runtimeDeviceDefinitions = new RuntimeDeviceDefinitions();

    private @Nullable ElectricalRuntime runtime;
    private @Nullable PortDomain<ElectricalPortConnection> electricalPortDomain;
    private @Nullable PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorPortStandard;

    private @Nullable WireBlockPortDefinitions wireBlockPortDefinitions;
    private @Nullable DeviceBlockDefinitions deviceBlockDefinitions;

    private boolean started;

    public ElectricityModule(
            PortModule portModule,
            ComponentRegistryProxy<ChunkStore> chunkStoreRegistry,
            AssetRegistry assetRegistry,
            EventRegistry eventRegistry
    ) {
        this.portModule = Objects.requireNonNull(portModule, "portModule");
        this.chunkStoreRegistry = Objects.requireNonNull(chunkStoreRegistry, "chunkStoreRegistry");
        this.assetRegistry = Objects.requireNonNull(assetRegistry, "assetRegistry");
        this.eventRegistry = Objects.requireNonNull(eventRegistry, "eventRegistry");
    }

    @Override
    public void setup() {
        LOGGER.at(Level.INFO).log("Setting up electricity module");

        ElectricalRuntime runtime = ElectricalRuntime.create();
        this.runtime = runtime;

        registerPortProtocols();
        registerDevice(
                RESISTANCE_DESCRIPTOR.id(),
                RESISTANCE_DESCRIPTOR.type(),
                RESISTANCE_DESCRIPTOR.parameters(),
                RESISTANCE_DESCRIPTOR.terminals(),
                RESISTANCE_DESCRIPTOR.observers()
        );

        ComponentType<ChunkStore, WireComponent> wireComponentType =
                chunkStoreRegistry.registerComponent(
                        WireComponent.class,
                        "HynergyWire",
                        WireComponent.CODEC
                );

        ComponentType<ChunkStore, DeviceComponent> deviceComponentType =
                chunkStoreRegistry.registerComponent(
                        DeviceComponent.class,
                        "HynergyDevice",
                        DeviceComponent.CODEC
                );

        wireBlockPortDefinitions =
                new WireBlockPortDefinitions(
                        portModule,
                        conductorPortStandard(),
                        wireComponentType
                );

        deviceBlockDefinitions = new DeviceBlockDefinitions(
                deviceDescriptors,
                runtimeDeviceDefinitions,
                portModule,
                conductorPortStandard(),
                deviceComponentType
        );

        assetRegistry.register(
                HytaleAssetStore.builder(
                                        WireConfig.class,
                                        new DefaultAssetMap<>()
                                )
                                .setPath("Hynergy/Electricity/Wires")
                                .setCodec(WireConfig.CODEC)
                                .setKeyFunction(WireConfig::getId)
                                .build()
        );

        assetRegistry.register(
                HytaleAssetStore.builder(
                                        DeviceConfig.class,
                                        new DefaultAssetMap<>()
                                )
                                .setPath("Hynergy/Electricity/Devices")
                                .setCodec(DeviceConfig.CODEC)
                                .setKeyFunction(DeviceConfig::getId)
                                .build()
        );

        eventRegistry.register(
                AssetEditorRequestDataSetEvent.class,
                WireConfig.DATA_SET,
                WireConfig::populateDataSet
        );

        eventRegistry.register(
                AssetEditorRequestDataSetEvent.class,
                DeviceConfig.DATA_SET,
                DeviceConfig::populateDataSet
        );

        eventRegistry.register(
                LoadedAssetsEvent.class,
                WireConfig.class,
                (Consumer<LoadedAssetsEvent<
                        String,
                        WireConfig,
                        DefaultAssetMap<String, WireConfig>
                        >>) event -> rebuildWireBlockPorts()
        );

        eventRegistry.register(
                RemovedAssetsEvent.class,
                WireConfig.class,
                (Consumer<RemovedAssetsEvent<
                        String,
                        WireConfig,
                        DefaultAssetMap<String, WireConfig>
                        >>) event -> rebuildWireBlockPorts()
        );

        eventRegistry.register(
                LoadedAssetsEvent.class,
                DeviceConfig.class,
                (Consumer<LoadedAssetsEvent<
                        String,
                        DeviceConfig,
                        DefaultAssetMap<String, DeviceConfig>
                        >>) event -> rebuildDeviceBlockDefinitions()
        );

        eventRegistry.register(
                RemovedAssetsEvent.class,
                DeviceConfig.class,
                (Consumer<RemovedAssetsEvent<
                        String,
                        DeviceConfig,
                        DefaultAssetMap<String, DeviceConfig>
                        >>) event -> rebuildDeviceBlockDefinitions()
        );

        eventRegistry.register(
                LoadedAssetsEvent.class,
                BlockType.class,
                (Consumer<LoadedAssetsEvent<
                        String,
                        BlockType,
                        BlockTypeAssetMap<String, BlockType>
                        >>) event -> rebuildBlockDefinitions()
        );

        eventRegistry.register(
                RemovedAssetsEvent.class,
                BlockType.class,
                (Consumer<RemovedAssetsEvent<
                        String,
                        BlockType,
                        BlockTypeAssetMap<String, BlockType>
                        >>) event -> rebuildBlockDefinitions()
        );

        registerSystems(runtime, wireComponentType, deviceComponentType);
    }

    private void registerPortProtocols() {
        electricalPortDomain = portModule.registerDomain("hynergy:electrical");
        conductorPortStandard = portModule.registerStandard(
                "hynergy:electrical/conductor",
                electricalPortDomain,
                ElectricalPortProfile.class,
                ElectricityModule::resolveConductorConnection
        );
    }

    private void registerSystems(
            ElectricalRuntime runtime,
            ComponentType<ChunkStore, WireComponent> wireComponentType,
            ComponentType<ChunkStore, DeviceComponent> deviceComponentType
    ) {
        ResourceType<ChunkStore, ElectricalSystemResource> resourceType =
                chunkStoreRegistry.registerResource(
                        ElectricalSystemResource.class,
                        ElectricalSystemResource::new
                );

        chunkStoreRegistry.registerSystem(
                new ElectricalSystemLifecycleSystem(resourceType)
        );

        chunkStoreRegistry.registerSystem(
                new WireSystem(
                        runtime,
                        resourceType,
                        wireComponentType,
                        portModule,
                        electricalPortDomain()
                )
        );

        chunkStoreRegistry.registerSystem(
                new ElectricalDeviceSystem(
                        runtime,
                        resourceType,
                        deviceComponentType,
                        wireComponentType,
                        runtimeDeviceDefinitions,
                        portModule,
                        electricalPortDomain()
                )
        );

        chunkStoreRegistry.registerSystem(
                new ElectricalTickSystem(runtime, resourceType)
        );
    }

    private void rebuildWireBlockPorts() {
        WireBlockPortDefinitions definitions = wireBlockPortDefinitions;
        if (definitions == null) {
            throw new IllegalStateException(
                    "Wire block port definitions are unavailable before module setup"
            );
        }
        definitions.rebuild();
    }

    private void rebuildDeviceBlockDefinitions() {
        if (!deviceDescriptors.isFrozen()) {
            return;
        }

        DeviceBlockDefinitions definitions = deviceBlockDefinitions;
        if (definitions == null) {
            throw new IllegalStateException(
                    "Device block definitions are unavailable before module setup"
            );
        }
        definitions.rebuild();
    }

    private void rebuildBlockDefinitions() {
        rebuildWireBlockPorts();
        rebuildDeviceBlockDefinitions();
    }

    @Override
    public void start() {
        deviceDescriptors.freeze();
        rebuildBlockDefinitions();
        started = true;
    }

    @Override
    public void shutdown() {
        if (runtime == null) {
            throw new IllegalStateException("Electrical runtime must be initialized before shutting down.");
        }

        runtime.requestClose();
    }

    public PortDomain<ElectricalPortConnection> electricalPortDomain() {
        PortDomain<ElectricalPortConnection> domain = electricalPortDomain;
        if (domain == null) {
            throw new IllegalStateException("Electrical port domain is unavailable before module setup");
        }
        return domain;
    }

    public PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorPortStandard() {
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> standard = conductorPortStandard;
        if (standard == null) {
            throw new IllegalStateException("Electrical conductor port standard is unavailable before module setup");
        }
        return standard;
    }

    static @Nullable ElectricalPortConnection resolveConductorConnection(
            ElectricalPortProfile first,
            ElectricalPortProfile second,
            PortGeometry geometry
    ) {
        Vector3i firstNormal = new Vector3i(first.normalX(), first.normalY(), first.normalZ());
        geometry.firstRotation().applyRotationTo(firstNormal);

        if (geometry.ownerDx() != firstNormal.x()
                || geometry.ownerDy() != firstNormal.y()
                || geometry.ownerDz() != firstNormal.z()
                || geometry.anchorDx() != firstNormal.x()
                || geometry.anchorDy() != firstNormal.y()
                || geometry.anchorDz() != firstNormal.z()) {
            return null;
        }

        Vector3i secondNormal = new Vector3i(second.normalX(), second.normalY(), second.normalZ());
        geometry.secondRotation().applyRotationTo(secondNormal);

        if (secondNormal.x() != -firstNormal.x()
                || secondNormal.y() != -firstNormal.y()
                || secondNormal.z() != -firstNormal.z()) {
            return null;
        }

        return ElectricalPortConnection.DIRECT;
    }

    public <T extends Device> DeviceDescriptor<T> registerDevice(
            String id,
            DeviceType<T> type,
            MemberMapping parameters,
            MemberMapping terminals,
            MemberMapping observers
    ) {
        requireRegistrationOpen();

        ElectricalRuntime runtime = this.runtime;
        if (runtime == null) {
            throw new IllegalStateException(
                    "Electrical runtime must be initialized before registering a device"
            );
        }

        DeviceDescriptor<T> descriptor = deviceDescriptors.register(
                id,
                type,
                parameters,
                terminals,
                observers
        );
        runtime.register(type);
        return descriptor;
    }

    public <T extends Device> DeviceDefinition register(DeviceType<T> type) {
        requireRegistrationOpen();

        ElectricalRuntime runtime = this.runtime;
        if (runtime == null) {
            throw new IllegalStateException(
                    "Electrical runtime must be initialized before registering a device"
            );
        }
        return runtime.register(type);
    }

    private void requireRegistrationOpen() {
        if (started || deviceDescriptors.isFrozen()) {
            throw new IllegalStateException("Electrical device types must be registered during setup");
        }
    }
}
