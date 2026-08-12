package com.shortlink.exception;

// Exception thrown when a bad request is received with invalid parameter values or business constraints.
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
