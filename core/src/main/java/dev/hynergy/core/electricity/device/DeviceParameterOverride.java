package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.record.RecordCodec;

/**
 * Serialized form of one stable device parameter override.
 *
 * <p>Runtime storage remains primitive in {@link ParameterOverrides}; instances
 * of this record are created only at persistence codec boundaries.</p>
 */
public record DeviceParameterOverride(int id, double value) {
    public static final RecordCodec<DeviceParameterOverride> CODEC =
            RecordCodec.builder(DeviceParameterOverride.class)
                       .append(new KeyedCodec<>("Id", Codec.INTEGER), DeviceParameterOverride::id)
                       .append(new KeyedCodec<>("Value", Codec.DOUBLE), DeviceParameterOverride::value)
                       .build(DeviceParameterOverride::new);

    public DeviceParameterOverride {
        if (id < 0) {
            throw new IllegalArgumentException("Stable parameter ID must be non-negative");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Device parameter override must be finite");
        }
    }
}
