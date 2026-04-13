package com.vupico.notification.service;

/**
 * Thrown when outbound email fails (e.g. SES throttling); triggers consumer retry/DLQ policy.
 * Optional {@link #httpStatusCode} tags DLQ messages for HTTP 5xx replay when set.
 */
public class EmailSendException extends RuntimeException {

    private final Integer httpStatusCode;

    public EmailSendException(String message) {
        super(message);
        this.httpStatusCode = null;
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = null;
    }

    public EmailSendException(String message, Throwable cause, Integer httpStatusCode) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}
