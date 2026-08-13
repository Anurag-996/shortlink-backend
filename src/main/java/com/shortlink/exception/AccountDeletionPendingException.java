package com.shortlink.exception;

// Exception thrown when an authenticated login attempt occurs on an account queued in the 7-day deletion grace period.
public class AccountDeletionPendingException extends AuthException {
    public AccountDeletionPendingException(String message) {
        super(message);
    }

    public AccountDeletionPendingException() {
        super("Your account is scheduled for permanent deletion.");
    }
}
