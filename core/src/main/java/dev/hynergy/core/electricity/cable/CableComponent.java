package dev.hynergy.core.electricity.cable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDisplayMode;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.electricity.ElectricalCodecs;
import dev.hynergy.electrical.Wire;
import dev.hynergy.electrical.WireId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NoArgsConstructor
public class CableComponent implements Component<ChunkStore> {

    public static final BuilderCodec<CableComponent> CODEC =
            BuilderCodec.builder(CableComponent.class, CableComponent::new)
                        .append(
                                new KeyedCodec<>("Config", Codec.STRING),
                                CableComponent::setConfigId,
                                CableComponent::getConfigId)
                        .metadata(new UIEditor(new UIEditor.Dropdown(CableConfig.DATA_SET)))
                        .addValidator(Validators.nonNull())
                        .addValidator(Validators.nonEmptyString())
                        .addValidatorLate(() -> CableConfig.VALIDATOR_CACHE.getValidator().late())
                        .add()
                        .append(
                                new KeyedCodec<>("WireId", ElectricalCodecs.WIRE_ID),
                                (component, id) -> component.wireId = id,
                                component -> component.wireId)
                        .metadata(UIDisplayMode.HIDDEN)
                        .add()
                        .build();

    @Getter private @Nullable UUID systemId;

    @Getter @Setter private String configId;
    @Getter private @Nullable WireId wireId;
    @Getter private transient @Nullable Wire wire;

    private CableComponent(String configId) {
        this.configId = configId;
    }

    void setSystemId(@Nullable UUID systemId) {
        this.systemId = systemId;
    }

    void bindRuntime(@Nullable Wire wire) {
        this.wire = wire;
    }

    void setWireId(@Nullable WireId wireId) {
        this.wireId = wireId;
    }

    @Override
    public Component<ChunkStore> cloneSerializable() {
        CableComponent cloned = new CableComponent(configId);
        cloned.wireId = wireId;
        return cloned;
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        try {
            CableComponent cloned = (CableComponent) super.clone();

            cloned.wireId = null;
            cloned.wire = null;

            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    void clearRuntime() {
        wire = null;
    }
}
