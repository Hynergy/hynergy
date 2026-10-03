package dev.hynergy.core.electricity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.record.RecordCodec;
import dev.hynergy.electrical.DeviceId;
import dev.hynergy.electrical.WireId;

public final class ElectricalCodecs {

    public static final RecordCodec<WireId> WIRE_ID =
            RecordCodec.builder(WireId.class)
                       .append(
                               new KeyedCodec<>("Id", Codec.INTEGER),
                               WireId::value
                       )
                       .append(
                               new KeyedCodec<>("Generation", Codec.INTEGER),
                               WireId::generation
                       )
                       .build(WireId::new);

    public static final RecordCodec<DeviceId> DEVICE_ID =
            RecordCodec.builder(DeviceId.class)
                       .append(
                               new KeyedCodec<>("Id", Codec.INTEGER),
                               DeviceId::value
                       )
                       .append(
                               new KeyedCodec<>("Generation", Codec.INTEGER),
                               DeviceId::generation
                       )
                       .build(DeviceId::new);

    private ElectricalCodecs() {
    }
}