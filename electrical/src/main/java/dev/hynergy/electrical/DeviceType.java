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
    private final @Nullable Metadata primitiveMetadata;
    private final @Nullable Consumer<DeviceDefinitionBuilder> definitionBuilder;

    private volatile @Nullable Registration registration;

    private DeviceType(
            @Nullable DeviceDefinition primitiveDefinition,
            @Nullable Metadata primitiveMetadata,
            @Nullable Consumer<DeviceDefinitionBuilder> definitionBuilder
    ) {
        this.primitiveDefinition = primitiveDefinition;
        this.primitiveMetadata = primitiveMetadata;
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
     *                          definition
     * @return the device type
     * @throws NullPointerException if an argument is null
     */
    public static DeviceType create(
            Consumer<DeviceDefinitionBuilder> definitionBuilder
    ) {
        return new DeviceType(null, null, Objects.requireNonNull(definitionBuilder, "definitionBuilder"));
    }

    public static DeviceType primitive(int definitionId) {
        Metadata metadata = switch (definitionId) {
            case 1, 2, 3, 4, 7, 8 -> new Metadata(1, 2, 2);
            case 5, 6 -> new Metadata(1, 4, 3);
            case 9, 13 -> new Metadata(3, 4, 3);
            case 10 -> new Metadata(4, 3, 3);
            case 11 -> new Metadata(0, 4, 3);
            case 12 -> new Metadata(2, 2, 2);
            case 14, 15, 16, 17 -> new Metadata(3, 5, 4);
            case 18 -> new Metadata(4, 4, 3);
            default -> throw new IllegalArgumentException("Unknown primitive definition ID: " + definitionId);
        };
        return new DeviceType(new DeviceDefinition(definitionId), metadata, null);
    }

    public Metadata metadata() {
        if (primitiveMetadata != null) {
            return primitiveMetadata;
        }
        Registration registration = this.registration;
        if (registration == null) {
            throw new IllegalStateException("Device type is not registered");
        }
        return registration.metadata();
    }

    public int parameterCount() {
        return metadata().parameterCount();
    }

    public int terminalCount() {
        return metadata().terminalCount();
    }

    public int observerCount() {
        return metadata().observerCount();
    }

    public void validateParameter(int parameterId, double value) {
        Registration registration = this.registration;
        if (registration == null) {
            throw new IllegalStateException("Device type is not registered");
        }
        registration.metadata().requireParameter(parameterId);
        registration.runtime().validateParameter(registration.definition(), parameterId, value);
    }

    boolean registeredWith(ElectricalRuntime runtime) {
        Registration registration = this.registration;
        return registration != null && registration.runtime() == runtime;
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

    synchronized void bind(ElectricalRuntime runtime, DeviceDefinition definition, Metadata metadata) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(definition, "definition");


        Registration registration = this.registration;

        if (registration != null && registration.runtime() == runtime) {
            throw new IllegalStateException("Device type is already bound to this electrical runtime");
        }

        this.registration = new Registration(runtime, definition, Objects.requireNonNull(metadata, "metadata"));
    }

    synchronized void unbind(ElectricalRuntime runtime) {
        Registration registration = this.registration;

        if (registration != null && registration.runtime() == runtime) {
            this.registration = null;
        }
    }

    public record Metadata(int parameterCount, int terminalCount, int observerCount) {
        public Metadata {
            if (parameterCount < 0 || terminalCount < 0 || observerCount < 0) {
                throw new IllegalArgumentException("Device member counts must be non-negative");
            }
        }

        void requireParameter(int id) {
            requireMember(id, parameterCount, "parameter");
        }

        void requireTerminal(int id) {
            requireMember(id, terminalCount, "terminal");
        }

        private static void requireMember(int id, int count, String member) {
            if (id < 0 || id >= count) {
                throw new IllegalArgumentException("Device " + member + " index " + id
                        + " is outside [0, " + count + ")");
            }
        }
    }

    private record Registration(ElectricalRuntime runtime, DeviceDefinition definition, Metadata metadata) {
    }
}
