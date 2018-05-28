package com.github.rostmyr.jrpc.core.server.handler;

import com.github.rostmyr.jrpc.common.io.Resource;

/**
 * Rostyslav Myroshnychenko
 * on 26.05.2018.
 */
class ResourceDescriptor {
    private final String address;
    private final int methodId;
    private final Resource resource;

    ResourceDescriptor(String address, Resource resource, int methodId) {
        this.address = address;
        this.resource = resource;
        this.methodId = methodId;
    }

    public String getAddress() {
        return address;
    }

    public Resource getResource() {
        return resource;
    }

    public int getMethodId() {
        return methodId;
    }
}
