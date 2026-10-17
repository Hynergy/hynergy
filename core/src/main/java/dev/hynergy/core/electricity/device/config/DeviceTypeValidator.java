package dev.hynergy.core.electricity.device.config;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import dev.hynergy.core.electricity.device.DeviceRegistry;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

final class DeviceTypeValidator implements Validator<@Nullable String> {

    private final DeviceRegistry devices;

    DeviceTypeValidator(DeviceRegistry devices) {
        this.devices = Objects.requireNonNull(devices, "devices");
    }

    @Override
    public void accept(@Nullable String id, ValidationResults results) {
        if (id == null || devices.get(id) == null) {
            results.fail("Unknown device type: " + id);
        }
    }

    @Override
    public void updateSchema(SchemaContext context, Schema target) {
    }
}