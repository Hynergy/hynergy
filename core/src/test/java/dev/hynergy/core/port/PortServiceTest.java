package dev.hynergy.core.port;

import dev.hynergy.core.port.layout.BlockPortDefinition;
import dev.hynergy.core.port.layout.PortDefinition;
import dev.hynergy.core.port.layout.PortOffset;
import dev.hynergy.core.port.layout.PortReach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PortServiceTest {
    private record Profile(int value) {
    }

    private enum Result {CONNECTED}

    @Test
    void protocolRegistrationIsOpenBeforeFreezeAndFrozenAfterward() {
        PortService module = new PortService();
        PortDomain<Result> domain = module.registerDomain("test:domain");
        PortStandard<Profile, Result> standard = module.registerStandard(
                "test:standard", domain, Profile.class,
                (first, second, geometry) -> Result.CONNECTED
        );
        assertEquals(standard, module.standard("test:standard"));

        PortServiceTestAccess.freeze(module);
        assertThrows(IllegalStateException.class, () -> module.registerDomain("test:late"));
    }

    @Test
    void runtimeBlockDefinitionsRemainMutableAfterProtocolFreeze() {
        PortService module = new PortService();
        PortDomain<Result> domain = module.registerDomain("test:domain");
        PortStandard<Profile, Result> standard = module.registerStandard(
                "test:standard", domain, Profile.class,
                (first, second, geometry) -> Result.CONNECTED
        );
        PortServiceTestAccess.freeze(module);

        BlockPortDefinition definition = BlockPortDefinition.of(
                new PortDefinition<>(0, PortOffset.ZERO, PortReach.single(1, 0, 0), standard, new Profile(1))
        );
        module.setBlockPorts(42, definition);
        assertEquals(definition, module.blockPorts(42));
        module.clearBlockPorts(42);
        assertNull(module.blockPorts(42));
    }

    @Test
    void differentServicesOwnDifferentStandards() {
        PortService owner = new PortService();
        PortService foreign = new PortService();
        PortDomain<Result> domain = owner.registerDomain("test:domain");
        PortDomain<Result> foreignDomain = foreign.registerDomain("test:domain");
        PortStandard<Profile, Result> standard = owner.registerStandard(
                "test:standard", domain, Profile.class,
                (first, second, geometry) -> Result.CONNECTED
        );
        PortStandard<Profile, Result> foreignStandard = foreign.registerStandard(
                "test:standard", foreignDomain, Profile.class,
                (first, second, geometry) -> Result.CONNECTED
        );
        BlockPortDefinition foreignLayout = BlockPortDefinition.of(
                new PortDefinition<>(0, PortOffset.ZERO, PortReach.single(1, 0, 0),
                        foreignStandard, new Profile(1))
        );
        assertThrows(IllegalArgumentException.class, () -> owner.setBlockPorts(1, foreignLayout));
        assertNull(owner.blockPorts(1));
        assertSame(standard, owner.standard("test:standard"));
        assertNotSame(standard, foreignStandard);
    }

    @Test
    void freezeClosesStandardAndAdapterRegistration() {
        PortService service = new PortService();
        PortDomain<Result> domain = service.registerDomain("test:domain");
        PortStandard<Profile, Result> first = service.registerStandard(
                "test:first", domain, Profile.class, (a, b, geometry) -> Result.CONNECTED);
        PortStandard<Profile, Result> second = service.registerStandard(
                "test:second", domain, Profile.class, (a, b, geometry) -> Result.CONNECTED);
        service.freeze();
        assertThrows(IllegalStateException.class,
                () -> service.registerStandard("test:late", domain, Profile.class,
                        (a, b, geometry) -> Result.CONNECTED));
        assertThrows(IllegalStateException.class,
                () -> service.registerAdapter(first, second,
                        (a, b, geometry) -> Result.CONNECTED));
    }

    @Test
    void secondFreezeIsRejectedButDiscoveryRemainsAvailable() {
        PortService service = new PortService();
        service.freeze();
        assertThrows(IllegalStateException.class, service::freeze);
        assertNotNull(service.discovery());
        assertThrows(IllegalStateException.class,
                () -> service.registerDomain("test:late"));
    }

    @Test
    void discoveryBeforeFreezeIsRejected() {
        PortService module = new PortService();
        assertThrows(IllegalStateException.class, module::discovery);
    }

    @Test
    void freezeMakesDiscoveryAvailable() {
        PortService module = new PortService();
        PortServiceTestAccess.freeze(module);
        assertNotNull(module.discovery());
    }
}
