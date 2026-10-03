package dev.hynergy.core.electricity.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MemberMappingTest {
    @Test
    void lookupSupportsGapsRetiredIdsAndOutOfRangeIds() {
        MemberMapping mapping = new MemberMapping(0, -1, 2, -1, 1);

        assertEquals(0, mapping.nativeIndex(0));
        assertEquals(-1, mapping.nativeIndex(1));
        assertEquals(2, mapping.nativeIndex(2));
        assertEquals(-1, mapping.nativeIndex(3));
        assertEquals(1, mapping.nativeIndex(4));
        assertEquals(-1, mapping.nativeIndex(-1));
        assertEquals(-1, mapping.nativeIndex(5));
    }

    @Test
    void constructorDefensivelyCopiesMapping() {
        int[] indexes = {0, -1, 1};
        MemberMapping mapping = new MemberMapping(indexes);

        indexes[0] = 7;
        indexes[1] = 6;

        assertEquals(0, mapping.nativeIndex(0));
        assertEquals(-1, mapping.nativeIndex(1));
    }

    @Test
    void nativeIndexesBelowUnmappedSentinelAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MemberMapping(0, -2, 1));
    }

    @Test
    void duplicateMappedNativeIndexesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MemberMapping(0, -1, 0));
    }
}
