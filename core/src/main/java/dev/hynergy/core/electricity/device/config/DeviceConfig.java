package dev.hynergy.core.electricity.device.config;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import dev.hynergy.core.electricity.device.DeviceRegistration;
import dev.hynergy.core.electricity.device.DeviceRegistry;
import dev.hynergy.core.electricity.port.ElectricalPortConnection;
import dev.hynergy.core.electricity.port.ElectricalPortProfile;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.core.port.layout.BlockPortDefinition;
import dev.hynergy.core.port.layout.PortDefinition;
import dev.hynergy.core.port.layout.PortOffset;
import dev.hynergy.core.port.layout.PortReach;
import lombok.Getter;
import org.joml.Vector3i;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Configures one generic electrical device block definition.
 *
 * <p>The persistence codec saves only stable IDs.
 * Compilation resolves typed members through the selected {@link DeviceRegistration}.</p>
 */
public final class DeviceConfig implements JsonAssetWithMap<String, DefaultAssetMap<String, DeviceConfig>> {
    public static final String DATA_SET = "DeviceConfigs";

    private static final DeviceParameterConfig[] EMPTY_PARAMETERS = new DeviceParameterConfig[0];
    private static final DevicePortConfig[] EMPTY_PORTS = new DevicePortConfig[0];

    /**
     * Creates the asset codec for the plugin's device registry.
     * Register devices during plugin setup, before Hytale loads assets.
     */
    public static AssetBuilderCodec<String, DeviceConfig> createCodec(DeviceRegistry devices) {
        Objects.requireNonNull(devices, "devices");
        return AssetBuilderCodec.builder(
                                        DeviceConfig.class,
                                        DeviceConfig::new,
                                        Codec.STRING,
                                        (config, id) -> config.id = id,
                                        DeviceConfig::codecId,
                                        (config, data) -> config.extraData = data,
                                        config -> config.extraData
                                )
                                .append(
                                        new KeyedCodec<>("Type", Codec.STRING, true),
                                        (config, type) -> config.type = type,
                                        config -> config.type
                                )
                                .addValidator(Validators.nonEmptyString())
                                .addValidator(new DeviceTypeValidator(devices))
                                .metadata(new UIEditor(new UIEditor.Dropdown(DeviceRegistry.DATA_SET)))
                                .documentation("Stable registered electrical device type ID.")
                                .add()
                                .append(
                                        new KeyedCodec<>(
                                                "Parameters",
                                                new ArrayCodec<>(DeviceParameterConfig.CODEC, DeviceParameterConfig[]::new)
                                        ),
                                        (config, parameters) -> config.parameters = defaultParameters(parameters),
                                        config -> config.parameters
                                )
                                .documentation("Optional configured defaults keyed by stable parameter ID.")
                                .add()
                                .append(
                                        new KeyedCodec<>(
                                                "Ports",
                                                new ArrayCodec<>(DevicePortConfig.CODEC, DevicePortConfig[]::new)
                                        ),
                                        (config, ports) -> config.ports = defaultPorts(ports),
                                        config -> config.ports
                                )
                                .documentation("Physical electrical ports exposed by this device block. May be empty.")
                                .add()
                                .build();
    }

    public static final ValidatorCache<@Nullable String> VALIDATOR_CACHE =
            new ValidatorCache<>(new AssetKeyValidator<>(DeviceConfig::getAssetStore));

    private static @Nullable AssetStore<String, DeviceConfig, DefaultAssetMap<String, DeviceConfig>> ASSET_STORE;

    private AssetExtraInfo.@Nullable Data extraData;
    private @Nullable String id;
    @Getter private @Nullable String type;
    @Getter private DeviceParameterConfig[] parameters = EMPTY_PARAMETERS;
    @Getter private DevicePortConfig[] ports = EMPTY_PORTS;

    private DeviceConfig() {
    }

    @Override
    public String getId() {
        return Objects.requireNonNull(id, "DeviceConfig asset ID is not initialized");
    }

    private static DeviceParameterConfig[] defaultParameters(DeviceParameterConfig @Nullable [] values) {
        return values == null ? EMPTY_PARAMETERS : values;
    }

    private static DevicePortConfig[] defaultPorts(DevicePortConfig @Nullable [] values) {
        return values == null ? EMPTY_PORTS : values;
    }

    @SuppressWarnings("unused")
    DeviceConfig(
            String id,
            String type,
            DeviceParameterConfig @Nullable [] parameters,
            DevicePortConfig @Nullable [] ports
    ) {
        this.id = id;
        this.type = type;
        this.parameters = parameters == null ? EMPTY_PARAMETERS : parameters.clone();
        this.ports = ports == null ? EMPTY_PORTS : ports.clone();
    }

    /**
     * Compiles stable asset member IDs to immutable typed bindings.
     *
     * <p>Asset loading and rebuilds use this method.
     * The electrical engine owns the native constraints for each definition.
     * This method does not duplicate those constraints.</p>
     */
    public CompiledDeviceConfig compile(
            DeviceRegistry devices,
            PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard
    ) {
        Objects.requireNonNull(devices, "devices");
        Objects.requireNonNull(conductorStandard, "conductorStandard");

        String type = this.type;
        if (type == null || type.isBlank()) {
            throw invalid("Type must name a registered device type");
        }

        DeviceRegistration registration;
        try {
            registration = devices.require(type);
        } catch (IllegalArgumentException failure) {
            throw invalid("Unknown device type: " + type, failure);
        }

        DeviceParameterConfig[] parameterConfigs = parameters;
        int[] stableParameterIds = new int[parameterConfigs.length];
        var parameterBindings = new ArrayList<CompiledDeviceConfig.ParameterBinding>(parameterConfigs.length);

        for (int index = 0; index < parameterConfigs.length; index++) {
            DeviceParameterConfig parameter = requireParameter(parameterConfigs[index]);

            int stableId = parameter.getId();
            if (stableId < 0) {
                throw invalid("Parameter stable ID must be non-negative: " + stableId);
            }
            requireUniqueParameter(stableParameterIds, index, stableId);

            dev.hynergy.electrical.DeviceParameter member;
            try {
                member = registration.type().parameter(stableId);
            } catch (IllegalArgumentException failure) {
                throw invalid("Unknown parameter stable ID: " + stableId, failure);
            }

            double defaultValue = parameter.getDefaultValue();
            if (!Double.isFinite(defaultValue)) {
                throw invalid("Parameter Default must be finite for stable ID " + stableId);
            }

            stableParameterIds[index] = stableId;
            try {
                registration.binding().validateParameter(member, defaultValue);
            } catch (IllegalArgumentException failure) {
                throw invalid("Invalid parameter Default for stable ID " + stableId, failure);
            }
            parameterBindings.add(new CompiledDeviceConfig.ParameterBinding(member, defaultValue));
        }

        DevicePortConfig[] portConfigs = ports;
        int[] portIds = new int[portConfigs.length];
        var portBindings = new ArrayList<CompiledDeviceConfig.PortBinding>(portConfigs.length);
        PortDefinition<?, ?>[] portDefinitions = new PortDefinition[portConfigs.length];

        for (int index = 0; index < portConfigs.length; index++) {
            DevicePortConfig port = requirePort(portConfigs[index]);

            int portId = port.getId();
            if (portId < 0) {
                throw invalid("Port Id must be non-negative: " + portId);
            }
            requireUniquePort(portIds, index, portId);

            int stableTerminalId = port.getTerminal();
            if (stableTerminalId < 0) {
                throw invalid("Terminal stable ID must be non-negative: " + stableTerminalId);
            }

            dev.hynergy.electrical.DeviceTerminal member;
            try {
                member = registration.type().terminal(stableTerminalId);
            } catch (IllegalArgumentException failure) {
                throw invalid("Unknown terminal stable ID: " + stableTerminalId, failure);
            }

            Vector3i normal = port.getNormal();
            if (normal == null || DevicePortConfig.isNotCardinal(normal)) {
                throw invalid("Port " + portId + " Normal must be one of the six cardinal unit vectors");
            }

            Vector3i anchor = port.getAnchor();
            PortOffset offset = anchor == null
                    ? PortOffset.ZERO
                    : new PortOffset(anchor.x, anchor.y, anchor.z);

            portIds[index] = portId;
            portBindings.add(new CompiledDeviceConfig.PortBinding(portId, member));
            portDefinitions[index] = new PortDefinition<>(
                    portId,
                    offset,
                    PortReach.single(normal.x, normal.y, normal.z),
                    conductorStandard,
                    new ElectricalPortProfile(normal.x, normal.y, normal.z)
            );
        }

        return new CompiledDeviceConfig(
                registration,
                parameterBindings,
                portBindings,
                BlockPortDefinition.of(portDefinitions)
        );
    }

    private DeviceParameterConfig requireParameter(@Nullable DeviceParameterConfig parameter) {
        if (parameter == null) {
            throw invalid("Parameters must not contain null entries");
        }
        return parameter;
    }

    private DevicePortConfig requirePort(@Nullable DevicePortConfig port) {
        if (port == null) {
            throw invalid("Ports must not contain null entries");
        }
        return port;
    }

    private static void requireUniqueParameter(int[] stableIds, int length, int stableId) {
        for (int previous = 0; previous < length; previous++) {
            if (stableIds[previous] == stableId) {
                throw new IllegalArgumentException("Duplicate device parameter Id: " + stableId);
            }
        }
    }

    private static void requireUniquePort(int[] portIds, int length, int portId) {
        for (int previous = 0; previous < length; previous++) {
            if (portIds[previous] == portId) {
                throw new IllegalArgumentException("Duplicate device port Id: " + portId);
            }
        }
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid device config " + displayId() + ": " + message);
    }

    private IllegalArgumentException invalid(String message, Throwable cause) {
        return new IllegalArgumentException("Invalid device config " + displayId() + ": " + message, cause);
    }

    @NullUnmarked
    private static String codecId(DeviceConfig config) {
        return config.id;
    }

    private String displayId() {
        return id == null ? "<unknown>" : id;
    }

    public static AssetStore<String, DeviceConfig, DefaultAssetMap<String, DeviceConfig>> getAssetStore() {
        var store = ASSET_STORE;
        if (store == null) {
            store = Objects.requireNonNull(
                    AssetRegistry.getAssetStore(DeviceConfig.class),
                    "DeviceConfig asset store is not registered"
            );
            ASSET_STORE = store;
        }
        return store;
    }

    public static DefaultAssetMap<String, DeviceConfig> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public static void populateDataSet(AssetEditorRequestDataSetEvent event) {
        String[] ids = getAssetMap()
                .getAssetMap()
                .keySet()
                .stream()
                .sorted()
                .toArray(String[]::new);
        event.setResults(ids);
    }
}
