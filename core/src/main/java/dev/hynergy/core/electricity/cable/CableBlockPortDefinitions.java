package dev.hynergy.core.electricity.cable;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.BlockPortDefinition;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.core.port.PortStandard;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public final class CableBlockPortDefinitions {
    private final PortModule portModule;
    private final PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard;
    private final ComponentType<ChunkStore, CableComponent> cableComponentType;

    private final Map<CableConfig, BlockPortDefinition> compiledDefinitions = new IdentityHashMap<>();
    private final IntOpenHashSet publishedBlockTypes = new IntOpenHashSet();
    private final IntOpenHashSet rebuiltBlockTypes = new IntOpenHashSet();

    public CableBlockPortDefinitions(
            PortModule portModule,
            PortStandard<
                    ElectricalPortProfile,
                    ElectricalPortConnection
                    > conductorStandard,
            ComponentType<ChunkStore, CableComponent> cableComponentType
    ) {
        this.portModule =
                Objects.requireNonNull(portModule, "portModule");

        this.conductorStandard =
                Objects.requireNonNull(
                        conductorStandard,
                        "conductorStandard"
                );

        this.cableComponentType =
                Objects.requireNonNull(
                        cableComponentType,
                        "cableComponentType"
                );
    }

    public synchronized void rebuild() {
        compiledDefinitions.clear();
        rebuiltBlockTypes.clear();

        var blockTypes = BlockType.getAssetMap();
        var cableConfigs = CableConfig.getAssetMap();

        for (BlockType blockType : blockTypes.getAssetMap().values()) {
            var blockEntity = blockType.getBlockEntity();
            if (blockEntity == null) {
                continue;
            }

            CableComponent cableComponent =
                    blockEntity.getComponent(cableComponentType);

            if (cableComponent == null) {
                continue;
            }

            String configId = cableComponent.getConfigId();
            if (configId == null) {
                continue;
            }

            CableConfig config = cableConfigs.getAsset(configId);
            if (config == null) {
                continue;
            }

            int blockTypeIndex =
                    blockTypes.getIndex(blockType.getId());

            if (blockTypeIndex < 0) {
                throw new IllegalStateException(
                        "Loaded block type has no runtime index: "
                                + blockType.getId()
                );
            }

            BlockPortDefinition definition =
                    compiledDefinitions.get(config);

            if (definition == null) {
                definition =
                        config.buildPortDefinition(conductorStandard);

                compiledDefinitions.put(config, definition);
            }

            portModule.setBlockPorts(
                    blockTypeIndex,
                    definition
            );

            rebuiltBlockTypes.add(blockTypeIndex);
        }

        IntIterator iterator = publishedBlockTypes.iterator();
        while (iterator.hasNext()) {
            int blockTypeIndex = iterator.nextInt();

            if (!rebuiltBlockTypes.contains(blockTypeIndex)) {
                portModule.clearBlockPorts(blockTypeIndex);
            }
        }

        publishedBlockTypes.clear();
        publishedBlockTypes.addAll(rebuiltBlockTypes);
    }
}