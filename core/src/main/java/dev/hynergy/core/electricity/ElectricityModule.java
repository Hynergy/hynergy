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
import dev.hynergy.electrical.ElectricalRuntime;
import dev.hynergy.electrical.PrimitiveDeviceTypes;
import dev.hynergy.electrical.composite.GroundedLogicGates;
import dev.hynergy.electrical.composite.GroundedSwitchedLogicGates;
import dev.hynergy.electrical.composite.ResistiveLoad;
import dev.hynergy.electrical.composite.VoltageSupply;
import org.joml.Vector3i;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ElectricityModule extends HynergyModule {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    static final String RESISTANCE_ID = "hynergy:resistance";

    private final PortModule portModule;
    private final ComponentRegistryProxy<ChunkStore> chunkStoreRegistry;
    private final AssetRegistry assetRegistry;
    private final EventRegistry eventRegistry;
    private @Nullable DeviceRegistry deviceRegistry;

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
        this.deviceRegistry = new DeviceRegistry(runtime);

        registerPortProtocols();
        registerBuiltinDevices(devices());

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

        ComponentType<ChunkStore, LightbulbComponent> lightbulbComponentType =
                chunkStoreRegistry.registerComponent(
                        LightbulbComponent.class,
                        "HynergyLightbulb",
                        LightbulbComponent.CODEC
                );

        wireBlockPortDefinitions =
                new WireBlockPortDefinitions(
                        portModule,
                        conductorPortStandard(),
                        wireComponentType
                );

        deviceBlockDefinitions = new DeviceBlockDefinitions(
                devices(),
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
                                .setCodec(DeviceConfig.createCodec(devices()))
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
                AssetEditorRequestDataSetEvent.class,
                DeviceRegistry.DATA_SET,
                devices()::populateDataSet
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

        registerSystems(runtime, wireComponentType, deviceComponentType, lightbulbComponentType);
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
            ComponentType<ChunkStore, DeviceComponent> deviceComponentType,
            ComponentType<ChunkStore, LightbulbComponent> lightbulbComponentType
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
                        deviceComponentType,
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
                        Objects.requireNonNull(deviceBlockDefinitions, "deviceBlockDefinitions"),
                        portModule,
                        electricalPortDomain()
                )
        );

        chunkStoreRegistry.registerSystem(
                new LightbulbSystem(deviceComponentType, lightbulbComponentType)
        );
        chunkStoreRegistry.registerSystem(
                new ElectricalTickSystem(runtime, resourceType)
        );
        chunkStoreRegistry.registerSystem(
                new LightbulbSystem.Visuals(deviceComponentType, lightbulbComponentType)
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
        if (!devices().isFrozen()) {
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
        devices().freeze();
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

    static void registerBuiltinDevices(DeviceRegistry devices) {
        devices.register(RESISTANCE_ID, PrimitiveDeviceTypes.RESISTANCE);
        devices.register("hynergy:conductance", PrimitiveDeviceTypes.CONDUCTANCE);
        devices.register("hynergy:voltage_source", PrimitiveDeviceTypes.VOLTAGE_SOURCE);
        devices.register("hynergy:current_source", PrimitiveDeviceTypes.CURRENT_SOURCE);
        devices.register("hynergy:voltage_controlled_current_source", PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_CURRENT_SOURCE);
        devices.register("hynergy:voltage_controlled_voltage_source", PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_VOLTAGE_SOURCE);
        devices.register("hynergy:capacitor", PrimitiveDeviceTypes.CAPACITOR);
        devices.register("hynergy:inductor", PrimitiveDeviceTypes.INDUCTOR);
        devices.register("hynergy:voltage_controlled_switch", PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_SWITCH);
        devices.register("hynergy:voltage_controlled_conductance", PrimitiveDeviceTypes.VOLTAGE_CONTROLLED_CONDUCTANCE);
        devices.register("hynergy:tick_delay", PrimitiveDeviceTypes.TICK_DELAY);
        devices.register("hynergy:diode", PrimitiveDeviceTypes.DIODE);
        devices.register("hynergy:not", PrimitiveDeviceTypes.NOT);
        devices.register("hynergy:and", PrimitiveDeviceTypes.AND);
        devices.register("hynergy:nand", PrimitiveDeviceTypes.NAND);
        devices.register("hynergy:or", PrimitiveDeviceTypes.OR);
        devices.register("hynergy:nor", PrimitiveDeviceTypes.NOR);
        devices.register("hynergy:schmitt_buffer", PrimitiveDeviceTypes.SCHMITT_BUFFER);

        devices.register("hynergy:voltage_supply", VoltageSupply.TYPE);
        devices.register("hynergy:resistive_load", ResistiveLoad.RESISTIVE_LOAD);
        devices.register("hynergy:grounded_and", GroundedLogicGates.GROUNDED_AND);
        devices.register("hynergy:grounded_or", GroundedLogicGates.GROUNDED_OR);
        devices.register("hynergy:grounded_nand", GroundedLogicGates.GROUNDED_NAND);
        devices.register("hynergy:grounded_nor", GroundedLogicGates.GROUNDED_NOR);
        devices.register("hynergy:grounded_not", GroundedLogicGates.GROUNDED_NOT);
        devices.register("hynergy:grounded_switched_not", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOT);
        devices.register("hynergy:grounded_switched_and", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_AND);
        devices.register("hynergy:grounded_switched_nand", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NAND);
        devices.register("hynergy:grounded_switched_or", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_OR);
        devices.register("hynergy:grounded_switched_nor", GroundedSwitchedLogicGates.GROUNDED_SWITCHED_NOR);
    }

    /**
     * Returns the asset registry for plugin setup.
     */
    public DeviceRegistry devices() {
        if (deviceRegistry == null)
            throw new IllegalStateException("Electrical runtime must be initialized before registering a device");
        return deviceRegistry;
    }
}
