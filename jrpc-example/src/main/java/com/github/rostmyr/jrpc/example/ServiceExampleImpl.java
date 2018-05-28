package com.github.rostmyr.jrpc.example;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
public class ServiceExampleImpl implements ServiceExample {

    @Override
    public int compute(ComputeResource resource) {
        return resource.getFactor() * 41;
    }

    @Override
    public String sayHello(GreetingResource greeting) {
        return "Hello, " + greeting.getName();
    }

    @Override
    public UserInfoResource getUserInfo(GetUserResource getUserResource) {
        return new UserInfoResource("Ivan", "Ivanov");
    }
}
