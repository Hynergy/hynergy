package dev.hynergy.electrical;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Collects a declaration during one definition callback.
 * Use this builder only during that callback.
 * Member IDs must be zero or greater and unique within each member kind.
 * Names must contain a character other than whitespace.
 * Names must be unique within each member kind.
 */
public final class DeviceTypeBuilder {
    final Object owner = new Object();
    private boolean open = true;
    private final List<DeviceParameter> parameters = new ArrayList<>();
    private final List<DeviceTerminal> terminals = new ArrayList<>();
    private final List<DeviceObserver> observers = new ArrayList<>();
    private final List<NodeReference> nodes = new ArrayList<>();
    private final List<DeviceDeclaration.Element> elements = new ArrayList<>();
    private final List<DeviceDeclaration.Observation> observations = new ArrayList<>();

    DeviceTypeBuilder() {
    }

    void requireOpen() {
        if (!open) throw new IllegalStateException("Device declaration callback has finished");
    }

    void close() {
        open = false;
    }

    DeviceDeclaration finish() {
        return new DeviceDeclaration(owner, parameters, terminals, observers, nodes, elements, observations);
    }

    private void member(int id, String name, List<Integer> ids, List<String> names) {
        requireOpen();
        Objects.requireNonNull(name, "name");
        if (id < 0 || name.isBlank() || ids.contains(id) || names.contains(name))
            throw new IllegalArgumentException("Invalid or duplicate member: " + id + " (" + name + ")");
    }

    /**
     * Declares an external terminal that also serves as a node reference.
     * @param id the stable terminal ID
     * @param name the terminal name
     * @return the terminal reference
     */
    public DeviceTerminal terminal(int id, String name) {
        member(id, name, terminals.stream().map(DeviceTerminal::id).toList(), terminals.stream()
                                                                                       .map(DeviceTerminal::name)
                                                                                       .toList());
        var terminal = new DeviceTerminal(owner, id, name);
        terminals.add(terminal);
        nodes.add(terminal);
        return terminal;
    }

    /**
     * Declares a parameter and its value constraints.
     * This method does not set a default value or validate all child constraints.
     * @param id the stable parameter ID
     * @param name the parameter name
     * @param constraints the declared value constraints
     * @return the parameter reference
     */
    public DeviceParameter parameter(int id, String name, ParameterConstraints constraints) {
        member(id, name, parameters.stream().map(DeviceParameter::id).toList(), parameters.stream()
                                                                                          .map(DeviceParameter::name)
                                                                                          .toList());
        var parameter = new DeviceParameter(owner, id, name, Objects.requireNonNull(constraints, "constraints"));
        parameters.add(parameter);
        return parameter;
    }

    /** Returns a new internal node without a public terminal or stable member ID. */
    public NodeReference node() {
        return node(false);
    }

    /**
     * Returns a new internal ideal 0 V reference.
     * Separate ground references do not connect device partitions.
     * Reuse the same reference to connect branches within this declaration.
     */
    public NodeReference ground() {
        return node(true);
    }

    private NodeReference node(boolean ground) {
        requireOpen();
        var node = new DeviceDeclaration.InternalNode(owner, ground);
        nodes.add(node);
        return node;
    }

    void requireNode(NodeReference node) {
        DeviceDeclaration.requireOwner(DeviceDeclaration.owner(Objects.requireNonNull(node, "node")), owner, "node");
    }

    /**
     * Declares one child instance and runs its binding callback immediately.
     * Bind every child terminal and parameter exactly once, in any order.
     * The child builder rejects changes after its callback finishes.
     * @param type the complete child declaration
     * @param definition the callback that binds child members
     * @return the child instance reference
     * @throws IllegalArgumentException if required bindings are missing or invalid
     */
    public DeviceElement element(DeviceType type, Consumer<DeviceElementBuilder> definition) {
        requireOpen();
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(definition, "definition");
        var reference = new DeviceElement(owner, type);
        var builder = new DeviceElementBuilder(this, reference);
        try {
            definition.accept(builder);
            elements.add(builder.finish());
            return reference;
        } finally {
            builder.close();
        }
    }

    /**
     * Declares an observation of positive voltage minus negative voltage.
     * Both node references must belong to this declaration.
     * @param id the stable observer ID
     * @param name the observer name
     * @param positive the positive node
     * @param negative the reference node
     * @return the observer reference
     */
    public DeviceObserver voltageObserver(int id, String name, NodeReference positive, NodeReference negative) {
        requireOpen();
        requireNode(positive);
        requireNode(negative);
        return observer(id, name, new DeviceDeclaration.Voltage(positive, negative));
    }

    /**
     * Declares an observer that forwards a child observation.
     * The element must belong to this declaration. The observer must belong to the element's type.
     * @param id the stable observer ID
     * @param name the observer name
     * @param element the child instance
     * @param observer the child's observer reference
     * @return the parent observer reference
     */
    public DeviceObserver childObserver(int id, String name, DeviceElement element, DeviceObserver observer) {
        requireOpen();
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(observer, "observer");
        DeviceDeclaration.requireOwner(element.owner, owner, "element");
        DeviceDeclaration.requireOwner(observer.owner, element.type.declaration().owner(), "child observer");
        return observer(id, name, new DeviceDeclaration.Child(element, observer));
    }

    private DeviceObserver observer(int id, String name, DeviceDeclaration.Observation observation) {
        member(id, name, observers.stream().map(DeviceObserver::id).toList(), observers.stream()
                                                                                       .map(DeviceObserver::name)
                                                                                       .toList());
        var observer = new DeviceObserver(owner, id, name);
        observers.add(observer);
        observations.add(observation);
        return observer;
    }
}
