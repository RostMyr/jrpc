package com.github.rostmyr.jrpc.example.rpc;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.common.annotation.ResourceId;
import com.github.rostmyr.jrpc.common.io.BaseResource;

import java.nio.charset.StandardCharsets;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
@ResourceId(3)
public class UserInfoResource extends BaseResource {
    private String firstName;
    private String lastName;

    public UserInfoResource(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public void read(ByteBuf in) {
        this.firstName = (String) in.readCharSequence(in.readUnsignedShort(), StandardCharsets.UTF_8);
        this.lastName = (String) in.readCharSequence(in.readUnsignedShort(), StandardCharsets.UTF_8);
    }

    @Override
    public void write(ByteBuf out) {
        out.writeShort(firstName.length());
        out.writeCharSequence(firstName, StandardCharsets.UTF_8);
        out.writeShort(lastName.length());
        out.writeCharSequence(lastName, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "UserInfoResource{"
            + "firstName='" + firstName + '\''
            + ", lastName='" + lastName + '\''
            + '}';
    }
}
