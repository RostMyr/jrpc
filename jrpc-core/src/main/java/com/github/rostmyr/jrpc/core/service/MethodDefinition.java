package com.github.rostmyr.jrpc.core.service;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MethodDefinition {
    private final int methodId;
    private final int responseResourceId;
    private final List<ResponseType> inputTypes;
    private final ResponseType responseType;
    private final Method method;
    private final MethodHandle methodHandle;

    public MethodDefinition(
        int methodId,
        Method method,
        MethodHandle methodHandle,
        int responseResourceId
    ) {
        this.methodId = methodId;
        this.inputTypes = getInputTypes(method);
        this.responseResourceId = responseResourceId;
        this.method = method;
        this.methodHandle = methodHandle;
        this.responseType = ResponseType.of(method.getReturnType());
    }

    private List<ResponseType> getInputTypes(Method method) {
        return Stream.of(method.getParameterTypes())
            .map(ResponseType::of)
            .collect(toList());
    }

    public int getMethodId() {
        return methodId;
    }

    public int getResponseResourceId() {
        return responseResourceId;
    }

    public List<ResponseType> getInputTypes() {
        return inputTypes;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public Method getMethod() {
        return method;
    }

    public Object invoke(Object... args) {
        try {
            return methodHandle.invokeWithArguments(args);
        } catch (Throwable e) {
            throw new RuntimeException("Can't invoke service " + args[0] + " with args " + Arrays.toString(args));
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
            && Objects.equals(getMethod(), that.getMethod());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodId(), getMethod());
    }
}
