package com.github.rostmyr.jrpc.core.service.reader;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.core.service.MethodDefinition;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class MethodDefinitionReader {
    /**
     * Parses a class to produce a list of {@link MethodDefinition} objects
     *
     * @param clazz a class to parse methods
     * @return list with method definitions
     */
    public List<MethodDefinition> read(Class<?> clazz) {
        TreeMap<String, Method> methodsByEndpointAddress = new TreeMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (isPublic(modifiers) && !isStatic(modifiers)) {
                methodsByEndpointAddress.put(getAddress(method), method);
            }
        }
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        return range(0, methodsByEndpointAddress.size())
            .mapToObj(idx -> toMethodDefinition(clazz, methodsByEndpointAddress.pollFirstEntry().getValue(), lookup, idx))
            .collect(toList());
    }

    private MethodDefinition toMethodDefinition(Class<?> clazz, Method method, MethodHandles.Lookup lookup, int idx) {
        try {
            return new MethodDefinition(idx, method, lookup.unreflect(method), getResourceId(method.getReturnType()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Can't access method " + method.getName() + " in " + clazz);
        }
    }

    @SuppressWarnings("unchecked")
    private static int getResourceId(Class<?> clazz) {
        if (Resource.class.isAssignableFrom(clazz)) {
            try {
                Field resourceId = clazz.getField("_resourceId");
                return resourceId.getInt(null); // static field
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("There is no auto-generated '_resourceId' field in " + clazz);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Can't access auto-generated '_resourceId' field in " + clazz);
            }
        }
        return -1;
    }

    private static String getAddress(Method method) {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        return methodName + Stream.of(parameterTypes).map(Class::getSimpleName).collect(joining());
    }
}
