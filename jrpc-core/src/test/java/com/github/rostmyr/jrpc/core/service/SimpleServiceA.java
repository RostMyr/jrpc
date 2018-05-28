package com.github.rostmyr.jrpc.core.service;

import com.github.rostmyr.jrpc.core.resource.EmptyResource;

/**
 * Rostyslav Myroshnychenko
 * on 22.05.2018.
 */
public class SimpleServiceA implements ServiceA {
    public void doSomethingWithResource(EmptyResource resource) {
        System.out.println("Handle resource");
    }
}