package dev.hynergy.electrical;

import org.jspecify.annotations.Nullable;

/** Declared value and reciprocal constraints. Native validation can impose additional rules. */
public record ParameterConstraints(@Nullable Bound lower, @Nullable Bound upper, boolean nonZero,
                                   boolean requireFiniteReciprocal, @Nullable Bound reciprocalLower,
                                   @Nullable Bound reciprocalUpper) {
    public record Bound(double value, boolean inclusive) {
        public static Bound inclusive(double value) { return new Bound(value, true); }
        public static Bound exclusive(double value) { return new Bound(value, false); }
    }
    public ParameterConstraints {
        if (!requireFiniteReciprocal && (reciprocalLower != null || reciprocalUpper != null))
            throw new IllegalArgumentException("Reciprocal bounds require finite reciprocal validation");
    }
    public static ParameterConstraints unconstrained() {
        return new ParameterConstraints(null, null, false, false, null, null);
    }
    public static ParameterConstraints positiveFinite() {
        return new ParameterConstraints(Bound.exclusive(0), null, false, false, null, null);
    }
    public void validate(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Parameter must be finite");
        if (nonZero && value == 0) throw new IllegalArgumentException("Parameter must be nonzero");
        if (!within(value, lower, upper)) throw new IllegalArgumentException("Parameter is outside its declared range");
        if (requireFiniteReciprocal) {
            double reciprocal = 1 / value;
            if (!Double.isFinite(reciprocal) || !within(reciprocal, reciprocalLower, reciprocalUpper))
                throw new IllegalArgumentException("Parameter reciprocal is outside its declared range");
        }
    }
    private static boolean within(double value, @Nullable Bound lower, @Nullable Bound upper) {
        return (lower == null || (lower.inclusive() ? value >= lower.value() : value > lower.value()))
            && (upper == null || (upper.inclusive() ? value <= upper.value() : value < upper.value()));
    }
}
