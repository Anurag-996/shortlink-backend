package com.shortlink.exception;

// Exception thrown when email dispatch fails.
public class EmailSendingException extends RuntimeException {
    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
