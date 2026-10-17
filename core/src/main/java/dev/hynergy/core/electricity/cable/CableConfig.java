package dev.hynergy.core.electricity.cable;

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
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import dev.hynergy.core.electricity.port.ElectricalPortConnection;
import dev.hynergy.core.electricity.port.ElectricalPortProfile;
import dev.hynergy.core.port.PortStandard;
import dev.hynergy.core.port.layout.BlockPortDefinition;
import dev.hynergy.core.port.layout.PortDefinition;
import dev.hynergy.core.port.layout.PortOffset;
import dev.hynergy.core.port.layout.PortReach;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;
import org.joml.Vector3i;

import java.util.Objects;

public class CableConfig implements JsonAssetWithMap<String, DefaultAssetMap<String, CableConfig>> {
    public static final String DATA_SET = "WireConfigs";

    private static final Validator<CablePortConfig[]> PORTS_VALIDATOR =
            new Validator<>() {
                @Override
                public void accept(
                        CablePortConfig[] ports,
                        ValidationResults results
                ) {
                    if (ports == null) {
                        return;
                    }

                    IntOpenHashSet ids =
                            new IntOpenHashSet(ports.length);

                    for (CablePortConfig port : ports) {
                        if (port == null) {
                            results.fail(
                                    "Wire ports must not contain null entries"
                            );
                            continue;
                        }

                        if (!ids.add(port.getId())) {
                            results.fail(
                                    "Duplicate wire port Id: "
                                            + port.getId()
                            );
                        }
                    }
                }

                @Override
                public void updateSchema(
                        SchemaContext context,
                        Schema target
                ) {
                }
            };

    public static final AssetBuilderCodec<String, CableConfig> CODEC =
            AssetBuilderCodec.builder(
                                     CableConfig.class,
                                     CableConfig::new,
                                     Codec.STRING,

                                     (config, id) -> config.id = id,
                                     config -> config.id,

                                     (config, data) -> config.extraData = data,
                                     config -> config.extraData
                             )
                             .append(
                                     new KeyedCodec<>(
                                             "Ports",
                                             new ArrayCodec<>(
                                                     CablePortConfig.CODEC,
                                                     CablePortConfig[]::new
                                             ),
                                             true
                                     ),
                                     (config, ports) -> config.ports = ports,
                                     config -> config.ports
                             )
                             .addValidator(Validators.nonEmptyArray())
                             .addValidator(PORTS_VALIDATOR)
                             .documentation(
                                     "The electrical connection ports exposed by this wire."
                             )
                             .add()
                             .build();

    public static final ValidatorCache<String> VALIDATOR_CACHE =
            new ValidatorCache<>(
                    new AssetKeyValidator<>(
                            CableConfig::getAssetStore
                    )
            );

    private static AssetStore<
            String,
            CableConfig,
            DefaultAssetMap<String, CableConfig>
            > ASSET_STORE;


    private AssetExtraInfo.Data extraData;

    @Getter private String id;
    @Getter private CablePortConfig[] ports;


    private CableConfig() {
    }


    public BlockPortDefinition buildPortDefinition(
            PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard
    ) {
        Objects.requireNonNull(conductorStandard, "conductorStandard");

        PortDefinition<?, ?>[] definitions = new PortDefinition[ports.length];
        for (int index = 0; index < ports.length; index++) {
            CablePortConfig port = ports[index];
            Vector3i anchor = port.getAnchor();
            Vector3i normal = port.getNormal();

            definitions[index] = new PortDefinition<>(
                    port.getId(),
                    anchor == null
                            ? PortOffset.ZERO
                            : new PortOffset(anchor.x, anchor.y, anchor.z),
                    PortReach.single(normal.x, normal.y, normal.z),
                    conductorStandard,
                    new ElectricalPortProfile(normal.x, normal.y, normal.z)
            );
        }

        return BlockPortDefinition.of(definitions);
    }

    public static AssetStore<
            String,
            CableConfig,
            DefaultAssetMap<String, CableConfig>
            > getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE =
                    AssetRegistry.getAssetStore(
                            CableConfig.class
                    );
        }

        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, CableConfig>

    getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public static void populateDataSet(
            AssetEditorRequestDataSetEvent event
    ) {
        String[] ids =
                getAssetMap()
                        .getAssetMap()
                        .keySet()
                        .stream()
                        .sorted()
                        .toArray(String[]::new);

        event.setResults(ids);
    }
}
