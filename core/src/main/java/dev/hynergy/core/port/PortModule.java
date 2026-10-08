package dev.hynergy.core.port;

import dev.hynergy.core.HynergyModule;

import java.util.Objects;

/**
 * Registers port domains, standards, and adapters, and provides access to discovery.
 *
 * <p>Registration closes when this module starts. The block-type lookup can be
 * updated before or after the module starts. World views can use this lookup to
 * select port layouts.</p>
 */
public final class PortModule extends HynergyModule {
    private final PortRegistry registry = new PortRegistry();
    private final RuntimePortDefinitions blockPorts = new RuntimePortDefinitions();

    private PortDiscovery discovery;

    @Override
    protected void setup() {
    }

    @Override
    protected void start() {
        freeze();
    }

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

    /**
     * Assigns a port layout to a block type.
     *
     * <p>This module must own each standard in the layout. The assignment
     * replaces any previous layout for the block type.</p>
     */
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

    public BlockPortDefinition blockPorts(int blockTypeId) {
        return blockPorts.get(blockTypeId);
    }

    public PortStandard<?, ?> standard(String id) {
        return registry.standard(Objects.requireNonNull(id, "id"));
    }

    public PortDiscovery discovery() {
        PortDiscovery discovery = this.discovery;
        if (discovery == null) {
            throw new IllegalStateException("Port discovery is unavailable before the port module starts");
        }
        return discovery;
    }

    void freezeForTest() {
        freeze();
    }

    private void freeze() {
        registry.freeze();
        discovery = new PortDiscovery(registry);
    }
}
