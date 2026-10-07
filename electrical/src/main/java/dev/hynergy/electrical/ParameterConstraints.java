package dev.hynergy.electrical;

import org.jspecify.annotations.Nullable;

/**
 * Defines constraints on finite values and their reciprocals.
 * Native validation can apply additional rules.
 * @param lower the lower value bound, or null for no lower bound
 * @param upper the upper value bound, or null for no upper bound
 * @param nonZero whether validation rejects zero
 * @param requireFiniteReciprocal whether validation requires a finite reciprocal
 * @param reciprocalLower the lower reciprocal bound, or null for no lower bound
 * @param reciprocalUpper the upper reciprocal bound, or null for no upper bound
 */
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
    /** Returns constraints that accept any finite value, including zero. */
    public static ParameterConstraints unconstrained() {
        return new ParameterConstraints(null, null, false, false, null, null);
    }
    /** Returns constraints that require a finite value greater than zero. */
    public static ParameterConstraints positiveFinite() {
        return new ParameterConstraints(Bound.exclusive(0), null, false, false, null, null);
    }
    /**
     * Checks finiteness, value bounds, and any declared reciprocal constraints.
     * This method does not require a runtime or check native parameter relationships.
     * @param value the proposed value
     * @throws IllegalArgumentException if the value violates a declared constraint
     */
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
