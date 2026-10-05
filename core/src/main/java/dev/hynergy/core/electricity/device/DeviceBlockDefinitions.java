package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.electricity.ElectricalPortConnection;
import dev.hynergy.core.electricity.ElectricalPortProfile;
import dev.hynergy.core.port.PortModule;
import dev.hynergy.core.port.PortStandard;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Publishes immutable compiled device configurations for runtime block indexes.
 *
 * <p>Asset scanning and compilation are cold-path work. Every rebuild compiles
 * each distinct {@link DeviceConfig} once, publishes all replacement mappings,
 * and only then clears indexes that disappeared from the new snapshot.</p>
 */
public final class DeviceBlockDefinitions {
    private final ComponentType<ChunkStore, DeviceComponent> deviceComponentType;
    private final Publisher publisher;
    private final ArrayList<Binding> bindings = new ArrayList<>();

    public DeviceBlockDefinitions(
            DeviceDescriptorRegistry descriptors,
            PortModule portModule,
            PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard,
            ComponentType<ChunkStore, DeviceComponent> deviceComponentType
    ) {
        this.deviceComponentType = Objects.requireNonNull(deviceComponentType, "deviceComponentType");
        this.publisher = new Publisher(descriptors, portModule, conductorStandard);
    }

    /**
     * Returns the published configuration for a runtime block type.
     */
    public @Nullable CompiledDeviceConfig get(int blockTypeId) {
        return publisher.get(blockTypeId);
    }

    public synchronized void rebuild() {
        bindings.clear();

        var blockTypes = BlockType.getAssetMap();
        var deviceConfigs = DeviceConfig.getAssetMap();

        for (BlockType blockType : blockTypes.getAssetMap().values()) {
            var blockEntity = blockType.getBlockEntity();
            if (blockEntity == null) {
                continue;
            }

            DeviceComponent component = blockEntity.getComponent(deviceComponentType);
            if (component == null) {
                continue;
            }

            String configId = component.getConfigId();
            if (configId == null) {
                continue;
            }

            DeviceConfig config = deviceConfigs.getAsset(configId);
            if (config == null) {
                continue;
            }

            int blockTypeIndex = blockTypes.getIndex(blockType.getId());
            if (blockTypeIndex < 0) {
                throw new IllegalStateException(
                        "Loaded block type has no runtime index: " + blockType.getId()
                );
            }

            bindings.add(new Binding(blockTypeIndex, config));
        }

        publisher.rebuild(bindings);
    }

    record Binding(int blockTypeIndex, DeviceConfig config) {
        Binding {
            if (blockTypeIndex < 0) {
                throw new IllegalArgumentException("blockTypeIndex must be non-negative");
            }
            Objects.requireNonNull(config, "config");
        }
    }

    interface Sink {
        void set(int blockTypeIndex, CompiledDeviceConfig compiled);

        void clear(int blockTypeIndex);
    }

    static final class Publisher {
        private volatile AtomicReferenceArray<CompiledDeviceConfig> definitions = new AtomicReferenceArray<>(16);

        @Nullable CompiledDeviceConfig get(int blockTypeId) {
            if (blockTypeId < 0) {
                return null;
            }
            AtomicReferenceArray<CompiledDeviceConfig> current = definitions;
            return blockTypeId < current.length() ? current.get(blockTypeId) : null;
        }

        private void publish(int blockTypeId, CompiledDeviceConfig compiled) {
            AtomicReferenceArray<CompiledDeviceConfig> current = definitions;
            if (blockTypeId >= current.length()) {
                int newLength = current.length();
                while (blockTypeId >= newLength) {
                    newLength = Math.multiplyExact(newLength, 2);
                }
                var grown = new AtomicReferenceArray<CompiledDeviceConfig>(newLength);
                for (int index = 0; index < current.length(); index++) {
                    grown.set(index, current.get(index));
                }
                // Publish the new entry before readers can observe the grown table.
                grown.set(blockTypeId, compiled);
                definitions = grown;
            } else {
                current.set(blockTypeId, compiled);
            }
        }

        private final DeviceDescriptorRegistry descriptors;
        private final PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard;
        private final Sink sink;

        private final Map<DeviceConfig, CompiledDeviceConfig> compiledDefinitions = new IdentityHashMap<>();
        private final IntOpenHashSet publishedBlockTypes = new IntOpenHashSet();
        private final IntOpenHashSet rebuiltBlockTypes = new IntOpenHashSet();
        private final ArrayList<PreparedBinding> preparedBindings = new ArrayList<>();

        Publisher(
                DeviceDescriptorRegistry descriptors,
                PortModule portModule,
                PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard
        ) {
            this(
                    descriptors,
                    conductorStandard,
                    new RuntimeSink(portModule)
            );
        }

        Publisher(
                DeviceDescriptorRegistry descriptors,
                PortStandard<ElectricalPortProfile, ElectricalPortConnection> conductorStandard,
                Sink sink
        ) {
            this.descriptors = Objects.requireNonNull(descriptors, "descriptors");
            this.conductorStandard = Objects.requireNonNull(conductorStandard, "conductorStandard");
            this.sink = Objects.requireNonNull(sink, "sink");
        }

        synchronized void rebuild(Iterable<Binding> bindings) {
            Objects.requireNonNull(bindings, "bindings");
            if (!descriptors.isFrozen()) {
                throw new IllegalStateException(
                        "Device descriptor registry must be frozen before runtime publication"
                );
            }

            compiledDefinitions.clear();
            rebuiltBlockTypes.clear();
            preparedBindings.clear();

            for (Binding binding : bindings) {
                Objects.requireNonNull(binding, "bindings must not contain null entries");
                DeviceConfig config = binding.config();
                CompiledDeviceConfig compiled = compiledDefinitions.get(config);
                if (compiled == null) {
                    compiled = config.compile(descriptors, conductorStandard);
                    compiledDefinitions.put(config, compiled);
                }
                preparedBindings.add(new PreparedBinding(binding.blockTypeIndex(), compiled));
            }

            for (PreparedBinding binding : preparedBindings) {
                publish(binding.blockTypeIndex(), binding.compiled());
                sink.set(binding.blockTypeIndex(), binding.compiled());
                rebuiltBlockTypes.add(binding.blockTypeIndex());
            }

            IntIterator iterator = publishedBlockTypes.iterator();
            while (iterator.hasNext()) {
                int blockTypeIndex = iterator.nextInt();
                if (!rebuiltBlockTypes.contains(blockTypeIndex)) {
                    definitions.set(blockTypeIndex, null);
                    sink.clear(blockTypeIndex);
                }
            }

            publishedBlockTypes.clear();
            publishedBlockTypes.addAll(rebuiltBlockTypes);
        }
    }

    private record PreparedBinding(int blockTypeIndex, CompiledDeviceConfig compiled) {
    }

    private record RuntimeSink(PortModule portModule) implements Sink {
        private RuntimeSink(PortModule portModule) {
            this.portModule = Objects.requireNonNull(portModule, "portModule");
        }

        @Override
        public void set(int blockTypeIndex, CompiledDeviceConfig compiled) {
            portModule.setBlockPorts(blockTypeIndex, compiled.portDefinition());
        }

        @Override
        public void clear(int blockTypeIndex) {
            portModule.clearBlockPorts(blockTypeIndex);
        }
    }
}
