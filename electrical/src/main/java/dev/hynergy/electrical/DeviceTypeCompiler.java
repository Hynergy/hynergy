package dev.hynergy.electrical;

import java.util.IdentityHashMap;
import java.util.function.Function;

/** Assigns separate native node and member indexes and emits the existing protocol. */
final class DeviceTypeCompiler {
    private DeviceTypeCompiler() { }
    static DeviceDefinition compile(DeviceType type, ElectricalEngine engine,
                                    Function<DeviceType, RegisteredDeviceType> dependencies) {
        var declaration = type.declaration();
        var nodes = new IdentityHashMap<NodeReference, Integer>();
        var parameters = new IdentityHashMap<DeviceParameter, Integer>();
        var elements = new IdentityHashMap<DeviceElement, Integer>();
        try (var encoder = new DeviceDefinitionBuilder()) {
            for (var node : declaration.nodes()) {
                int index = switch (node) {
                    case DeviceTerminal ignored -> encoder.addTerminal();
                    case DeviceDeclaration.InternalNode internal ->
                        internal.ground() ? encoder.addGroundNode() : encoder.addNode();
                };
                nodes.put(node, index);
            }
            for (var parameter : type.parameters()) {
                var c = parameter.constraints();
                int index = c.requireFiniteReciprocal()
                    ? encoder.addParameterWithReciprocalRange(bound(c.lower()), bound(c.upper()), c.nonZero(), bound(c.reciprocalLower()), bound(c.reciprocalUpper()))
                    : encoder.addParameter(bound(c.lower()), bound(c.upper()), c.nonZero());
                parameters.put(parameter, index);
            }
            for (var element : declaration.elements()) {
                elements.put(element.reference(), elements.size());
                var binding = dependencies.apply(element.reference().type);
                encoder.beginElement(binding.definition());
                for (var terminal : binding.type().terminals()) encoder.elementTerminal(nodes.get(element.terminals().get(terminal)));
                for (var parameter : binding.type().parameters()) {
                    switch (element.parameters().get(parameter)) {
                        case DeviceDeclaration.ParameterValue value -> encoder.elementParameter(parameters.get(value.parameter()));
                        case DeviceDeclaration.LiteralValue value -> encoder.elementLiteral(value.value());
                    }
                }
                encoder.endElement();
            }
            for (var observation : declaration.observations()) {
                switch (observation) {
                    case DeviceDeclaration.Voltage voltage -> encoder.addVoltageObserver(nodes.get(voltage.positive()), nodes.get(voltage.negative()));
                    case DeviceDeclaration.Child child -> encoder.addChildObserver(elements.get(child.element()),
                        dependencies.apply(child.element().type).observerIndex(child.observer()));
                }
            }
            return engine.registerDefinition(encoder);
        }
    }
    private static DeviceDefinitionBuilder.@org.jspecify.annotations.Nullable Bound bound(ParameterConstraints.@org.jspecify.annotations.Nullable Bound bound) {
        return bound == null ? null : new DeviceDefinitionBuilder.Bound(bound.value(), bound.inclusive());
    }
}
