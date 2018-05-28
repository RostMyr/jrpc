package com.github.rostmyr.jrpc.core.client;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.core.service.ResponseType;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ServerCall {
    private static final AtomicInteger ID = new AtomicInteger();

    private final int id;
    private final Resource resource;
    private final String address;
    private final int methodId;
    private final ResponseType responseType;

    public ServerCall(Resource resource, String address, int methodId, Class<?> responseType) {
        this.id = ID.getAndIncrement();
        this.resource = resource;
        this.address = address;
        this.methodId = methodId;
        this.responseType = ResponseType.of(responseType);
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public int getMethodId() {
        return methodId;
    }

    public Resource getResource() {
        return resource;
    }

    public boolean isVoid() {
        return ResponseType.NULL == responseType;
    }

    public ResponseType getResponseType() {
        return responseType;
    }
}
