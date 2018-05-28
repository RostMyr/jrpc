package com.github.rostmyr.jrpc.core.resource;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.io.Resource;

import java.util.function.Supplier;

/**
 * Rostyslav Myroshnychenko
 * on 22.05.2018.
 */
public class EmptyResource implements Resource {
    public static final int _resourceId = 1;

    @Override
    public int getResourceId() {
        return _resourceId;
    }

    public static Supplier<EmptyResource> create() {
        return EmptyResource::new;
    }

    @Override
    public void read(ByteBuf in) {

    }

    @Override
    public void write(ByteBuf out) {

    }
}