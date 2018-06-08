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
    protected final UserService service = new UserService();

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

    public class AppFiber extends Fiber {
        private Long id;
        private User user;

        @Override
        public int update() {
            switch(this.state) {
                case 0:
                    awaitFor(service.saveUser("Ivan", "Ivanov"));
                    return 1;
                case 1:
                    return callInternal();
                case 2:
                    awaitFor(service.getUser((Long) result));
                    return 3;
                case 3:
                    return callInternal();
                case 4:
                    return nothingInternal();
                default:
                    throw new IllegalStateException("Unknown state: " + this.state);
            }
        }
    }
}
