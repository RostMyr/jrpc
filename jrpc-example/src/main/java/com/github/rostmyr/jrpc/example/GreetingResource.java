package com.github.rostmyr.jrpc.example;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.annotation.ResourceId;
import com.github.rostmyr.jrpc.common.io.BaseResource;

import java.nio.charset.StandardCharsets;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
@ResourceId(2)
public class GreetingResource extends BaseResource {
    private String name;

    public GreetingResource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void read(ByteBuf in) {
        int length = in.readUnsignedShort();
        this.name = (String) in.readCharSequence(length, StandardCharsets.UTF_8);
    }

    @Override
    public void write(ByteBuf out) {
        out.writeShort(name.length());
        out.writeCharSequence(name, StandardCharsets.UTF_8);
    }
}
