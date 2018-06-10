package com.github.rostmyr.jrpc.fibers;

import static com.github.rostmyr.jrpc.fibers.Fiber.call;
import static com.github.rostmyr.jrpc.fibers.Fiber.result;

/**
 * Rostyslav Myroshnychenko
 * on 02.06.2018.
 */
public class FooService {

    public Fiber<String> bar(String second) {
        String first = call(getFirst());
        return result(join(first, second));
    }

    public static String join(String one, String another) {
        return String.join(one, another);
    }

    public String getFirst() {
        return "Hello";
    }
}
