package com.github.rostmyr.jrpc.core.client.proxy;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.core.client.ClientChannel;
import com.github.rostmyr.jrpc.core.client.ServerCall;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ProxyInvocationHandler implements InvocationHandler {
    private final Class clazz;
    private final ClientChannel clientChannel;
    private final Map<Method, Integer> methodsIdByMethod;

    ProxyInvocationHandler(Class clazz, ClientChannel clientChannel, Map<Method, Integer> methodsIdByMethod) {
        this.clazz = clazz;
        this.clientChannel = clientChannel;
        this.methodsIdByMethod = methodsIdByMethod;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Contract.checkArg(args != null && args.length != 0 && args[0] != null, "You must provide resource argument");
        Contract.checkState(clazz.getSimpleName().length() <= 255, "Endpoint address should not be > 255 symbols");

        Resource resource = Resource.class.cast(args[0]);
        try {
            Class<?> returnType = method.getReturnType();
            String address = clazz.getSimpleName();
            ServerCall serverCall = new ServerCall(resource, address, methodsIdByMethod.get(method), returnType);
            return clientChannel.newCall(serverCall).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Error during the server call", e);
        }
    }
}
