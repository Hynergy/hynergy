package dev.hynergy.electrical;

/**
 * Persistently identifies one device in an electrical world.
 *
 * <p>A device ID remains associated with the same logical device when the
 * electrical world is serialized and restored. The generation distinguishes
 * different devices that reuse the same numeric ID.</p>
 *
 * <p>Device IDs are scoped to an electrical world. An ID from one world must
 * not be used to identify a device in another world.</p>
 *
 * <p>A device ID does not provide access to the device by itself. A live
 * {@link Device} handle is required to interact with the device at runtime.</p>
 *
 * @param value      the positive device ID
 * @param generation the non-zero generation of the device ID
 */
public record DeviceId(int value, int generation) {

    public DeviceId {
        if (value <= 0) {
            throw new IllegalArgumentException("Device ID must be positive");
        }
        if (generation == 0) {
            throw new IllegalArgumentException("Device generation must not be zero");
        }
    }

    public long packed() {
        return ((long) value << 32)
                | Integer.toUnsignedLong(generation);
    }

    public static DeviceId fromPacked(long packed) {
        return new DeviceId(
                (int) (packed >>> 32),
                (int) packed
        );
    }
}
