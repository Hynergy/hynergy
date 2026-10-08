package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.exception.CodecValidationException;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import dev.hynergy.electrical.primitives.passive.Resistance;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DeviceConfigCodecTest {
    private dev.hynergy.electrical.ElectricalRuntime runtime;
    @org.junit.jupiter.api.BeforeEach void setupRuntime() { runtime = dev.hynergy.electrical.ElectricalRuntime.create(); }
    @org.junit.jupiter.api.AfterEach void closeRuntime() { runtime.close(); }
    @Test
    void decodingRejectsUnknownTypesEvenWithoutADeviceBlock() {
        var codec = DeviceConfig.createCodec(new DeviceRegistry(runtime));
        assertThrows(CodecValidationException.class, () -> codec.decode(
                BsonDocument.parse("{\"Type\":\"test:unknown\"}"), new ExtraInfo()
        ));
    }

    @Test
    void codecUsesRegistrationsMadeAfterConstructionAndAfterFreeze() {
        DeviceRegistry registry = new DeviceRegistry(runtime);
        var codec = DeviceConfig.createCodec(registry);
        registry.register("test:resistance", Resistance.TYPE);

        BsonDocument json = BsonDocument.parse("{\"Type\":\"test:resistance\"}");
        assertEquals("test:resistance", codec.decode(json, new ExtraInfo()).getType());
        registry.freeze();
        assertEquals("test:resistance", codec.decode(json, new ExtraInfo()).getType());
        assertThrows(CodecValidationException.class, () -> codec.decode(
                BsonDocument.parse("{\"Type\":\"test:unknown\"}"), new ExtraInfo()
        ));
    }

    @Test
    void typeSchemaUsesTheDeviceTypesDropdown() {
        var schema = DeviceConfig.createCodec(new DeviceRegistry(runtime)).toSchema(new SchemaContext());
        var editor = schema.getProperties().get("Type").getHytale().getUiEditorComponent();
        assertInstanceOf(UIEditor.Dropdown.class, editor);
        var json = UIEditor.Dropdown.CODEC.encode((UIEditor.Dropdown) editor).asDocument();
        assertEquals("DeviceTypes", json.getString("dataSet").getValue());
    }

    @Test
    void codecValidationUsesOnlyItsOwnRegistry() {
        DeviceRegistry registry = new DeviceRegistry(runtime);
        registry.register("test:resistance", Resistance.TYPE);
        var firstCodec = DeviceConfig.createCodec(registry);
        var secondCodec = DeviceConfig.createCodec(new DeviceRegistry(runtime));
        BsonDocument json = BsonDocument.parse("{\"Type\":\"test:resistance\"}");

        assertEquals("test:resistance", firstCodec.decode(json, new ExtraInfo()).getType());
        assertThrows(CodecValidationException.class, () -> secondCodec.decode(json, new ExtraInfo()));
    }
}
