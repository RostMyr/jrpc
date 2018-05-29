# jrpc
Tiny and simple RPC framework

## Getting Started
See `jrpc-example` module for more information
### Creating server
```
Server server = ServerBuilder.forPort(serverPort)
     .addService(`one or more objects which implement interface`)
     .build();
```

### Creating client
```
ClientChannel clientChannel = ClientBuilder.forPort(serverPort)
    .build();
```

### Create one or more proxy by providing an interface
```
SomeService service = clientChannel.createProxy(SomeService.class);
```

#### Supported return types for service methods
- primitives and wrappers
- String
- objects which implement `Resource` interface

#### Supported input types for service methods
For now it support only methods with one input parameter which implements `Resource` interface  
but I'm planning to add a support for 1...* args which can have the same type as available return types.

### Do remote call
```
int response = service.callMethod(new SomeResource());
```
Note that it's a blocking call but I'm planning to implement a tiny library to work with `coroutine`|`fibers`
in order to avoid blocking calls and usage of callbacks/listeners.

### Stop the client and server
```
clientChannel.shutdown();

server.shutdown();
server.awaitTermination();
```

### What is a resource
`Resource` interface provides two methods to serialize/deserialize object and an additional method which returns an integer which uniquely identifies a resource. The recommended way to create resource is to extend the `BaseResource` abstract class and annotate it with a `ResourceId` annotation which has required int value - resource id. `jrpc-maven-plugin` instruments all classes with `ResoureId` annotation and adds a default constructor, static field `_resourceId` and implements a method `getResourceId`. These info is used then in runtime to create a proper mapping between resource ids and resource suppliers in order to serialize/desirialize resource during the network call.
