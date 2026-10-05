package dev.hynergy.electrical;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Describes one type of electrical device.
 *
 * <p>The native definition supplies the electrical behavior. Java convenience
 * wrappers are independent of this type.</p>
 *
 * <p>Use {@link #create(Consumer)} to define a custom device
 * type. Register the type in an {@link ElectricalRuntime} before an
 * {@link ElectricalSystem} creates a device of that type.</p>
 */
public final class DeviceType {
    private final @Nullable DeviceDefinition primitiveDefinition;
    private final @Nullable Consumer<DeviceDefinitionBuilder> definitionBuilder;

    private volatile @Nullable Registration registration;

    private DeviceType(
        @Nullable DeviceDefinition primitiveDefinition,
        @Nullable Consumer<DeviceDefinitionBuilder> definitionBuilder
    ) {
        this.primitiveDefinition = primitiveDefinition;
        this.definitionBuilder = definitionBuilder;
    }

    /**
     * Creates a custom device type.
     *
     * <p>The runtime calls {@code definitionBuilder} when it registers the
     * type. The runtime owns the builder that it supplies to the callback.
     * Do not close the builder. Do not keep a reference to the builder after
     * the callback returns.</p>
     *
     * @param definitionBuilder the function that builds the electrical
     *     definition
     *
     * @return the device type
     *
     * @throws NullPointerException if an argument is null
     */
    public static DeviceType create(
        Consumer<DeviceDefinitionBuilder> definitionBuilder
    ) {
        return new DeviceType(null, Objects.requireNonNull(definitionBuilder, "definitionBuilder"));
    }

    public static DeviceType primitive(int definitionId) {
        return new DeviceType(new DeviceDefinition(definitionId), null);
    }

    void buildDefinition(DeviceDefinitionBuilder builder) {
        Consumer<DeviceDefinitionBuilder> definitionBuilder = this.definitionBuilder;

        if (definitionBuilder == null) {
            throw new IllegalStateException("Primitive device type does not have a composite definition");
        }

        definitionBuilder.accept(Objects.requireNonNull(builder, "builder"));
    }

    @Nullable DeviceDefinition currentDefinition() {
        DeviceDefinition primitiveDefinition = this.primitiveDefinition;

        if (primitiveDefinition != null) {
            return primitiveDefinition;
        }

        Registration registration = this.registration;

        return registration == null ? null : registration.definition();
    }

    @Nullable DeviceDefinition existingDefinition(ElectricalRuntime runtime) {
        Objects.requireNonNull(runtime, "runtime");

        DeviceDefinition primitiveDefinition = this.primitiveDefinition;

        if (primitiveDefinition != null) {
            return primitiveDefinition;
        }

        Registration registration = this.registration;

        if (registration == null || registration.runtime() != runtime) {
            return null;
        }

        return registration.definition();
    }

    DeviceDefinition requireDefinition(ElectricalRuntime runtime) {
        DeviceDefinition definition = existingDefinition(runtime);

        if (definition == null) {
            throw new IllegalStateException("Device type is not registered for this electrical runtime");
        }

        return definition;
    }

    synchronized void bind(ElectricalRuntime runtime, DeviceDefinition definition) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(definition, "definition");

        if (primitiveDefinition != null) {
            throw new IllegalStateException("Primitive device type cannot be runtime-bound");
        }

        Registration registration = this.registration;

        if (registration != null && registration.runtime() == runtime) {
            throw new IllegalStateException("Device type is already bound to this electrical runtime");
        }

        this.registration = new Registration(runtime, definition);
    }

    synchronized void unbind(ElectricalRuntime runtime) {
        Registration registration = this.registration;

        if (registration != null && registration.runtime() == runtime) {
            this.registration = null;
        }
    }

    private record Registration(ElectricalRuntime runtime, DeviceDefinition definition) {
    }
}
