package com.github.rostmyr.jrpc.example.fibers;

import com.github.rostmyr.jrpc.fibers.FiberManager;
import com.github.rostmyr.jrpc.fibers.FiberManagers;

/**
 * Rostyslav Myroshnychenko
 * on 02.06.2018.
 */
public class Application {
    public static void main(String[] args) {
        FiberManager fiberManager = FiberManagers.current();
        FooService fooService = new FooService();
        fiberManager.schedule(fooService.callAnotherFiber());
        fiberManager.run();
    }
}
