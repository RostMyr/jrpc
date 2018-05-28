package com.github.rostmyr.jrpc.core.service;

import com.github.rostmyr.jrpc.common.utils.Contract;

import java.util.Collection;
import java.util.Map;

import static java.util.Collections.unmodifiableCollection;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Rostyslav Myroshnychenko
 * on 22.05.2018.
 */
public class ServerServiceDefinition {
    private final String name;
    private final Object service;
    private final Map<Integer, MethodDefinition> methods;

    public ServerServiceDefinition(String name, Object service, Collection<MethodDefinition> methods) {
        this.name = name;
        this.service = service;
        this.methods = toDefinitionsByName(methods);
    }

    private Map<Integer, MethodDefinition> toDefinitionsByName(Collection<MethodDefinition> methods) {
        return methods.stream().collect(toMap(MethodDefinition::getMethodId, identity()));
    }

    public String getName() {
        return name;
    }

    public Object getService() {
        return service;
    }

    public Collection<MethodDefinition> getMethods() {
        return unmodifiableCollection(methods.values());
    }

    public MethodDefinition getMethod(int methodId) {
        MethodDefinition methodDefinition = methods.get(methodId);
        Contract.checkArg(methodDefinition, "There is no method with id " + methodId + " in " + service);
        return methodDefinition;
    }
}
