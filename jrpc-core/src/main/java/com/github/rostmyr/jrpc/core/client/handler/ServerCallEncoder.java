package com.github.rostmyr.jrpc.core.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.core.client.ServerCall;
import com.github.rostmyr.jrpc.core.server.handler.TransportServerDecoder;

import java.nio.charset.StandardCharsets;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ServerCallEncoder extends MessageToByteEncoder<ServerCall> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerCall serverCall, ByteBuf out) {
        Resource resource = serverCall.getResource();

        writeHeader(serverCall, out, resource);
        writeBody(out, resource);
        writeLength(out);
    }

    private void writeLength(ByteBuf out) {
        out.setShort(0, out.writerIndex());
    }

    private void writeHeader(ServerCall serverCall, ByteBuf out, Resource resource) {
        out.writerIndex(TransportServerDecoder.LENGTH_HEADER_SIZE);
        out.writeInt(serverCall.getId());
        out.writeByte(serverCall.getMethodId());
        out.writeShort(resource.getResourceId());
        out.writeByte(serverCall.getAddress().length());
        out.writeCharSequence(serverCall.getAddress(), StandardCharsets.UTF_8);
    }

    private void writeBody(ByteBuf out, Resource resource) {
        resource.write(out);
    }
}
