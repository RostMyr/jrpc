package com.github.rostmyr.jrpc.example.fibers;

import com.github.rostmyr.jrpc.fibers.Fiber;

import static com.github.rostmyr.jrpc.fibers.Fiber.call;
import static com.github.rostmyr.jrpc.fibers.Fiber.result;

/**
 * Rostyslav Myroshnychenko
 * on 02.06.2018.
 */
public class FooService {

    public Fiber<String> callWithParameter(String second) {
        String first = call(getFirst());
        return result(join(first, second));
    }

    public Fiber<String> constant() {
        return result("A");
    }

    public Fiber<String> callAnotherFiber() {
        String constant = call(callWithParameter("A"));
        return result(constant + "B");
    }


    public static String join(String one, String another) {
        return one + another;
    }

    public String getFirst() {
        return "Hello";
    }
}
