package com.github.rostmyr.jrpc.core.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.github.rostmyr.jrpc.core.client.ServerCall;
import com.github.rostmyr.jrpc.core.server.handler.TransportServerDecoder;
import com.github.rostmyr.jrpc.core.service.ResponseType;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ServerCallEncoder extends MessageToByteEncoder<ServerCall> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerCall serverCall, ByteBuf out) {
        writeHeader(serverCall, out);
        writeBody(serverCall, out);
        writeLength(out);
    }

    private void writeLength(ByteBuf out) {
        out.setShort(0, out.writerIndex());
    }

    private void writeHeader(ServerCall serverCall, ByteBuf out) {
        out.writerIndex(TransportServerDecoder.LENGTH_HEADER_SIZE);
        out.writeInt(serverCall.getId());
        out.writeByte(serverCall.getMethodId());
        out.writeByte(serverCall.getAddress().length());
        out.writeCharSequence(serverCall.getAddress(), StandardCharsets.UTF_8);
    }

    private void writeBody(ServerCall serverCall, ByteBuf out) {
        List<ResponseType> inputTypes = serverCall.getInputTypes();
        Object[] args = serverCall.getArgs();
        for (int i = 0; i < args.length; i++) {
            inputTypes.get(i).write(out, args[i]);
        }
    }
}
