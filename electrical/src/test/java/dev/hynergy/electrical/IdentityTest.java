package dev.hynergy.electrical;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class IdentityTest {
    @Test
    void deviceIdentityValidatesComponentsAndPreservesSignedGeneration() {
        assertThrows(IllegalArgumentException.class, () -> new DeviceId(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new DeviceId(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new DeviceId(1, 0));
        assertThrows(IllegalArgumentException.class, () -> DeviceId.fromPacked(0L));

        DeviceId id = new DeviceId(7, -1);
        assertEquals(id, DeviceId.fromPacked(id.packed()));
    }

    @Test
    void wireIdentityValidatesComponentsAndPreservesSignedGeneration() {
        assertThrows(IllegalArgumentException.class, () -> new WireId(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new WireId(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new WireId(1, 0));
        assertThrows(IllegalArgumentException.class, () -> WireId.fromPacked(0L));

        WireId id = new WireId(7, -1);
        assertEquals(id, WireId.fromPacked(id.packed()));
    }
}
