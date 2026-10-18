package dev.hynergy.core.electricity;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import dev.hynergy.core.electricity.cable.CableBlockPortDefinitions;
import dev.hynergy.core.electricity.cable.CableComponent;
import dev.hynergy.core.electricity.cable.CableConfig;
import dev.hynergy.core.electricity.cable.CableSystem;
import dev.hynergy.core.electricity.device.DeviceRegistry;
import dev.hynergy.core.electricity.device.config.DeviceBlockDefinitions;
import dev.hynergy.core.electricity.device.config.DeviceConfig;
import dev.hynergy.core.electricity.device.runtime.DeviceComponent;
import dev.hynergy.core.electricity.device.runtime.DeviceSystem;
import dev.hynergy.core.electricity.port.ElectricalPortConnection;
import dev.hynergy.core.electricity.port.ElectricalPortProfile;
import dev.hynergy.core.electricity.system.ElectricalSystemLifecycleSystem;
import dev.hynergy.core.electricity.system.ElectricalSystemResource;
import dev.hynergy.core.electricity.system.ElectricalTickSystem;

import dev.hynergy.core.port.PortDomain;
import dev.hynergy.core.port.PortPlugin;
import dev.hynergy.core.port.PortService;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.core.port.connection.PortGeometry;

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

public final class ElectricityPlugin extends JavaPlugin {

    static final String RESISTANCE_ID = "hynergy:resistance";

    private @Nullable ElectricalRuntime runtime;
    private @Nullable DeviceRegistry deviceRegistry;

    private @Nullable PortDomain<ElectricalPortConnection> electricalPortDomain;

    private @Nullable PortStandard<
            ElectricalPortProfile,
            ElectricalPortConnection
            > conductorPortStandard;

    private @Nullable CableBlockPortDefinitions cableBlockPortDefinitions;
    private @Nullable DeviceBlockDefinitions deviceBlockDefinitions;

    public ElectricityPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("Setting up electricity");

        PortPlugin portPlugin = Objects.requireNonNull(
                PluginManager.get().getPlugin(PortPlugin.class),
                "Ports plugin is unavailable"
        );

        PortService ports = portPlugin.ports();

        ElectricalRuntime runtime = ElectricalRuntime.create();
        this.runtime = runtime;

        try {
            DeviceRegistry devices = new DeviceRegistry(runtime);
            this.deviceRegistry = devices;

            registerPortProtocols(ports);
            registerBuiltinDevices(devices);

            var registry = getChunkStoreRegistry();

            ComponentType<ChunkStore, CableComponent> cableComponentType =
                    registry.registerComponent(
                            CableComponent.class,
                            "HynergyWire",
                            CableComponent.CODEC
                    );

            ComponentType<ChunkStore, DeviceComponent> deviceComponentType =
                    registry.registerComponent(
                            DeviceComponent.class,
                            "HynergyDevice",
                            DeviceComponent.CODEC
                    );

            cableBlockPortDefinitions = new CableBlockPortDefinitions(
                    ports,
                    conductorPortStandard(),
                    cableComponentType
            );

            DeviceBlockDefinitions definitions = new DeviceBlockDefinitions(
                    devices,
                    ports,
                    conductorPortStandard(),
                    deviceComponentType
            );

            deviceBlockDefinitions = definitions;

            registerAssets(devices);
            registerEvents(devices);

            registerSystems(
                    runtime,
                    ports,
                    definitions,
                    cableComponentType,
                    deviceComponentType
            );
        } catch (RuntimeException | Error failure) {
            try {
                runtime.requestClose();
            } catch (RuntimeException | Error closeFailure) {
                failure.addSuppressed(closeFailure);
            }

            throw failure;
        }
    }

    @Override
    protected void start() {
        devices().freeze();
        rebuildBlockDefinitions();

        getLogger().at(Level.INFO).log("Electricity started");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("Shutting down electricity");

        ElectricalRuntime runtime = this.runtime;

        if (runtime != null) {
            runtime.requestClose();
        }
    }

    private void registerPortProtocols(PortService ports) {
        PortDomain<ElectricalPortConnection> domain =
                ports.registerDomain("hynergy:electrical");

        PortStandard<ElectricalPortProfile, ElectricalPortConnection> standard =
                ports.registerStandard(
                        "hynergy:electrical/conductor",
                        domain,
                        ElectricalPortProfile.class,
                        ElectricityPlugin::resolveConductorConnection
                );

        electricalPortDomain = domain;
        conductorPortStandard = standard;
    }

    private void registerAssets(DeviceRegistry devices) {
        var registry = getAssetRegistry();

        registry.register(
                HytaleAssetStore.builder(
                                        CableConfig.class,
                                        new DefaultAssetMap<>()
                                )
                                .setPath("Hynergy/Electricity/Wires")
                                .setCodec(CableConfig.CODEC)
                                .setKeyFunction(CableConfig::getId)
                                .build()
        );

        registry.register(
                HytaleAssetStore.builder(
                                        DeviceConfig.class,
                                        new DefaultAssetMap<>()
                                )
                                .setPath("Hynergy/Electricity/Devices")
                                .setCodec(DeviceConfig.createCodec(devices))
                                .setKeyFunction(DeviceConfig::getId)
                                .build()
        );
    }

    private void registerEvents(DeviceRegistry devices) {
        var registry = getEventRegistry();

        registry.register(
                AssetEditorRequestDataSetEvent.class,
                CableConfig.DATA_SET,
                CableConfig::populateDataSet
        );

        registry.register(
                AssetEditorRequestDataSetEvent.class,
                DeviceConfig.DATA_SET,
                DeviceConfig::populateDataSet
        );

        registry.register(
                AssetEditorRequestDataSetEvent.class,
                DeviceRegistry.DATA_SET,
                devices::populateDataSet
        );

        registry.register(
                LoadedAssetsEvent.class,
                CableConfig.class,
                (Consumer<LoadedAssetsEvent<
                        String,
                        CableConfig,
                        DefaultAssetMap<String, CableConfig>
                        >>) event -> rebuildCableBlockPorts()
        );

        registry.register(
                RemovedAssetsEvent.class,
                CableConfig.class,
                (Consumer<RemovedAssetsEvent<
                        String,
                        CableConfig,
                        DefaultAssetMap<String, CableConfig>
                        >>) event -> rebuildCableBlockPorts()
        );

        registry.register(
                LoadedAssetsEvent.class,
                DeviceConfig.class,
                (Consumer<LoadedAssetsEvent<
                        String,
                        DeviceConfig,
                        DefaultAssetMap<String, DeviceConfig>
                        >>) event -> rebuildDeviceBlockDefinitions()
        );

        registry.register(
                RemovedAssetsEvent.class,
                DeviceConfig.class,
                (Consumer<RemovedAssetsEvent<
                        String,
                        DeviceConfig,
                        DefaultAssetMap<String, DeviceConfig>
                        >>) event -> rebuildDeviceBlockDefinitions()
        );

        registry.register(
                LoadedAssetsEvent.class,
                BlockType.class,
                (Consumer<LoadedAssetsEvent<
                        String,
                        BlockType,
                        BlockTypeAssetMap<String, BlockType>
                        >>) event -> rebuildBlockDefinitions()
        );

        registry.register(
                RemovedAssetsEvent.class,
                BlockType.class,
                (Consumer<RemovedAssetsEvent<
                        String,
                        BlockType,
                        BlockTypeAssetMap<String, BlockType>
                        >>) event -> rebuildBlockDefinitions()
        );
    }

    private void registerSystems(
            ElectricalRuntime runtime,
            PortService ports,
            DeviceBlockDefinitions definitions,
            ComponentType<ChunkStore, CableComponent> cableComponentType,
            ComponentType<ChunkStore, DeviceComponent> deviceComponentType
    ) {
        var registry = getChunkStoreRegistry();

        ResourceType<ChunkStore, ElectricalSystemResource> resourceType =
                registry.registerResource(
                        ElectricalSystemResource.class,
                        ElectricalSystemResource::new
                );

        PortDomain<ElectricalPortConnection> domain =
                electricalPortDomain();

        registry.registerSystem(
                new ElectricalSystemLifecycleSystem(resourceType)
        );

        registry.registerSystem(
                new CableSystem(
                        runtime,
                        resourceType,
                        cableComponentType,
                        deviceComponentType,
                        ports,
                        domain
                )
        );

        registry.registerSystem(
                new DeviceSystem(
                        runtime,
                        resourceType,
                        deviceComponentType,
                        cableComponentType,
                        definitions,
                        ports,
                        domain
                )
        );

        registry.registerSystem(
                new ElectricalTickSystem(runtime, resourceType)
        );
    }

    private void rebuildCableBlockPorts() {
        CableBlockPortDefinitions definitions =
                Objects.requireNonNull(
                        cableBlockPortDefinitions,
                        "Cable block definitions are not initialized"
                );

        definitions.rebuild();
    }

    private void rebuildDeviceBlockDefinitions() {
        DeviceRegistry devices = devices();

        if (!devices.isFrozen()) {
            return;
        }

        DeviceBlockDefinitions definitions =
                Objects.requireNonNull(
                        deviceBlockDefinitions,
                        "Device block definitions are not initialized"
                );

        definitions.rebuild();
    }

    private void rebuildBlockDefinitions() {
        rebuildCableBlockPorts();
        rebuildDeviceBlockDefinitions();
    }

    public DeviceRegistry devices() {
        DeviceRegistry registry = deviceRegistry;

        if (registry == null) {
            throw new IllegalStateException(
                    "Device registry is unavailable before electricity setup"
            );
        }

        return registry;
    }

    public PortDomain<ElectricalPortConnection> electricalPortDomain() {
        PortDomain<ElectricalPortConnection> domain = electricalPortDomain;

        if (domain == null) {
            throw new IllegalStateException(
                    "Electrical port domain is unavailable before setup"
            );
        }

        return domain;
    }

    public PortStandard<ElectricalPortProfile, ElectricalPortConnection>
    conductorPortStandard() {
        PortStandard<ElectricalPortProfile, ElectricalPortConnection> standard =
                conductorPortStandard;

        if (standard == null) {
            throw new IllegalStateException(
                    "Conductor port standard is unavailable before setup"
            );
        }

        return standard;
    }

    static @Nullable ElectricalPortConnection resolveConductorConnection(
            ElectricalPortProfile first,
            ElectricalPortProfile second,
            PortGeometry geometry
    ) {
        Vector3i firstNormal = new Vector3i(
                first.normalX(),
                first.normalY(),
                first.normalZ()
        );

        geometry.firstRotation().applyRotationTo(firstNormal);

        if (geometry.ownerDx() != firstNormal.x()
                || geometry.ownerDy() != firstNormal.y()
                || geometry.ownerDz() != firstNormal.z()
                || geometry.anchorDx() != firstNormal.x()
                || geometry.anchorDy() != firstNormal.y()
                || geometry.anchorDz() != firstNormal.z()) {
            return null;
        }

        Vector3i secondNormal = new Vector3i(
                second.normalX(),
                second.normalY(),
                second.normalZ()
        );

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
}