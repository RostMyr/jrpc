package com.github.rostmyr.jrpc.core.exception;

public class ApiDefinitionParseException extends RuntimeException {
    public ApiDefinitionParseException() {
    }

    public ApiDefinitionParseException(String message) {
        super(message);
    }

    public ApiDefinitionParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiDefinitionParseException(Throwable cause) {
        super(cause);
    }

    public ApiDefinitionParseException(
        String message,
        Throwable cause,
        boolean enableSuppression,
        boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
