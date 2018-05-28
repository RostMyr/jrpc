package com.github.rostmyr.jrpc.example;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
public interface ServiceExample {
    int compute(ComputeResource resource);

    String sayHello(GreetingResource greeting);

    UserInfoResource getUserInfo(GetUserResource getUserResource);
}
