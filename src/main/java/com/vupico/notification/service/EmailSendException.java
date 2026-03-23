package com.vupico.notification.service;

/**
 * Thrown when outbound email fails (e.g. SES throttling); triggers consumer retry/DLQ policy.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
