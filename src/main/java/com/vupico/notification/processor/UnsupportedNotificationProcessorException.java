package com.vupico.notification.processor;

/** No processor registered for the given {@code notification_type} (unsupported channel). */
public class UnsupportedNotificationProcessorException extends RuntimeException {

    public UnsupportedNotificationProcessorException(String notificationType) {
        super("No processor for notification_type=%s".formatted(notificationType));
    }
}
