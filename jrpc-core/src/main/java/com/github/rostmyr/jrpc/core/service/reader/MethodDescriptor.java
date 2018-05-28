package com.github.rostmyr.jrpc.core.service.reader;

import com.github.rostmyr.jrpc.common.io.Resource;

import java.lang.reflect.Method;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class MethodDescriptor {
    private final String address;
    private final Method method;
    private final Class<? extends Resource> inputType;

    public MethodDescriptor(Method method, Class<? extends Resource> inputType) {
        this.method = method;
        this.inputType = inputType;
        this.address = method.getName() + inputType.getSimpleName();
    }

    public String getAddress() {
        return address;
    }

    public Method getMethod() {
        return method;
    }

    public Class<? extends Resource> getInputType() {
        return inputType;
    }
}
