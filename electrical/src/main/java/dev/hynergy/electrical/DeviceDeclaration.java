package dev.hynergy.electrical;

import java.util.List;
import java.util.Map;

record DeviceDeclaration(Object owner, List<DeviceParameter> parameters, List<DeviceTerminal> terminals,
                         List<DeviceObserver> observers, List<NodeReference> nodes,
                         List<Element> elements, List<Observation> observations) {
    DeviceDeclaration {
        parameters = List.copyOf(parameters); terminals = List.copyOf(terminals);
        observers = List.copyOf(observers); nodes = List.copyOf(nodes);
        elements = List.copyOf(elements); observations = List.copyOf(observations);
    }
    record InternalNode(Object owner, boolean ground) implements NodeReference { }
    sealed interface Value permits ParameterValue, LiteralValue { }
    record ParameterValue(DeviceParameter parameter) implements Value { }
    record LiteralValue(double value) implements Value { }
    record Element(DeviceElement reference, Map<DeviceTerminal, NodeReference> terminals,
                   Map<DeviceParameter, Value> parameters) {
        Element { terminals = Map.copyOf(terminals); parameters = Map.copyOf(parameters); }
    }
    sealed interface Observation permits Voltage, Child { }
    record Voltage(NodeReference positive, NodeReference negative) implements Observation { }
    record Child(DeviceElement element, DeviceObserver observer) implements Observation { }
    static Object owner(NodeReference node) {
        return switch (node) {
            case DeviceTerminal terminal -> terminal.owner;
            case InternalNode internal -> internal.owner();
        };
    }
    static void requireOwner(Object actual, Object expected, String kind) {
        if (actual != expected) throw new IllegalArgumentException("Foreign " + kind + " reference");
    }
}
