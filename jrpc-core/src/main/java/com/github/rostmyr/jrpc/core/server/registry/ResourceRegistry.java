package com.github.rostmyr.jrpc.core.server.registry;

import com.github.rostmyr.jrpc.common.io.Resource;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public interface ResourceRegistry {
    Resource get(int id);
    void add(int id, Class<? extends Resource> clazz);
}
