package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterConstraintsTest {
    @Test
    void validatesFiniteValuesEndpointsAndNonzero() {
        var inclusive = new ParameterConstraints(new ParameterConstraints.Bound(1, true), new ParameterConstraints.Bound(2, true), false, false, null, null);
        inclusive.validate(1);
        inclusive.validate(2);
        var exclusive = new ParameterConstraints(new ParameterConstraints.Bound(1, false), new ParameterConstraints.Bound(2, false), false, false, null, null);
        exclusive.validate(1.5);
        for (double v : new double[]{1, 2, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> exclusive.validate(v));
        for (double v : new double[]{0, -0.0, -1, Double.NaN, Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> ParameterConstraints.positiveFinite().validate(v));
        ParameterConstraints.unconstrained().validate(-1);
    }

    @Test
    void validatesFiniteReciprocalAndItsBounds() {
        var finite = new ParameterConstraints(null, null, false, true, null, null);
        for (double v : new double[]{0, -0.0, Double.MIN_VALUE})
            assertThrows(IllegalArgumentException.class, () -> finite.validate(v));
        finite.validate(Double.MAX_VALUE);
        var bounded = new ParameterConstraints(null, null, false, true, new ParameterConstraints.Bound(0.4, true), new ParameterConstraints.Bound(0.6, false));
        bounded.validate(2);
        assertThrows(IllegalArgumentException.class, () -> bounded.validate(0.5));
        assertThrows(IllegalArgumentException.class, () -> bounded.validate(1 / 0.6));
    }
}
