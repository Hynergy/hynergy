package dev.hynergy.core.electricity.device;

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
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.*;
import lombok.Getter;
import org.joml.Vector3i;

import java.util.Objects;

/**
 * Asset configuration for one generic electrical device block definition.
 *
 * <p>Only stable IDs are serialized. Native parameter and terminal indexes are
 * resolved through the selected {@link DeviceDescriptor} during compilation.</p>
 */
public final class DeviceConfig implements JsonAssetWithMap<String, DefaultAssetMap<String, DeviceConfig>> {
    public static final String DATA_SET = "DeviceConfigs";

    private static final DeviceParameterConfig[] EMPTY_PARAMETERS = new DeviceParameterConfig[0];
    private static final DevicePortConfig[] EMPTY_PORTS = new DevicePortConfig[0];

    public static final AssetBuilderCodec<String, DeviceConfig> CODEC =
            AssetBuilderCodec.builder(
                                     DeviceConfig.class,
                                     DeviceConfig::new,
                                     Codec.STRING,
                                     (config, id) -> config.id = id,
                                     config -> config.id,
                                     (config, data) -> config.extraData = data,
                                     config -> config.extraData
                             )
                             .append(
                                     new KeyedCodec<>("Type", Codec.STRING, true),
                                     (config, type) -> config.type = type,
                                     config -> config.type
                             )
                             .addValidator(Validators.nonEmptyString())
                             .documentation("Stable registered electrical device descriptor ID.")
                             .add()
                             .append(
                                     new KeyedCodec<>(
                                             "Parameters",
                                             new ArrayCodec<>(DeviceParameterConfig.CODEC, DeviceParameterConfig[]::new)
                                     ),
                                     (config, parameters) -> config.parameters = parameters == null ? EMPTY_PARAMETERS : parameters,
                                     config -> config.parameters
                             )
                             .documentation("Optional configured defaults keyed by stable parameter ID.")
                             .add()
                             .append(
                                     new KeyedCodec<>(
                                             "Ports",
                                             new ArrayCodec<>(DevicePortConfig.CODEC, DevicePortConfig[]::new)
                                     ),
                                     (config, ports) -> config.ports = ports == null ? EMPTY_PORTS : ports,
                                     config -> config.ports
                             )
                             .documentation("Physical electrical ports exposed by this device block. May be empty.")
                             .add()
                             .build();

    public static final ValidatorCache<String> VALIDATOR_CACHE =
            new ValidatorCache<>(new AssetKeyValidator<>(DeviceConfig::getAssetStore));

    private static AssetStore<String, DeviceConfig, DefaultAssetMap<String, DeviceConfig>> ASSET_STORE;

    private AssetExtraInfo.Data extraData;
    @Getter private String id;
    @Getter private String type;
    @Getter private DeviceParameterConfig[] parameters = EMPTY_PARAMETERS;
    @Getter private DevicePortConfig[] ports = EMPTY_PORTS;

    private DeviceConfig() {
    }

    DeviceConfig(
            String id,
            String type,
            DeviceParameterConfig[] parameters,
            DevicePortConfig[] ports
    ) {
        this.id = id;
        this.type = type;
        this.parameters = parameters == null ? EMPTY_PARAMETERS : parameters.clone();
        this.ports = ports == null ? EMPTY_PORTS : ports.clone();
    }

    /**
     * Compiles stable asset member IDs to immutable runtime-native indexes.
     *
     * <p>This is a cold asset/rebuild path. Definition-specific native parameter
     * constraints remain owned by the electrical engine and are not duplicated
     * here.</p>
     */
    public CompiledDeviceConfig compile(
            DeviceDescriptorRegistry descriptors,
            PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard
    ) {
        Objects.requireNonNull(descriptors, "descriptors");
        Objects.requireNonNull(conductorStandard, "conductorStandard");

        String type = this.type;
        if (type == null || type.isBlank()) {
            throw invalid("Type must name a registered device descriptor");
        }

        DeviceDescriptor<?> descriptor;
        try {
            descriptor = descriptors.require(type);
        } catch (IllegalArgumentException failure) {
            throw invalid("Unknown device descriptor: " + type, failure);
        }

        DeviceParameterConfig[] parameterConfigs = parameters == null ? EMPTY_PARAMETERS : parameters;
        int[] stableParameterIds = new int[parameterConfigs.length];
        int[] nativeParameterIds = new int[parameterConfigs.length];
        double[] parameterDefaults = new double[parameterConfigs.length];

        for (int index = 0; index < parameterConfigs.length; index++) {
            DeviceParameterConfig parameter = parameterConfigs[index];
            if (parameter == null) {
                throw invalid("Parameters must not contain null entries");
            }

            int stableId = parameter.getId();
            if (stableId < 0) {
                throw invalid("Parameter stable ID must be non-negative: " + stableId);
            }
            requireUniqueParameter(stableParameterIds, index, stableId);

            int nativeId = descriptor.parameters().nativeIndex(stableId);
            if (nativeId == MemberMapping.UNMAPPED) {
                throw invalid("Parameter stable ID is not mapped by descriptor " + type + ": " + stableId);
            }

            double defaultValue = parameter.getDefaultValue();
            if (!Double.isFinite(defaultValue)) {
                throw invalid("Parameter Default must be finite for stable ID " + stableId);
            }

            stableParameterIds[index] = stableId;
            nativeParameterIds[index] = nativeId;
            parameterDefaults[index] = defaultValue;
        }

        DevicePortConfig[] portConfigs = ports == null ? EMPTY_PORTS : ports;
        int[] portIds = new int[portConfigs.length];
        int[] stableTerminalIds = new int[portConfigs.length];
        int[] nativeTerminalIds = new int[portConfigs.length];
        PortDefinition<?, ?>[] portDefinitions = new PortDefinition[portConfigs.length];

        for (int index = 0; index < portConfigs.length; index++) {
            DevicePortConfig port = portConfigs[index];
            if (port == null) {
                throw invalid("Ports must not contain null entries");
            }

            int portId = port.getId();
            if (portId < 0) {
                throw invalid("Port Id must be non-negative: " + portId);
            }
            requireUniquePort(portIds, index, portId);

            int stableTerminalId = port.getTerminal();
            if (stableTerminalId < 0) {
                throw invalid("Terminal stable ID must be non-negative: " + stableTerminalId);
            }

            int nativeTerminalId = descriptor.terminals().nativeIndex(stableTerminalId);
            if (nativeTerminalId == MemberMapping.UNMAPPED) {
                throw invalid("Terminal stable ID is not mapped by descriptor " + type + ": " + stableTerminalId);
            }

            Vector3i normal = port.getNormal();
            if (normal == null || !DevicePortConfig.isCardinal(normal)) {
                throw invalid("Port " + portId + " Normal must be one of the six cardinal unit vectors");
            }

            Vector3i anchor = port.getAnchor();
            PortOffset offset = anchor == null
                    ? PortOffset.ZERO
                    : new PortOffset(anchor.x, anchor.y, anchor.z);

            portIds[index] = portId;
            stableTerminalIds[index] = stableTerminalId;
            nativeTerminalIds[index] = nativeTerminalId;
            portDefinitions[index] = new PortDefinition<>(
                    portId,
                    offset,
                    PortReach.single(normal.x, normal.y, normal.z),
                    conductorStandard,
                    new ElectricalPortProfile(normal.x, normal.y, normal.z)
            );
        }

        return new CompiledDeviceConfig(
                descriptor,
                stableParameterIds,
                nativeParameterIds,
                parameterDefaults,
                portIds,
                stableTerminalIds,
                nativeTerminalIds,
                BlockPortDefinition.of(portDefinitions)
        );
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

    private String displayId() {
        return id == null ? "<unknown>" : id;
    }

    public static AssetStore<String, DeviceConfig, DefaultAssetMap<String, DeviceConfig>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(DeviceConfig.class);
        }
        return ASSET_STORE;
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
