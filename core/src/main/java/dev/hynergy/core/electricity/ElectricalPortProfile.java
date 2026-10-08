package dev.hynergy.core.electricity;

/**
 * Provides compatibility data for one direct electrical conductor port.
 *
 * <p>The normal uses the block's local coordinates.
 * Port discovery transforms the normal with the owning block's rotation.</p>
 *
 * @param normalX the X component of the outward cardinal normal
 * @param normalY the Y component of the outward cardinal normal
 * @param normalZ the Z component of the outward cardinal normal
 */
public record ElectricalPortProfile(int normalX, int normalY, int normalZ) {
    public ElectricalPortProfile {
        if (normalX < -1 || normalX > 1
                || normalY < -1 || normalY > 1
                || normalZ < -1 || normalZ > 1
                || Math.abs(normalX) + Math.abs(normalY) + Math.abs(normalZ) != 1) {
            throw new IllegalArgumentException("Electrical port normal must be one unit cardinal vector");
        }
    }
}
