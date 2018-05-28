package com.github.rostmyr.jrpc.common.io;

import io.netty.buffer.ByteBuf;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
public abstract class BaseResource implements Resource {
    @Override
    public int getResourceId() {
        throw new UnsupportedOperationException("Should be overriden");
    }

    @Override
    public void read(ByteBuf in) {
        // no op
    }

    @Override
    public void write(ByteBuf out) {
        // no op
    }
}
