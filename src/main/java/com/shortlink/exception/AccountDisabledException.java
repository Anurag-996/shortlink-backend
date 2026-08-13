package com.shortlink.exception;

public class AccountDisabledException extends AuthException {
    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException() {
        super("Please verify your email address before logging in");
    }
}
