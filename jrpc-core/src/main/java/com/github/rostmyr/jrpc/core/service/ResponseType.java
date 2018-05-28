package com.github.rostmyr.jrpc.core.service;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public enum ResponseType {
    NULL(buffer -> null, (buffer, object) -> {}),
    BYTE(ByteBuf::readByte, (buffer, object) -> buffer.writeByte(Byte.class.cast(object))),
    SHORT(ByteBuf::readShort, (buffer, object) -> buffer.writeShort(Short.class.cast(object))),
    CHAR(ByteBuf::readChar, (buffer, object) -> buffer.writeChar(Character.class.cast(object))),
    INT(ByteBuf::readInt, (buffer, object) -> buffer.writeInt(Integer.class.cast(object))),
    LONG(ByteBuf::readLong, (buffer, object) -> buffer.writeLong(Long.class.cast(object))),
    FLOAT(ByteBuf::readFloat, (buffer, object) -> buffer.writeFloat(Float.class.cast(object))),
    DOUBLE(ByteBuf::readDouble, (buffer, object) -> buffer.writeDouble(Double.class.cast(object))),
    STRING(buffer -> {
        int length = buffer.readUnsignedShort();
        return buffer.readCharSequence(length, StandardCharsets.UTF_8);
    }, (buffer, object) -> {
        String string = (String) object;
        buffer.writeShort(string.length()).writeCharSequence(string, StandardCharsets.UTF_8);
    }),
    RESOURCE(buffer -> buffer.readInt(), (buffer, object) -> {
        Resource resource = (Resource) object;
        buffer.writeShort(resource.getResourceId());
        resource.write(buffer);
    });

    private final Function<ByteBuf, Object> reader;
    private final BiConsumer<ByteBuf, Object> writer;

    ResponseType(Function<ByteBuf, Object> reader, BiConsumer<ByteBuf, Object> writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public Object read(ByteBuf buffer) {
        return reader.apply(buffer);
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

        throw new IllegalArgumentException("Unknown type: " + clazz);
    }
}
