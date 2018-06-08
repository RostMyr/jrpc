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
//        long id = idSequence;
        repository.save(new User(idSequence, firstName, lastName));
        return result(idSequence);
    }

//    public class getUser_Fiber extends Fiber<User> {
//        private long id;
//
//        public getUser_Fiber(long var2) {
//            this.id = var2;
//        }
//
//        public int update() {
//            switch(this.state) {
//                case 0:
//                    this.result = repository.getById(this.id);
//                    return -1;
//                default:
//                    throw new IllegalStateException("Unknown state: " + this.state);
//            }
//        }
//    }
}
