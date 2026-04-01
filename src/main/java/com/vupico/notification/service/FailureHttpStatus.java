package com.vupico.notification.service;

/**
 * Extracts HTTP status from an exception chain for DLQ tagging and replay policy.
 */
public final class FailureHttpStatus {

    private FailureHttpStatus() {}

    /**
     * @return status code in {@code 500–599} if found, otherwise {@code null}
     */
    public static Integer findServerErrorCode(Throwable t) {
        Integer code = findAnyCode(t);
        if (code == null) {
            return null;
        }
        return (code >= 500 && code <= 599) ? code : null;
    }

    private static Integer findAnyCode(Throwable t) {
        while (t != null) {
            if (t instanceof NotificationHttpFailureException h) {
                return h.getStatusCode();
            }
            if (t instanceof EmailSendException e) {
                Integer s = e.getHttpStatusCode();
                if (s != null) {
                    return s;
                }
            }
            t = t.getCause();
        }
        return null;
    }
}
