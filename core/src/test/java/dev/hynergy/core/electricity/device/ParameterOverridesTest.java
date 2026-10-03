package dev.hynergy.core.electricity.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ParameterOverridesTest {
    @Test
    void insertUpdateRemoveAndCopyUseCompactStableIds() {
        ParameterOverrides overrides = new ParameterOverrides();

        overrides.set(7, 100.0);
        overrides.set(2, 220.0);
        overrides.set(7, 470.0);

        assertEquals(2, overrides.size());
        assertEquals(7, overrides.stableIdAt(0));
        assertEquals(470.0, overrides.valueAt(0));
        assertEquals(2, overrides.stableIdAt(1));
        assertEquals(220.0, overrides.valueAt(1));
        assertTrue(overrides.contains(7));
        assertEquals(220.0, overrides.getOrDefault(2, -1.0));
        assertEquals(-1.0, overrides.getOrDefault(99, -1.0));

        ParameterOverrides copy = overrides.copy();
        assertTrue(overrides.remove(7));
        assertFalse(overrides.remove(7));
        overrides.set(2, 330.0);

        assertEquals(1, overrides.size());
        assertEquals(2, copy.size());
        assertEquals(470.0, copy.getOrDefault(7, -1.0));
        assertEquals(220.0, copy.getOrDefault(2, -1.0));
    }

    @Test
    void invalidOverridesAndDuplicateSerializedIdsAreRejected() {
        ParameterOverrides overrides = new ParameterOverrides();

        assertThrows(IllegalArgumentException.class, () -> overrides.set(-1, 1.0));
        assertThrows(IllegalArgumentException.class, () -> overrides.set(0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> overrides.set(0, Double.POSITIVE_INFINITY));
        assertThrows(
                IllegalArgumentException.class,
                () -> ParameterOverrides.fromSerialized(new DeviceParameterOverride[]{
                        new DeviceParameterOverride(3, 1.0),
                        new DeviceParameterOverride(3, 2.0)
                })
        );
    }
}
