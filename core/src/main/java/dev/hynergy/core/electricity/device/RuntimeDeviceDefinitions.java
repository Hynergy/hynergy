package dev.hynergy.core.electricity.device;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Runtime block-type index to compiled device configuration mapping.
 *
 * <p>Reads are allocation-free. Asset rebuilds may safely grow, replace, or
 * clear entries while runtime readers observe either the old or new immutable
 * configuration.</p>
 */
public final class RuntimeDeviceDefinitions {
    private volatile AtomicReferenceArray<CompiledDeviceConfig> definitions = new AtomicReferenceArray<>(16);

    public @Nullable CompiledDeviceConfig get(int blockTypeId) {
        if (blockTypeId < 0) {
            return null;
        }

        AtomicReferenceArray<CompiledDeviceConfig> current = definitions;
        return blockTypeId < current.length() ? current.get(blockTypeId) : null;
    }

    public synchronized void set(int blockTypeId, CompiledDeviceConfig definition) {
        if (blockTypeId < 0) {
            throw new IllegalArgumentException("blockTypeId must be non-negative");
        }
        Objects.requireNonNull(definition, "definition");

        AtomicReferenceArray<CompiledDeviceConfig> current = definitions;
        if (blockTypeId >= current.length()) {
            int newLength = current.length();
            while (newLength <= blockTypeId) {
                newLength = Math.multiplyExact(newLength, 2);
            }

            AtomicReferenceArray<CompiledDeviceConfig> grown = new AtomicReferenceArray<>(newLength);
            for (int index = 0; index < current.length(); index++) {
                grown.set(index, current.get(index));
            }
            definitions = grown;
            current = grown;
        }

        current.set(blockTypeId, definition);
    }

    public synchronized void clear(int blockTypeId) {
        if (blockTypeId < 0) {
            throw new IllegalArgumentException("blockTypeId must be non-negative");
        }

        AtomicReferenceArray<CompiledDeviceConfig> current = definitions;
        if (blockTypeId < current.length()) {
            current.set(blockTypeId, null);
        }
    }
}
