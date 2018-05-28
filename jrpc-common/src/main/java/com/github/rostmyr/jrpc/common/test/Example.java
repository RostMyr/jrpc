package com.github.rostmyr.jrpc.common.test;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.io.Resource;

import java.util.function.Supplier;

/**
 * Rostyslav Myroshnychenko
 * on 26.05.2018.
 */
public class Example implements Resource {
    public Example() {
    }

    public static Supplier<Example> supplier() {
        return Example::new;
    }

    public static final int _resourceId = 50;

    public int getResourceId() {
        return _resourceId;
    }

    @Override
    public void read(ByteBuf in) {

    }

    @Override
    public void write(ByteBuf out) {

    }
}
