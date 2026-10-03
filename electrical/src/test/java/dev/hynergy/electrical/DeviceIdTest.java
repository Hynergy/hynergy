package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DeviceIdTest {

    @Test
    void identityRequiresPositiveValueAndNonZeroGeneration() {
        assertThrows(IllegalArgumentException.class, () -> new DeviceId(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new DeviceId(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new DeviceId(1, 0));
    }

    @Test
    void packedRoundTripPreservesSignedGeneration() {
        DeviceId deviceId = new DeviceId(7, -1);

        assertEquals(deviceId, DeviceId.fromPacked(deviceId.packed()));
    }

    @Test
    void invalidPackedIdentityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DeviceId.fromPacked(0L));
    }
}
