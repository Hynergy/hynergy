package dev.hynergy.core.port;

import dev.hynergy.core.port.connection.PortResolver;
import dev.hynergy.core.port.layout.BlockPortDefinition;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class PortService {
    private final PortRegistry registry = new PortRegistry();
    private final RuntimePortDefinitions blockPorts = new RuntimePortDefinitions();

    private @Nullable PortDiscovery discovery;

    public <R> PortDomain<R> registerDomain(String id) {
        return registry.registerDomain(id);
    }

    public <P, R> PortStandard<P, R> registerStandard(
            String id,
            PortDomain<R> domain,
            Class<P> profileType,
            PortResolver<P, P, R> resolver
    ) {
        return registry.registerStandard(id, domain, profileType, resolver);
    }

    public <A, B, R> void registerAdapter(
            PortStandard<A, R> first,
            PortStandard<B, R> second,
            PortResolver<A, B, R> resolver
    ) {
        registry.registerAdapter(first, second, resolver);
    }

    public PortStandard<?, ?> standard(String id) {
        return registry.standard(Objects.requireNonNull(id, "id"));
    }

    public void setBlockPorts(int blockTypeId, BlockPortDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        for (int index = 0; index < definition.size(); index++) {
            PortStandard<?, ?> standard = definition.portAt(index).standard();
            if (!registry.owns(standard)) {
                throw new IllegalArgumentException(
                        "Block port definition references a standard from another PortModule: " + standard.id()
                );
            }
        }
        blockPorts.set(blockTypeId, definition);
    }

    public void clearBlockPorts(int blockTypeId) {
        blockPorts.clear(blockTypeId);
    }

    public @Nullable BlockPortDefinition blockPorts(int blockTypeId) {
        return blockPorts.get(blockTypeId);
    }

    public PortDiscovery discovery() {
        PortDiscovery discovery = this.discovery;
        if (discovery == null) {
            throw new IllegalStateException("Port discovery is unavailable before the port module starts");
        }
        return discovery;
    }

    private void freezeForTest() {
        freeze();
    }

    void freeze() {
        registry.freeze();
        discovery = new PortDiscovery(registry);
    }
}
