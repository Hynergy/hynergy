package dev.hynergy.electrical;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Binds each child member once. Authoring order does not determine native indexes.
 */
public final class DeviceElementBuilder {
    private final DeviceTypeBuilder parent;
    private final DeviceElement reference;
    private final Map<DeviceTerminal, NodeReference> terminals = new IdentityHashMap<>();
    private final Map<DeviceParameter, DeviceDeclaration.Value> parameters = new IdentityHashMap<>();
    private boolean open = true;

    DeviceElementBuilder(DeviceTypeBuilder parent, DeviceElement reference) {
        this.parent = parent;
        this.reference = reference;
    }

    private void requireOpen() {
        if (!open) throw new IllegalStateException("Child definition callback has finished");
        parent.requireOpen();
    }

    void close() {
        open = false;
    }

    public void connect(DeviceTerminal child, NodeReference node) {
        requireOpen();
        Objects.requireNonNull(child, "child");
        DeviceDeclaration.requireOwner(child.owner, reference.type.declaration().owner(), "child terminal");
        parent.requireNode(node);
        if (terminals.putIfAbsent(child, node) != null)
            throw new IllegalArgumentException("Duplicate child terminal " + child.id());
    }

    public void bind(DeviceParameter child, DeviceParameter parameter) {
        requireOpen();
        Objects.requireNonNull(parameter, "parameter");
        DeviceDeclaration.requireOwner(parameter.owner, parent.owner, "parent parameter");
        value(child, new DeviceDeclaration.ParameterValue(parameter));
    }

    public void literal(DeviceParameter child, double value) {
        requireOpen();
        Objects.requireNonNull(child, "child");
        child.constraints().validate(value);
        value(child, new DeviceDeclaration.LiteralValue(value));
    }

    private void value(DeviceParameter child, DeviceDeclaration.Value value) {
        requireOpen();
        Objects.requireNonNull(child, "child");
        DeviceDeclaration.requireOwner(child.owner, reference.type.declaration().owner(), "child parameter");
        if (parameters.putIfAbsent(child, value) != null)
            throw new IllegalArgumentException("Duplicate child parameter " + child.id());
    }

    DeviceDeclaration.Element finish() {
        requireOpen();
        if (terminals.size() != reference.type.terminalCount() || parameters.size() != reference.type.parameterCount())
            throw new IllegalArgumentException("Child element has missing member bindings");
        return new DeviceDeclaration.Element(reference, terminals, parameters);
    }
}
