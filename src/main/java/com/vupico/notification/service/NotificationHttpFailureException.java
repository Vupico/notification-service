package com.vupico.notification.service;

/**
 * Thrown when an outbound call fails with an HTTP response status (e.g. SES HTTP API). Used to
 * tag DLQ messages for 5xx replay.
 */
public class NotificationHttpFailureException extends RuntimeException {

    private final int statusCode;

    public NotificationHttpFailureException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public NotificationHttpFailureException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
