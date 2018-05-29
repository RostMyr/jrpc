package com.github.rostmyr.jrpc.core.client;

import com.github.rostmyr.jrpc.core.service.MethodDefinition;
import com.github.rostmyr.jrpc.core.service.ResponseType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ServerCall {
    private static final AtomicInteger ID = new AtomicInteger();

    private final int id;
    private final Object[] args;
    private final List<ResponseType> inputTypes;
    private final String address;
    private final MethodDefinition methodDefinition;
    private final ResponseType responseType;

    public ServerCall(Object[] args, String address, MethodDefinition methodDefinition, Class<?> responseType) {
        this.id = ID.getAndIncrement();
        this.args = args;
        this.inputTypes = methodDefinition.getInputTypes();
        this.address = address;
        this.methodDefinition = methodDefinition;
        this.responseType = ResponseType.of(responseType);
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public int getMethodId() {
        return methodDefinition.getMethodId();
    }

    public Object[] getArgs() {
        return args;
    }

    public List<ResponseType> getInputTypes() {
        return inputTypes;
    }

    public boolean isVoid() {
        return ResponseType.NULL == responseType;
    }

    public ResponseType getResponseType() {
        return responseType;
    }
}
