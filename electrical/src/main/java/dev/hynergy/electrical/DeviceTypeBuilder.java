package dev.hynergy.electrical;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Collects a declaration during one definition callback.
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

    public DeviceTerminal terminal(int id, String name) {
        member(id, name, terminals.stream().map(DeviceTerminal::id).toList(), terminals.stream()
                                                                                       .map(DeviceTerminal::name)
                                                                                       .toList());
        var terminal = new DeviceTerminal(owner, id, name);
        terminals.add(terminal);
        nodes.add(terminal);
        return terminal;
    }

    public DeviceParameter parameter(int id, String name, ParameterConstraints constraints) {
        member(id, name, parameters.stream().map(DeviceParameter::id).toList(), parameters.stream()
                                                                                          .map(DeviceParameter::name)
                                                                                          .toList());
        var parameter = new DeviceParameter(owner, id, name, Objects.requireNonNull(constraints, "constraints"));
        parameters.add(parameter);
        return parameter;
    }

    public NodeReference node() {
        return node(false);
    }

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

    public DeviceObserver voltageObserver(int id, String name, NodeReference positive, NodeReference negative) {
        requireOpen();
        requireNode(positive);
        requireNode(negative);
        return observer(id, name, new DeviceDeclaration.Voltage(positive, negative));
    }

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
