package dev.hynergy.core.electricity.device;

import java.util.Objects;

/**
 * Immutable mapping from stable device member IDs to native definition indexes.
 *
 * <p>A value of {@value #UNMAPPED} marks a stable ID as unmapped or retired.
 * Stable IDs outside the represented range are also treated as unmapped.</p>
 */
public final class MemberMapping {
    public static final int UNMAPPED = -1;

    private final int[] nativeIndexes;

    /**
     * Creates a mapping whose array position is the stable member ID and whose
     * value is the corresponding native definition index.
     *
     * @param nativeIndexes native indexes by stable member ID
     * @throws NullPointerException     if {@code nativeIndexes} is null
     * @throws IllegalArgumentException if an entry is below {@link #UNMAPPED}
     *                                  or two stable IDs map to the same native index
     */
    public MemberMapping(int... nativeIndexes) {
        Objects.requireNonNull(nativeIndexes, "nativeIndexes");
        this.nativeIndexes = nativeIndexes.clone();
        validate(this.nativeIndexes);
    }

    /**
     * Returns the native definition index for a stable member ID.
     *
     * @param stableId the stable member ID
     * @return the native index, or {@link #UNMAPPED} when the stable ID is
     * retired, negative, or outside this mapping
     */
    public int nativeIndex(int stableId) {
        if (stableId < 0 || stableId >= nativeIndexes.length) {
            return UNMAPPED;
        }

        return nativeIndexes[stableId];
    }

    void validateAgainst(String descriptorId, String member, int count) {
        for (int stableId = 0; stableId < nativeIndexes.length; stableId++) {
            int nativeIndex = nativeIndexes[stableId];
            if (nativeIndex >= count) {
                throw new IllegalArgumentException("Device descriptor " + descriptorId + " maps "
                        + member + " stable ID " + stableId + " to native index " + nativeIndex
                        + ", outside [0, " + count + ")");
            }
        }
    }

    private static void validate(int[] nativeIndexes) {
        for (int stableId = 0; stableId < nativeIndexes.length; stableId++) {
            int nativeIndex = nativeIndexes[stableId];

            if (nativeIndex < UNMAPPED) {
                throw new IllegalArgumentException(
                        "Native member index must be non-negative or -1: stable ID " + stableId
                );
            }

            if (nativeIndex == UNMAPPED) {
                continue;
            }

            for (int previousStableId = 0; previousStableId < stableId; previousStableId++) {
                if (nativeIndexes[previousStableId] == nativeIndex) {
                    throw new IllegalArgumentException(
                            "Native member index " + nativeIndex + " is mapped by multiple stable IDs"
                    );
                }
            }
        }
    }
}
