package dev.hynergy.electrical;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/** A declaration binding owned by one runtime. */
public final class RegisteredDeviceType {
    private final ElectricalRuntime runtime;
    private final DeviceType type;
    private final DeviceDefinition definition;
    private final IdentityHashMap<DeviceParameter, Integer> parameters = new IdentityHashMap<>();
    private final IdentityHashMap<DeviceTerminal, Integer> terminals = new IdentityHashMap<>();
    private final IdentityHashMap<DeviceObserver, Integer> observers = new IdentityHashMap<>();
    RegisteredDeviceType(ElectricalRuntime runtime, DeviceType type, DeviceDefinition definition) {
        this.runtime = runtime; this.type = type; this.definition = definition;
        index(type.parameters(), parameters); index(type.terminals(), terminals); index(type.observers(), observers);
    }
    private static <T> void index(List<T> members, IdentityHashMap<T, Integer> indexes) {
        for (int i = 0; i < members.size(); i++) indexes.put(members.get(i), i);
    }
    public DeviceType type() { return type; }
    DeviceDefinition definition() { return definition; }
    int parameterIndex(DeviceParameter member) { return require(parameters, member, "parameter"); }
    int terminalIndex(DeviceTerminal member) { return require(terminals, member, "terminal"); }
    int observerIndex(DeviceObserver member) { return require(observers, member, "observer"); }
    private static <T> int require(IdentityHashMap<T, Integer> indexes, T member, String kind) {
        var index = indexes.get(Objects.requireNonNull(member, kind));
        if (index == null) throw new IllegalArgumentException("Foreign " + kind + " reference");
        return index;
    }
    public void validateParameter(DeviceParameter parameter, double value) {
        int index = parameterIndex(parameter);
        parameter.constraints().validate(value);
        runtime.validateParameter(definition, index, value);
    }
}
