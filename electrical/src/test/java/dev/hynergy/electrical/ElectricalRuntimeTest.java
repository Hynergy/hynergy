package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class ElectricalRuntimeTest {
    private static final int RESISTANCE_DEFINITION_ID = 1;

    @Test
    void registrationResolvesDependenciesAndCachesDefinitions() {
        DeviceType resistance = resistanceType();
        DeviceType child = resistorComposite(resistance);
        DeviceType parent = forwardingComposite(child);

        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            DeviceDefinition parentDefinition = runtime.register(parent);
            DeviceDefinition childDefinition = runtime.requireDefinition(child);

            assertNotEquals(0, parentDefinition.id());
            assertNotEquals(0, childDefinition.id());
            assertNotEquals(parentDefinition.id(), childDefinition.id());
            assertSame(parentDefinition, runtime.register(parent));
        }
    }

    @Test
    void registrationRejectsRecursiveDependenciesAndRemainsUsable() {
        AtomicReference<DeviceType> firstReference = new AtomicReference<>();
        AtomicReference<DeviceType> secondReference = new AtomicReference<>();

        DeviceType first =
            DeviceType.create(builder -> builder.beginElement(secondReference.get()));
        DeviceType second =
            DeviceType.create(builder -> builder.beginElement(firstReference.get()));

        firstReference.set(first);
        secondReference.set(second);

        DeviceType valid = resistorComposite(resistanceType());

        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            assertThrows(IllegalStateException.class, () -> runtime.register(first));

            assertDoesNotThrow(() -> runtime.register(valid));
        }
    }

    @Test
    void failedRootRegistrationKeepsSuccessfulDependencies() {
        DeviceType child = resistorComposite(resistanceType());
        DeviceType invalidParent =
            DeviceType.create(builder -> builder.beginElement(child).endElement());

        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            assertThrows(IllegalArgumentException.class, () -> runtime.register(invalidParent));

            DeviceDefinition childDefinition = runtime.requireDefinition(child);

            assertSame(childDefinition, runtime.register(child));
        }
    }

    @Test
    void unregisteredTypesCannotBeAddedWhileSystemsAreActive() {
        DeviceType type = resistorComposite(resistanceType());

        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            ElectricalSystem system = runtime.createSystem(20);

            try (system) {
                assertThrows(IllegalStateException.class, () -> runtime.register(type));
            }

            assertDoesNotThrow(() -> runtime.register(type));
        }
    }

    @Test
    void registeredDefinitionIsClearedWhenRuntimeCloses() {
        DeviceType type = resistorComposite(resistanceType());

        DeviceDefinition firstDefinition;

        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            firstDefinition = runtime.register(type);
        }

        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            DeviceDefinition secondDefinition = runtime.register(type);

            assertNotSame(firstDefinition, secondDefinition);
        }
    }


    @Test
    void staleRuntimeCleanupDoesNotClearNewRuntimeBinding() {
        DeviceType type = resistorComposite(resistanceType());

        ElectricalRuntime firstRuntime = ElectricalRuntime.create();

        firstRuntime.register(type);
        firstRuntime.close();

        try (ElectricalRuntime secondRuntime = ElectricalRuntime.create()) {
            DeviceDefinition definition = secondRuntime.register(type);

            type.unbind(firstRuntime);

            assertSame(definition, secondRuntime.requireDefinition(type));
        }
    }

    @Test
    void primitiveTypesHaveFixedDefinitions() {
        DeviceType resistance = resistanceType();

        try (ElectricalRuntime runtime = ElectricalRuntime.create()) {
            DeviceDefinition definition = runtime.register(resistance);

            assertEquals(RESISTANCE_DEFINITION_ID, definition.id());
            assertSame(definition, runtime.register(resistance));
        }
    }

    private static DeviceType resistanceType() {
        return DeviceType.primitive(RESISTANCE_DEFINITION_ID);
    }

    private static DeviceType resistorComposite(
        DeviceType resistance
    ) {
        return DeviceType.create(builder -> {
        int firstTerminal = builder.addTerminal();
        int secondTerminal = builder.addTerminal();

        builder.beginElement(resistance)
            .elementTerminal(firstTerminal)
            .elementTerminal(secondTerminal)
            .elementLiteral(1_000.0)
            .endElement();
        });
    }

    private static DeviceType forwardingComposite(
        DeviceType child
    ) {
        return DeviceType.create(builder -> {
        int firstTerminal = builder.addTerminal();
        int secondTerminal = builder.addTerminal();

        builder.beginElement(child).elementTerminal(firstTerminal).elementTerminal(secondTerminal).endElement();
        });
    }
}
