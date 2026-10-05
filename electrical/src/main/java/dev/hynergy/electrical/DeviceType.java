package dev.hynergy.electrical;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable electrical declaration. Stable member IDs are independent of native indexes.
 */
public final class DeviceType {
    private final DeviceDeclaration declaration;
    private final int primitiveId;

    private DeviceType(DeviceDeclaration declaration, int primitiveId) {
        this.declaration = declaration;
        this.primitiveId = primitiveId;
    }

    public static DeviceType define(Consumer<DeviceTypeBuilder> definition) {
        return define(0, definition);
    }

    static DeviceType define(int primitiveId, Consumer<DeviceTypeBuilder> definition) {
        Objects.requireNonNull(definition, "definition");
        var builder = new DeviceTypeBuilder();
        try {
            definition.accept(builder);
            return new DeviceType(builder.finish(), primitiveId);
        } finally {
            builder.close();
        }
    }

    DeviceDeclaration declaration() {
        return declaration;
    }

    int primitiveId() {
        return primitiveId;
    }

    public List<DeviceParameter> parameters() {
        return declaration.parameters();
    }

    public List<DeviceTerminal> terminals() {
        return declaration.terminals();
    }

    public List<DeviceObserver> observers() {
        return declaration.observers();
    }

    public DeviceParameter parameter(int id) {
        return parameters().stream().filter(p -> p.id() == id).findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("Unknown parameter ID: " + id));
    }

    public DeviceTerminal terminal(int id) {
        return terminals().stream().filter(p -> p.id() == id).findFirst()
                          .orElseThrow(() -> new IllegalArgumentException("Unknown terminal ID: " + id));
    }

    public DeviceObserver observer(int id) {
        return observers().stream().filter(p -> p.id() == id).findFirst()
                          .orElseThrow(() -> new IllegalArgumentException("Unknown observer ID: " + id));
    }

    public Metadata metadata() {
        return new Metadata(parameterCount(), terminalCount(), observerCount());
    }

    public int parameterCount() {
        return parameters().size();
    }

    public int terminalCount() {
        return terminals().size();
    }

    public int observerCount() {
        return observers().size();
    }

    public record Metadata(int parameterCount, int terminalCount, int observerCount) {
        public Metadata {
            if (parameterCount < 0 || terminalCount < 0 || observerCount < 0)
                throw new IllegalArgumentException("Device member counts must be non-negative");
        }

        void requireParameter(int index) {
            require(index, parameterCount, "parameter");
        }

        void requireTerminal(int index) {
            require(index, terminalCount, "terminal");
        }

        void requireObserver(int index) {
            require(index, observerCount, "observer");
        }

        private static void require(int index, int count, String kind) {
            if (index < 0 || index >= count) throw new IllegalArgumentException("Invalid " + kind + " index: " + index);
        }
    }
}
