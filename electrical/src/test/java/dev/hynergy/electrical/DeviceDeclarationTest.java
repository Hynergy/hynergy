package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceDeclarationTest {
    @Test
    void definitionRunsOnceWithoutRuntimeAndPublishesSparseSchema() {
        var calls = new AtomicInteger();
        var type = DeviceType.define(b -> {
            calls.incrementAndGet();
            var positive = b.terminal(2, "positive");
            var negative = b.terminal(5, "negative");
            b.parameter(4, "resistance", ParameterConstraints.positiveFinite());
            b.voltageObserver(6, "voltage", positive, negative);
        });
        assertEquals(1, calls.get());
        assertEquals(2, type.terminalCount());
        assertEquals(1, type.parameterCount());
        assertEquals(1, type.observerCount());
        assertEquals(java.util.List.of(2, 5), type.terminals().stream().map(DeviceTerminal::id).toList());
        assertEquals("resistance", type.parameter(4).name());
        assertEquals("voltage", type.observer(6).name());
        assertThrows(UnsupportedOperationException.class, () -> type.terminals().clear());
        assertThrows(IllegalArgumentException.class, () -> type.terminal(0));
        assertThrows(IllegalArgumentException.class, () -> type.parameter(0));
        assertThrows(IllegalArgumentException.class, () -> type.observer(0));
    }

    @Test
    void rejectsInvalidIdsNamesAndForeignReferences() {
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.terminal(-1, "x")));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.terminal(0, " ")));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            b.terminal(0, "x");
            b.terminal(0, "y");
        }));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            b.terminal(0, "x");
            b.terminal(1, "x");
        }));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            b.parameter(0, "x", ParameterConstraints.unconstrained());
            b.parameter(0, "y", ParameterConstraints.unconstrained());
        }));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            b.parameter(0, "x", ParameterConstraints.unconstrained());
            b.parameter(1, "x", ParameterConstraints.unconstrained());
        }));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            var n = b.node();
            b.voltageObserver(0, "x", n, n);
            b.voltageObserver(0, "y", n, n);
        }));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            var n = b.node();
            b.voltageObserver(0, "x", n, n);
            b.voltageObserver(1, "x", n, n);
        }));
        var other = DeviceType.define(b -> b.terminal(0, "x"));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.voltageObserver(0, "v", b.node(), other.terminal(0))));
        var separateKinds = DeviceType.define(b -> {
            var t = b.terminal(0, "x");
            b.parameter(0, "x", ParameterConstraints.unconstrained());
            b.voltageObserver(0, "x", t, t);
        });
        assertEquals(1, separateKinds.observerCount());
    }

    @Test
    void retainedBuildersCannotMutateFinishedOrFailedDeclarations() {
        DeviceTypeBuilder[] retained = new DeviceTypeBuilder[1];
        var type = DeviceType.define(b -> {
            retained[0] = b;
            b.terminal(2, "x");
        });
        assertThrows(IllegalStateException.class, () -> retained[0].terminal(3, "y"));
        assertEquals(1, type.terminalCount());
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            retained[0] = b;
            throw new IllegalArgumentException();
        }));
        assertThrows(IllegalStateException.class, () -> retained[0].node());
    }

    @Test
    void childBindingsRejectDuplicatesAndMissingMembers() {
        var child = DeviceType.define(b -> {
            var n = b.terminal(2, "x");
            b.parameter(4, "p", ParameterConstraints.unconstrained());
            b.voltageObserver(6, "v", n, n);
        });
        var other = DeviceType.define(b -> {
            var n = b.terminal(2, "x");
            b.parameter(4, "p", ParameterConstraints.unconstrained());
            b.voltageObserver(6, "v", n, n);
        });
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.element(child, e -> {
        })));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            var n = b.node();
            b.element(child, e -> {
                e.connect(child.terminal(2), n);
                e.connect(child.terminal(2), n);
            });
        }));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.element(child, e -> e.connect(other.terminal(2), b.node()))));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.element(child, e -> {
            e.literal(child.parameter(4), 1);
            e.literal(child.parameter(4), 2);
        })));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.element(child, e -> e.bind(child.parameter(4), other.parameter(4)))));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.element(child, e -> e.literal(other.parameter(4), 1))));
        DeviceElement[] foreignElement = new DeviceElement[1];
        DeviceType.define(b -> foreignElement[0] = b.element(child, e -> {
            e.connect(child.terminal(2), b.node());
            e.literal(child.parameter(4), 1);
        }));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.childObserver(0, "foreign", foreignElement[0], child.observer(6))));
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> {
            var instance = b.element(child, e -> {
                e.connect(child.terminal(2), b.node());
                e.literal(child.parameter(4), 1);
            });
            b.childObserver(0, "foreign", instance, other.observer(6));
        }));
        DeviceElementBuilder[] retained = new DeviceElementBuilder[1];
        var type = DeviceType.define(b -> {
            var n = b.node();
            b.element(child, e -> {
                retained[0] = e;
                e.literal(child.parameter(4), 1);
                e.connect(child.terminal(2), n);
            });
        });
        assertThrows(IllegalStateException.class, () -> retained[0].literal(child.parameter(4), 2));
        assertEquals(0, type.parameterCount());
        assertThrows(IllegalArgumentException.class, () -> DeviceType.define(b -> b.element(child, e -> {
            retained[0] = e;
            throw new IllegalArgumentException();
        })));
        assertThrows(IllegalStateException.class, () -> retained[0].literal(child.parameter(4), 2));
    }
}
