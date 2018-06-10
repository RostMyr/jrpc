package com.github.rostmyr.jrpc.example.fibers.service;

import com.github.rostmyr.jrpc.example.fibers.repository.User;
import com.github.rostmyr.jrpc.example.fibers.repository.UserRepository;
import com.github.rostmyr.jrpc.fibers.Fiber;

import static com.github.rostmyr.jrpc.fibers.Fiber.result;

/**
 * Rostyslav Myroshnychenko
 * on 07.06.2018.
 */
public class UserService {
    protected final UserRepository repository = new UserRepository();
    protected long idSequence = 1L;

    public Fiber<User> getUser(long id) {
        return result(repository.getById(id));
    }

    public Fiber<Long> saveUser(String firstName, String lastName) {
        long id = idSequence++;
        repository.save(new User(id, firstName, lastName));
        return result(id);
    }
}
