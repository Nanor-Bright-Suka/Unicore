package com.backend.authsystem.authentication.exception;

public class TooManyLoginAttemptsException extends RuntimeException {
    public TooManyLoginAttemptsException(String message) {
        super(message);
    }
}
