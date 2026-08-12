package com.shortlink.exception;

public class AccountDisabledException extends AuthException {
    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException() {
        super("User account is disabled");
    }
}
