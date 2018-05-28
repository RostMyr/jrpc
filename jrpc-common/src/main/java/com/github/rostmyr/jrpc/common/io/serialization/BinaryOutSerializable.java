package com.github.rostmyr.jrpc.common.io.serialization;

import io.netty.buffer.ByteBuf;

/**
 * Rostyslav Myroshnychenko
 * on 21.05.2018.
 */
public interface BinaryOutSerializable {
    void write(ByteBuf out);
}
