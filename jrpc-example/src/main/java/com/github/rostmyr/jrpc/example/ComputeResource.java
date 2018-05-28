package com.github.rostmyr.jrpc.example;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.annotation.ResourceId;
import com.github.rostmyr.jrpc.common.io.BaseResource;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
@ResourceId(1)
public class ComputeResource extends BaseResource {
    private int factor;

    public ComputeResource(int factor) {
        this.factor = factor;
    }

    public int getFactor() {
        return factor;
    }

    @Override
    public void read(ByteBuf in) {
        this.factor = in.readInt();
    }

    @Override
    public void write(ByteBuf out) {
        out.writeInt(factor);
    }
}
