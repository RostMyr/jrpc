package com.github.rostmyr.jrpc.example.fibers;

import com.github.rostmyr.jrpc.example.fibers.repository.User;
import com.github.rostmyr.jrpc.example.fibers.service.UserService;
import com.github.rostmyr.jrpc.fibers.Fiber;
import com.github.rostmyr.jrpc.fibers.FiberManager;
import com.github.rostmyr.jrpc.fibers.FiberManagers;

import static com.github.rostmyr.jrpc.fibers.Fiber.call;
import static com.github.rostmyr.jrpc.fibers.Fiber.nothing;

/**
 * Rostyslav Myroshnychenko
 * on 02.06.2018.
 */
public class Application {
    private final UserService service = new UserService();

    public static void main(String[] args) {
        FiberManager fiberManager = FiberManagers.current();
        fiberManager.schedule(new Application().start());
        fiberManager.run();
    }

    public Fiber<Void> start() {
        Long id = call(service.saveUser("Ivan", "Ivanov"));
        System.out.println("New user id: " + id);

        User user = call(service.getUser(id));
        System.out.println("User data: " + user);

        return nothing();
    }
}
