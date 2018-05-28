package com.github.rostmyr.jrpc.core.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.github.rostmyr.jrpc.core.service.ResponseType;

import static com.github.rostmyr.jrpc.core.server.handler.TransportServerDecoder.LENGTH_HEADER_SIZE;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class TransportServerEncoder extends MessageToByteEncoder<TransportServerEncoder.ResponseMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseMessage msg, ByteBuf out) {
        out.writerIndex(LENGTH_HEADER_SIZE);
        out.writeInt(msg.requestId);
        writeResponse(msg.object, msg.responseType, out);
        writeLength(out);
    }

    private void writeResponse(Object msg, ResponseType responseType, ByteBuf out) {
        responseType.write(out, msg);
    }

    private void writeLength(ByteBuf out) {
        out.setShort(0, out.writerIndex());
    }

    public static class ResponseMessage {
        private final int requestId;
        private final Object object;
        private final ResponseType responseType;

        public ResponseMessage(int requestId, Object object, ResponseType responseType) {
            this.requestId = requestId;
            this.object = object;
            this.responseType = responseType;
        }
    }
}
