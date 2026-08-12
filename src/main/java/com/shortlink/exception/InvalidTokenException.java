package com.shortlink.exception;

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException() {
        super("Invalid, expired, or revoked refresh token");
    }
}
