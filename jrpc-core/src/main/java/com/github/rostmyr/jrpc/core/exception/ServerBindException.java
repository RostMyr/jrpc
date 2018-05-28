package com.github.rostmyr.jrpc.core.exception;

import java.io.IOException;

/**
 * Rostyslav Myroshnychenko
 * on 22.05.2018.
 */
public class ServerBindException extends IOException {
    public ServerBindException() {
    }

    public ServerBindException(String message) {
        super(message);
    }

    public ServerBindException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerBindException(Throwable cause) {
        super(cause);
    }
}
