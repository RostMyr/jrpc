package com.github.rostmyr.jrpc.core.service.reader;

import com.github.rostmyr.jrpc.common.io.Resource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.toList;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class MethodDescriptorReader {

    public List<MethodDescriptor> read(Method... methods) {
        return Stream.of(methods)
            .map(this::read)
            .filter(Objects::nonNull)
            .collect(toList());
    }

    public MethodDescriptor read(Method method) {
        int modifiers = method.getModifiers();
        if (!isPublic(modifiers) || isStatic(modifiers)) {
            return null;
        }
        Class<? extends Resource> inputType = getInputType(method.getParameterTypes());
        if (inputType == null) {
            return null;
        }
        return new MethodDescriptor(method, inputType);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Resource> getInputType(Class<?>[] parameterTypes) {
        if (parameterTypes.length != 1) {
            return null;
        }
        Class<?> parameterClass = parameterTypes[0];
        if (!Resource.class.isAssignableFrom(parameterClass) || parameterClass == Resource.class) {
            return null;
        }
        return (Class<? extends Resource>) parameterClass;
    }
}
