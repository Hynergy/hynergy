package dev.hynergy.electrical;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Binds each child member once. The order of builder calls does not determine native indexes.
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

    /**
     * Connects one child terminal to a parent node.
     * @param child a terminal from this child type
     * @param node a terminal or internal node from the parent declaration
     * @throws IllegalArgumentException if a reference has the wrong owner or the terminal already has a binding
     */
    public void connect(DeviceTerminal child, NodeReference node) {
        requireOpen();
        Objects.requireNonNull(child, "child");
        DeviceDeclaration.requireOwner(child.owner, reference.type.declaration().owner(), "child terminal");
        parent.requireNode(node);
        if (terminals.putIfAbsent(child, node) != null)
            throw new IllegalArgumentException("Duplicate child terminal " + child.id());
    }

    /**
     * Binds one child parameter to a parent parameter.
     * Native registration checks effective child constraints.
     * @param child a parameter from this child type
     * @param parameter a parameter from the parent declaration
     * @throws IllegalArgumentException if a reference has the wrong owner or the child parameter already has a binding
     */
    public void bind(DeviceParameter child, DeviceParameter parameter) {
        requireOpen();
        Objects.requireNonNull(parameter, "parameter");
        DeviceDeclaration.requireOwner(parameter.owner, parent.owner, "parent parameter");
        value(child, new DeviceDeclaration.ParameterValue(parameter));
    }

    /**
     * Binds one child parameter to a constant value.
     * This method checks declared constraints. Native registration checks additional constraints and parameter relationships.
     * @param child a parameter from this child type
     * @param value the constant parameter value
     * @throws IllegalArgumentException if the value or reference is invalid, or the child parameter already has a binding
     */
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
