package com.github.rostmyr.jrpc.common.test;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.annotation.ResourceId;
import com.github.rostmyr.jrpc.common.io.Resource;

/**
 * Rostyslav Myroshnychenko
 * on 26.05.2018.
 */
@ResourceId(2)
public class MyResource implements Resource {
    public MyResource(int a) {

    }

    @Override
    public int getResourceId() {
        return 0;
    }

    @Override
    public void read(ByteBuf in) {

    }

    @Override
    public void write(ByteBuf out) {

    }
}
