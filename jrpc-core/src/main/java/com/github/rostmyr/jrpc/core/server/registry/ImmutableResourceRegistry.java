package com.github.rostmyr.jrpc.core.server.registry;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.common.utils.Contract;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ImmutableResourceRegistry implements ResourceRegistry {
    private final ResourceRegistry registry;

    public ImmutableResourceRegistry(ResourceRegistry registry) {
        this.registry = Contract.checkNotNull(registry, "Registry can't be null");
    }

    @Override
    public Resource get(int id) {
        return registry.get(id);
    }

    @Override
    public void add(int id, Class<? extends Resource> clazz) {
        throw new UnsupportedOperationException("You can't modify registry");
    }
}
