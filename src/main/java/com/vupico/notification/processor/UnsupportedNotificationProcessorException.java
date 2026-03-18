package com.vupico.notification.processor;

/**
 * No processor registered for the given channel + payload version (poison / unsupported message).
 */
public class UnsupportedNotificationProcessorException extends RuntimeException {

    public UnsupportedNotificationProcessorException(String notificationType, String payloadVersion) {
        super("No processor for notification_type=%s payload_version=%s"
                .formatted(notificationType, payloadVersion));
    }
}
