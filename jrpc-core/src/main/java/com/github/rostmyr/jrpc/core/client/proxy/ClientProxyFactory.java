package com.github.rostmyr.jrpc.core.client.proxy;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.core.client.ClientChannel;
import com.github.rostmyr.jrpc.core.server.registry.ImmutableResourceRegistry;
import com.github.rostmyr.jrpc.core.server.registry.MutableResourceRegistry;
import com.github.rostmyr.jrpc.core.server.registry.ResourceRegistry;
import com.github.rostmyr.jrpc.core.service.MethodDefinition;
import com.github.rostmyr.jrpc.core.service.ResponseType;
import com.github.rostmyr.jrpc.core.service.reader.MethodDefinitionReader;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ClientProxyFactory {
    private final MethodDefinitionReader reader = new MethodDefinitionReader();
    private final ResourceRegistry resourceRegistry = new MutableResourceRegistry();

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clazz, ClientChannel channel) {
        Contract.checkArg(clazz.isInterface(), "You must provide an interface to create a proxy");
        Contract.checkNotNull(channel, "Channel can't be null");

        Map<Method, MethodDefinition> methodDefinitionsByMethods = new HashMap<>();
        for (MethodDefinition methodDefinition : reader.read(clazz)) {
            ResponseType responseType = methodDefinition.getResponseType();
            if (responseType == ResponseType.RESOURCE) {
                resourceRegistry.add(
                    methodDefinition.getResponseResourceId(),
                    (Class<? extends Resource>) methodDefinition.getMethod().getReturnType()
                );
            }
            methodDefinitionsByMethods.put(methodDefinition.getMethod(), methodDefinition);
        }

        return clazz.cast(Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class[]{clazz},
            new ProxyInvocationHandler(clazz, channel, methodDefinitionsByMethods)
        ));
    }

    public ResourceRegistry getResourceRegistry() {
        return new ImmutableResourceRegistry(resourceRegistry);
    }
}
