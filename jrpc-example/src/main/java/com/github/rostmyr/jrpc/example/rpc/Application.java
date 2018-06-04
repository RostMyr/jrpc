package com.github.rostmyr.jrpc.example.rpc;

import com.github.rostmyr.jrpc.core.client.ClientBuilder;
import com.github.rostmyr.jrpc.core.client.ClientChannel;
import com.github.rostmyr.jrpc.core.exception.ServerBindException;
import com.github.rostmyr.jrpc.core.server.Server;
import com.github.rostmyr.jrpc.core.server.ServerBuilder;

/**
 * Rostyslav Myroshnychenko
 * on 28.05.2018.
 */
public class Application {
    public static void main(String[] args) throws ServerBindException, InterruptedException {
        // create and start a server
        Server server = ServerBuilder.forPort(4040)
            .addService(new ServiceExampleImpl())
            .build();

        server.start();

        // create and connect a client
        ClientChannel clientChannel = ClientBuilder.forPort(4040)
            .build();

        // create proxy
        ServiceExample serviceExample = clientChannel.createProxy(ServiceExample.class);

        // do remote calls
        int result = serviceExample.compute(new ComputeResource(10));
        System.out.println("Computing result is: " + result);

        String greeting = serviceExample.sayHello(new GreetingResource("World"));
        System.out.println("Greeting: " + greeting);

        UserInfoResource userInfo = serviceExample.getUserInfo(new GetUserResource(1));
        System.out.println("User info: " + userInfo);

        double sum = serviceExample.sum(2.0, 2.1);
        System.out.println("Sum of 2.0 + 2.1 is : " + sum);

        // stop the client
        clientChannel.shutdown();
        clientChannel.awaitTermination();

        // stop the server
        server.shutdown();
        server.awaitTermination();
    }
}
