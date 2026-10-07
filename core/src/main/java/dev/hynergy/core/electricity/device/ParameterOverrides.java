package dev.hynergy.core.electricity.device;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Stores mutable instance overrides by stable parameter ID.
 *
 * <p>Device instances usually override few parameters.
 * Linear search avoids a separate map and boxed values for each device.</p>
 */
public final class ParameterOverrides {
    private static final int[] EMPTY_IDS = new int[0];
    private static final double[] EMPTY_VALUES = new double[0];

    private int[] ids = EMPTY_IDS;
    private double[] values = EMPTY_VALUES;
    private int size;

    public int size() {
        return size;
    }

    public int stableIdAt(int index) {
        checkIndex(index);
        return ids[index];
    }

    public double valueAt(int index) {
        checkIndex(index);
        return values[index];
    }

    public boolean contains(int stableId) {
        return indexOf(stableId) >= 0;
    }

    public double getOrDefault(int stableId, double defaultValue) {
        int index = indexOf(stableId);
        return index < 0 ? defaultValue : values[index];
    }

    public void set(int stableId, double value) {
        requireStableId(stableId);
        requireFinite(value);

        int index = indexOf(stableId);
        if (index >= 0) {
            values[index] = value;
            return;
        }

        ensureCapacity(size + 1);
        ids[size] = stableId;
        values[size] = value;
        size++;
    }

    public boolean remove(int stableId) {
        requireStableId(stableId);

        int index = indexOf(stableId);
        if (index < 0) {
            return false;
        }

        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(ids, index + 1, ids, index, moved);
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        return true;
    }

    public ParameterOverrides copy() {
        ParameterOverrides copy = new ParameterOverrides();
        copy.ids = size == 0 ? EMPTY_IDS : Arrays.copyOf(ids, size);
        copy.values = size == 0 ? EMPTY_VALUES : Arrays.copyOf(values, size);
        copy.size = size;
        return copy;
    }

    DeviceParameterOverride[] toSerialized() {
        DeviceParameterOverride[] serialized = new DeviceParameterOverride[size];
        for (int index = 0; index < size; index++) {
            serialized[index] = new DeviceParameterOverride(ids[index], values[index]);
        }
        return serialized;
    }

    static ParameterOverrides fromSerialized(@Nullable DeviceParameterOverride[] serialized) {
        ParameterOverrides overrides = new ParameterOverrides();
        if (serialized == null || serialized.length == 0) {
            return overrides;
        }

        overrides.ids = new int[serialized.length];
        overrides.values = new double[serialized.length];

        for (DeviceParameterOverride entry : serialized) {
            Objects.requireNonNull(entry, "Device parameter overrides must not contain null entries");
            if (overrides.contains(entry.id())) {
                throw new IllegalArgumentException("Duplicate device parameter override Id: " + entry.id());
            }
            overrides.ids[overrides.size] = entry.id();
            overrides.values[overrides.size] = entry.value();
            overrides.size++;
        }
        return overrides;
    }

    private int indexOf(int stableId) {
        if (stableId < 0) {
            return -1;
        }
        for (int index = 0; index < size; index++) {
            if (ids[index] == stableId) {
                return index;
            }
        }
        return -1;
    }

    private void ensureCapacity(int required) {
        if (required <= ids.length) {
            return;
        }

        int newCapacity = ids.length == 0 ? 4 : ids.length << 1;
        if (newCapacity < required) {
            newCapacity = required;
        }
        ids = Arrays.copyOf(ids, newCapacity);
        values = Arrays.copyOf(values, newCapacity);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Override index out of bounds: " + index);
        }
    }

    private static void requireStableId(int stableId) {
        if (stableId < 0) {
            throw new IllegalArgumentException("Stable parameter ID must be non-negative");
        }
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Device parameter override must be finite");
        }
    }
}
