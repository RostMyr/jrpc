package com.github.rostmyr.jrpc.core.service;

import com.github.rostmyr.jrpc.common.io.Resource;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Objects;

public class MethodDefinition {
    private final int methodId;
    private final int resourceId;
    private final Class<? extends Resource> inputType;
    private final ResponseType responseType;
    private final Method method;
    private final MethodHandle methodHandle;

    public MethodDefinition(
        int methodId, Method method, MethodHandle methodHandle, Class<? extends Resource> inputType, int resourceId
    ) {
        this.methodId = methodId;
        this.resourceId = resourceId;
        this.method = method;
        this.methodHandle = methodHandle;
        this.inputType = inputType;
        this.responseType = ResponseType.of(method.getReturnType());
    }

    public int getMethodId() {
        return methodId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public Class<? extends Resource> getInputType() {
        return inputType;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public Method getMethod() {
        return method;
    }

    public <T extends Resource> Object invoke(Object service, T resource) {
        try {
            return methodHandle.invoke(service, resource);
        } catch (Throwable e) {
            throw new RuntimeException(
                "Can't invoke service " + service.getClass() + " with resource " + resource.getClass()
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodDefinition that = (MethodDefinition) o;
        return getMethodId() == that.getMethodId()
            && getResourceId() == that.getResourceId()
            && Objects.equals(getInputType(), that.getInputType())
            && getResponseType() == that.getResponseType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodId(), getResourceId(), getInputType(), getResponseType());
    }
}
