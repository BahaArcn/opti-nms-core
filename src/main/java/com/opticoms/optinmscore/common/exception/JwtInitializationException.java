package com.opticoms.optinmscore.common.exception;

public class JwtInitializationException extends RuntimeException {

    public JwtInitializationException(String message) {
        super(message);
    }

    public JwtInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
