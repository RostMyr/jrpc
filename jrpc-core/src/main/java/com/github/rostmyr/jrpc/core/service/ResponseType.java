package com.github.rostmyr.jrpc.core.service;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.core.server.registry.ResourceRegistry;

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public enum ResponseType {
    NULL((buffer, registry) -> null, (buffer, object) -> {}),
    BYTE((buffer, registry) -> buffer.readByte(), (buffer, object) -> buffer.writeByte(Byte.class.cast(object))),
    SHORT((buffer, registry) -> buffer.readShort(), (buffer, object) -> buffer.writeShort(Short.class.cast(object))),
    CHAR((buffer, registry) -> buffer.readChar(), (buffer, object) -> buffer.writeChar(Character.class.cast(object))),
    INT((buffer, registry) -> buffer.readInt(), (buffer, object) -> buffer.writeInt(Integer.class.cast(object))),
    LONG((buffer, registry) -> buffer.readLong(), (buffer, object) -> buffer.writeLong(Long.class.cast(object))),
    FLOAT((buffer, registry) -> buffer.readFloat(), (buffer, object) -> buffer.writeFloat(Float.class.cast(object))),
    DOUBLE((buffer, registry) -> buffer.readDouble(), (buffer, object) -> buffer.writeDouble(Double.class.cast(object))),
    STRING((buffer, registry) -> {
        int length = buffer.readUnsignedShort();
        return buffer.readCharSequence(length, StandardCharsets.UTF_8);
    }, (buffer, object) -> {
        String string = (String) object;
        buffer.writeShort(string.length()).writeCharSequence(string, StandardCharsets.UTF_8);
    }),
    RESOURCE((buffer, registry) -> {
        int resourceId = buffer.readUnsignedShort();
        Resource resource = registry.get(resourceId);
        resource.read(buffer);
        return resource;
    }, (buffer, object) -> {
        Resource resource = (Resource) object;
        buffer.writeShort(resource.getResourceId());
        resource.write(buffer);
    });

    private final BiFunction<ByteBuf, ResourceRegistry, Object> reader;
    private final BiConsumer<ByteBuf, Object> writer;

    ResponseType(BiFunction<ByteBuf, ResourceRegistry, Object> reader, BiConsumer<ByteBuf, Object> writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public Object read(ByteBuf buffer, ResourceRegistry resourceRegistry) {
        return reader.apply(buffer, resourceRegistry);
    }

    public void write(ByteBuf buffer, Object object) {
        writer.accept(buffer, object);
    }

    public static ResponseType of(Class<?> clazz) {
        if (clazz.equals(void.class) || clazz.equals(Void.class)) {
            return NULL;
        }
        if (clazz.equals(byte.class) || clazz.equals(Byte.class)) {
            return BYTE;
        }
        if (clazz.equals(short.class) || clazz.equals(Short.class)) {
            return SHORT;
        }
        if (clazz.equals(char.class) || clazz.equals(Character.class)) {
            return CHAR;
        }
        if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return INT;
        }
        if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            return LONG;
        }
        if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            return FLOAT;
        }
        if (clazz.equals(double.class) || clazz.equals(Double.class)) {
            return DOUBLE;
        }
        if (String.class.isAssignableFrom(clazz)) {
            return STRING;
        }
        if (Resource.class.isAssignableFrom(clazz)) {
            return RESOURCE;
        }

        throw new IllegalArgumentException("Unknown type: " + clazz);
    }
}
