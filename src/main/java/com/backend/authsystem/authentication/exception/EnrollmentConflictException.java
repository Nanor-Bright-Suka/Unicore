package com.backend.authsystem.authentication.exception;

public class EnrollmentConflictException extends RuntimeException {
    public EnrollmentConflictException(String message) {
        super(message);
    }
}
