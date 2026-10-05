package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;

final class DeviceTypeValidator implements Validator<String> {
    private final DeviceRegistry devices;

    DeviceTypeValidator(DeviceRegistry devices) {
        this.devices = devices;
    }

    @Override
    public void accept(String id, ValidationResults results) {
        if (id == null || devices.get(id) == null) {
            results.fail("Unknown device type: " + id);
        }
    }

    @Override
    public void updateSchema(SchemaContext context, Schema target) {
        // The dropdown dataset supplies IDs without capturing setup-time registrations.
    }
}
