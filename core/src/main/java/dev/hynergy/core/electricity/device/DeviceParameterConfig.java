package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.Validators;
import lombok.Getter;

/**
 * Configures the default value for one device parameter by stable ID.
 */
@Getter
public final class DeviceParameterConfig {
    private static final Validator<Double> FINITE_VALUE_VALIDATOR = new Validator<>() {
        @Override
        public void accept(Double value, ValidationResults results) {
            if (value != null && !Double.isFinite(value)) {
                results.fail("Device parameter Default must be finite");
            }
        }

        @Override
        public void updateSchema(SchemaContext context, Schema target) {
        }
    };

    public static final BuilderCodec<DeviceParameterConfig> CODEC =
            BuilderCodec.builder(DeviceParameterConfig.class, DeviceParameterConfig::new)
                        .append(
                                new KeyedCodec<>("Id", Codec.INTEGER, true),
                                (parameter, id) -> parameter.id = id,
                                parameter -> parameter.id
                        )
                        .addValidator(Validators.greaterThanOrEqual(0))
                        .documentation("Stable parameter identifier from the device descriptor.")
                        .add()
                        .append(
                                new KeyedCodec<>("Default", Codec.DOUBLE, true),
                                (parameter, value) -> parameter.defaultValue = value,
                                parameter -> parameter.defaultValue
                        )
                        .addValidator(FINITE_VALUE_VALIDATOR)
                        .documentation("Configured default value for this device parameter.")
                        .add()
                        .build();

    private int id;
    private double defaultValue;

    private DeviceParameterConfig() {
    }

    DeviceParameterConfig(int id, double defaultValue) {
        this.id = id;
        this.defaultValue = defaultValue;
    }

}
