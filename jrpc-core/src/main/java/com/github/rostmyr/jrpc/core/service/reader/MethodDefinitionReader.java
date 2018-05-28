package com.github.rostmyr.jrpc.core.service.reader;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.core.service.MethodDefinition;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TreeMap;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class MethodDefinitionReader {
    private final MethodDescriptorReader descriptorReader = new MethodDescriptorReader();

    /**
     * Parses a class to produce a list of {@link MethodDefinition} objects
     *
     * @param clazz a class to parse methods
     * @return list with method definitions
     */
    public List<MethodDefinition> read(Class<?> clazz) {
        TreeMap<String, MethodDescriptor> descriptorByEndpointAddress = new TreeMap<>();
        for (MethodDescriptor descriptor : descriptorReader.read(clazz.getDeclaredMethods())) {
            descriptorByEndpointAddress.put(descriptor.getAddress(), descriptor);
        }
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        return range(0, descriptorByEndpointAddress.size())
            .mapToObj(idx -> toMethodDefinition(clazz, descriptorByEndpointAddress.pollFirstEntry().getValue(), lookup, idx))
            .collect(toList());
    }

    private MethodDefinition toMethodDefinition(
        Class<?> clazz, MethodDescriptor descriptor, MethodHandles.Lookup lookup, int idx
    ) {
        Class<? extends Resource> inputType = descriptor.getInputType();
        Method method = descriptor.getMethod();
        try {
            MethodHandle methodHandle = lookup.unreflect(method);
            return new MethodDefinition(idx, method, methodHandle, inputType, getResourceId(inputType));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Can't access method " + method.getName() + " in " + clazz);
        }
    }

    private static int getResourceId(Class<? extends Resource> clazz) {
        try {
            Field resourceId = clazz.getField("_resourceId");
            return resourceId.getInt(null); // static field
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("There is no auto-generated '_resourceId' field in " + clazz);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Can't access auto-generated '_resourceId' field in " + clazz);
        }
    }
}
