package com.github.rostmyr.jrpc.core.client.proxy;

import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.core.client.ClientChannel;
import com.github.rostmyr.jrpc.core.service.MethodDefinition;
import com.github.rostmyr.jrpc.core.service.reader.MethodDefinitionReader;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ClientProxyFactory {
    private final MethodDefinitionReader reader = new MethodDefinitionReader();

    public <T> T create(Class<T> clazz, ClientChannel channel) {
        Contract.checkArg(clazz.isInterface(), "You must provide an interface to create a proxy");
        Contract.checkNotNull(channel, "Channel can't be null");

        Map<Method, Integer> methodsIdByMethod = reader.read(clazz).stream()
            .collect(toMap(MethodDefinition::getMethod, MethodDefinition::getMethodId));

        return clazz.cast(Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class[]{clazz},
            new ProxyInvocationHandler(clazz, channel, methodsIdByMethod)
        ));
    }
}
