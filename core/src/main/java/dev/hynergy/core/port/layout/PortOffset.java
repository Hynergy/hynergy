package dev.hynergy.core.port.layout;

public record PortOffset(int x, int y, int z) {
    public static final PortOffset ZERO = new PortOffset(0, 0, 0);
}
