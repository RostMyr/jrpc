package com.github.rostmyr.jrpc.core.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.core.server.registry.ResourceRegistry;
import com.github.rostmyr.jrpc.core.service.MethodDefinition;
import com.github.rostmyr.jrpc.core.service.ResponseType;
import com.github.rostmyr.jrpc.core.service.ServerServiceDefinition;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class TransportServerDecoder extends ByteToMessageDecoder {
    public static final int MAX_FRAME_SIZE = 2048;

    public static final int LENGTH_HEADER_SIZE = 2;
    public static final int REQUEST_ID_SIZE = 4;

    private static final int RESOURCE_ID_SIZE = 2;
    private static final int METHOD_ID_SIZE = 1;
    private static final int ADDRESS_SIZE = 1;
    private static final int TOTAL_HEADER_SIZE = REQUEST_ID_SIZE
        + LENGTH_HEADER_SIZE
        + RESOURCE_ID_SIZE
        + METHOD_ID_SIZE
        + ADDRESS_SIZE;

    private final ResourceRegistry resourceRegistry;
    private final Map<String, ServerServiceDefinition> definitionsByAddress;

    TransportServerDecoder(ResourceRegistry resourceRegistry, Map<String, ServerServiceDefinition> definitionsByAddress) {
        this.resourceRegistry = resourceRegistry;
        this.definitionsByAddress = definitionsByAddress;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int readableBytes = in.readableBytes();
        if (readableBytes > MAX_FRAME_SIZE) {
            in.skipBytes(readableBytes);
            throw new TooLongFrameException("Frame exceeds the limit: " + readableBytes);
        }
        if (readableBytes < TOTAL_HEADER_SIZE) {
            return;
        }
        int length = in.markReaderIndex().readUnsignedShort();
        if (readableBytes < length) {
            in.resetReaderIndex();
            return;
        }

        int requestId = in.readInt();
        short methodId = in.readUnsignedByte();
        int resourceId = in.readUnsignedShort();
        String address = (String) in.readCharSequence(in.readUnsignedByte(), StandardCharsets.UTF_8);

        Resource resource = resourceRegistry.get(resourceId);
        resource.read(in);

        ServerServiceDefinition serviceDefinition = definitionsByAddress.get(address);
        Contract.checkArg(serviceDefinition, "There is no service definition for address: " + address);

        MethodDefinition method = serviceDefinition.getMethod(methodId);

        // TODO move call to the separate thread
        Object response = method.invoke(serviceDefinition.getService(), resource);
        ResponseType responseType = response == null ? ResponseType.NULL : method.getResponseType();

        ctx.writeAndFlush(new TransportServerEncoder.ResponseMessage(requestId, response, responseType));
    }
}
