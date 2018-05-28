package com.github.rostmyr.jrpc.core.service.reader;

import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.core.exception.ApiDefinitionParseException;
import com.github.rostmyr.jrpc.core.service.MethodDefinition;
import com.github.rostmyr.jrpc.core.service.ServerServiceDefinition;

import java.util.List;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ServiceDefinitionReader {
    private final MethodDefinitionReader methodDefinitionReader = new MethodDefinitionReader();

    /**
     * Creates a {@link ServerServiceDefinition} object using reflection
     *
     * @param object object to parse metadata
     * @return service definition
     */
    public ServerServiceDefinition read(Object object) {
        Class<?>[] parent = object.getClass().getInterfaces();
        Contract.checkArg(parent != null && parent.length == 1, "You must provide an object which implements interface");
        return read(object, parent[0]);
    }

    private ServerServiceDefinition read(Object object, Class clazz) {
        Contract.checkArg(clazz.isInterface(), () -> clazz + " is not an interface");
        List<MethodDefinition> methodDefinitions = methodDefinitionReader.read(clazz);
        if (methodDefinitions.isEmpty()) {
            throw new ApiDefinitionParseException("Can't parse definition for " + clazz + ". There is no valid methods");
        }
        return new ServerServiceDefinition(clazz.getSimpleName(), object, methodDefinitions);
    }
}
