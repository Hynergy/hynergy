package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;

final class DeviceDescriptorValidator implements Validator<String> {
    private final DeviceDescriptorRegistry descriptors;

    DeviceDescriptorValidator(DeviceDescriptorRegistry descriptors) {
        this.descriptors = descriptors;
    }

    @Override
    public void accept(String id, ValidationResults results) {
        if (id == null || descriptors.get(id) == null) {
            results.fail("Unknown device descriptor: " + id);
        }
    }

    @Override
    public void updateSchema(SchemaContext context, Schema target) {
        // The dropdown dataset supplies IDs without capturing setup-time registrations.
    }
}
