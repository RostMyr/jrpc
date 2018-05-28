package com.github.rostmyr.jrpc.core.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

import com.github.rostmyr.jrpc.core.client.ServerResponseListener;

import java.util.List;

import static com.github.rostmyr.jrpc.core.server.handler.TransportServerDecoder.*;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ServerResponseDecoder extends ByteToMessageDecoder {
    private static final int RESPONSE_TYPE_SIZE = 1;

    private final ServerResponseListener responseListener;

    ServerResponseDecoder(ServerResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int readableBytes = in.readableBytes();
        if (readableBytes > MAX_FRAME_SIZE) {
            in.skipBytes(readableBytes);
            throw new TooLongFrameException("Frame exceeds the limit: " + readableBytes);
        }
        if (readableBytes < LENGTH_HEADER_SIZE + REQUEST_ID_SIZE + RESPONSE_TYPE_SIZE) {
            return;
        }
        int length = in.markReaderIndex().readUnsignedShort();
        if (readableBytes < length) {
            in.resetReaderIndex();
            return;
        }
        responseListener.setResponse(in.readInt(), in);
    }
}
