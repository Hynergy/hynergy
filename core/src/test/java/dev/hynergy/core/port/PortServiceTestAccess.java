package dev.hynergy.core.port;

public final class PortServiceTestAccess {
    private PortServiceTestAccess() {
    }

    public static void freeze(PortService module) {
        module.freeze();
    }
}
