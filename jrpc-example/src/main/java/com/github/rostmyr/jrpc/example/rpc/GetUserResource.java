package com.github.rostmyr.jrpc.example.rpc;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.annotation.ResourceId;
import com.github.rostmyr.jrpc.common.io.BaseResource;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
@ResourceId(4)
public class GetUserResource extends BaseResource {
    private int id;

    public GetUserResource(int id) {
        this.id = id;
    }

    @Override
    public void read(ByteBuf in) {
        this.id = in.readInt();
    }

    @Override
    public void write(ByteBuf out) {
        out.writeInt(id);
    }
}
