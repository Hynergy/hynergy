package dev.hynergy.core.electricity.device;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hynergy.core.electricity.ElectricalCodecs;
import dev.hynergy.electrical.Device;
import dev.hynergy.electrical.DeviceId;
import dev.hynergy.electrical.ObservationListener;
import dev.hynergy.electrical.ObservationSubscription;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Generic ECS component for one Hytale entity participating as an electrical device.
 */
public final class DeviceComponent implements Component<ChunkStore> {
    private static final ArrayCodec<DeviceParameterOverride> OVERRIDES_CODEC =
            new ArrayCodec<>(DeviceParameterOverride.CODEC, DeviceParameterOverride[]::new);

    public static final BuilderCodec<DeviceComponent> CODEC =
            BuilderCodec.builder(DeviceComponent.class, DeviceComponent::new)
                        .append(
                                new KeyedCodec<>("Config", Codec.STRING),
                                DeviceComponent::setConfigId,
                                DeviceComponent::getConfigId
                        )
                        .metadata(new UIEditor(new UIEditor.Dropdown(DeviceConfig.DATA_SET)))
                        .addValidator(Validators.nonNull())
                        .addValidator(Validators.nonEmptyString())
                        .addValidatorLate(() -> DeviceConfig.VALIDATOR_CACHE.getValidator().late())
                        .add()
                        .append(
                                new KeyedCodec<>("DeviceId", ElectricalCodecs.DEVICE_ID),
                                (component, id) -> component.deviceId = id,
                                component -> component.deviceId
                        )
                        .add()
                        .append(
                                new KeyedCodec<>("Overrides", OVERRIDES_CODEC),
                                (component, serialized) -> component.overrides = ParameterOverrides.fromSerialized(serialized),
                                component -> component.overrides.toSerialized()
                        )
                        .add()
                        .build();

    @Getter private String configId;
    private @Nullable DeviceId deviceId;
    private ParameterOverrides overrides = new ParameterOverrides();

    private transient @Nullable Device device;
    private transient @Nullable CompiledDeviceConfig compiledConfig;
    private transient @Nullable Runnable markNeedsSaving;

    private DeviceComponent() {
    }

    DeviceComponent(String configId) {
        this.configId = Objects.requireNonNull(configId, "configId");
    }

    public void setConfigId(String configId) {
        this.configId = Objects.requireNonNull(configId, "configId");
    }

    public @Nullable DeviceId getDeviceId() {
        return deviceId;
    }

    /**
     * Sets a configured parameter by stable parameter ID.
     *
     * <p>Native constraints are checked before the mutation is queued. A rejected
     * value does not change the override. An accepted value is queued intent;
     * the electrical system applies it during its next tick. Changes to the
     * persisted override mark the block for saving. Setting the exact configured
     * default removes the override.</p>
     */
    public void setParameter(int stableParameterId, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Device parameter value must be finite");
        }

        CompiledDeviceConfig compiled = requireCompiledConfig();
        Device device = requireDevice();
        CompiledDeviceConfig.ParameterBinding parameter = compiled.parameter(stableParameterId);
        if (parameter == null) {
            throw new IllegalArgumentException(
                    "Stable parameter ID is not configurable by this device config: " + stableParameterId
            );
        }

        device.setParameter(parameter.nativeId(), value);

        boolean changed;
        if (Double.doubleToLongBits(value) == Double.doubleToLongBits(parameter.defaultValue())) {
            changed = overrides.remove(stableParameterId);
        } else {
            changed = !overrides.contains(stableParameterId)
                    || Double.doubleToLongBits(overrides.getOrDefault(stableParameterId, value))
                    != Double.doubleToLongBits(value);
            overrides.set(stableParameterId, value);
        }
        if (changed) {
            Objects.requireNonNull(markNeedsSaving, "markNeedsSaving").run();
        }
    }

    /**
     * Creates an observation subscription by stable observer ID.
     */
    public ObservationSubscription observe(int stableObserverId, ObservationListener listener) {
        CompiledDeviceConfig compiled = requireCompiledConfig();
        Device device = requireDevice();
        int nativeObserverId = compiled.nativeObserverId(stableObserverId);
        if (nativeObserverId == MemberMapping.UNMAPPED) {
            throw new IllegalArgumentException("Unknown or retired stable observer ID: " + stableObserverId);
        }
        return device.observe(nativeObserverId, listener);
    }

    @Override
    public DeviceComponent cloneSerializable() {
        DeviceComponent cloned = new DeviceComponent(configId);
        cloned.deviceId = deviceId;
        cloned.overrides = overrides.copy();
        return cloned;
    }

    @Override
    public @NonNull DeviceComponent clone() {
        try {
            DeviceComponent cloned = (DeviceComponent) super.clone();
            cloned.deviceId = null;
            cloned.overrides = overrides.copy();
            cloned.device = null;
            cloned.compiledConfig = null;
            cloned.markNeedsSaving = null;
            return cloned;
        } catch (CloneNotSupportedException failure) {
            throw new AssertionError(failure);
        }
    }

    ParameterOverrides overrides() {
        return overrides;
    }

    @Nullable Device device() {
        return device;
    }

    @Nullable CompiledDeviceConfig compiledConfig() {
        return compiledConfig;
    }

    void setDeviceId(@Nullable DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    void bindRuntime(Device device, CompiledDeviceConfig compiledConfig, Runnable markNeedsSaving) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(compiledConfig, "compiledConfig");
        Objects.requireNonNull(markNeedsSaving, "markNeedsSaving");
        if (this.device != null || this.compiledConfig != null) {
            throw new IllegalStateException("Device component is already runtime-bound");
        }
        this.device = device;
        this.compiledConfig = compiledConfig;
        this.markNeedsSaving = markNeedsSaving;
    }

    void clearRuntime() {
        device = null;
        compiledConfig = null;
        markNeedsSaving = null;
    }

    private Device requireDevice() {
        Device device = this.device;
        if (device == null) {
            throw new IllegalStateException("Device component is not runtime-bound");
        }
        return device;
    }

    private CompiledDeviceConfig requireCompiledConfig() {
        CompiledDeviceConfig compiled = this.compiledConfig;
        if (compiled == null) {
            throw new IllegalStateException("Device component has no compiled runtime config");
        }
        return compiled;
    }
}
