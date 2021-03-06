package com.github.rostmyr.jrpc.example.rpc;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
public interface ServiceExample {
    int compute(ComputeResource resource);

    String sayHello(GreetingResource greeting);

    UserInfoResource getUserInfo(GetUserResource getUserResource);

    double sum(double a, double b);
}
