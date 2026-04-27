package com.vupico.notification.processor;

import com.vupico.notification.dto.NotificationChannelType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the {@link NotificationProcessor} for a message using {@code notification_type} only.
 * Payload shape and optional fields are handled inside each processor.
 */
@Component
public class NotificationProcessorRegistry {

    private final Map<String, NotificationProcessor> byNotificationType;

    public NotificationProcessorRegistry(List<NotificationProcessor> processors) {
        this.byNotificationType =
                Map.copyOf(
                        processors.stream()
                                .collect(
                                        Collectors.toMap(
                                                p -> normalizeNotificationType(p.getNotificationType()),
                                                p -> p,
                                                (a, b) -> {
                                                    throw new IllegalStateException(
                                                            "Duplicate processor for notification_type=%s: %s vs %s"
                                                                    .formatted(
                                                                            a.getNotificationType(),
                                                                            a.getClass().getName(),
                                                                            b.getClass().getName()));
                                                })));
    }

    /**
     * Looks up a processor by {@link NotificationChannelType} (same as JSON {@code notification_type}).
     */
    public NotificationProcessor require(NotificationChannelType channelType) {
        if (channelType == null) {
            throw new UnsupportedNotificationProcessorException(null);
        }
        return require(channelType.getValue());
    }

    /**
     * Looks up a processor by {@code notification_type} string (case-insensitive).
     */
    public NotificationProcessor require(String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            throw new UnsupportedNotificationProcessorException(String.valueOf(notificationType));
        }
        NotificationProcessor p = byNotificationType.get(normalizeNotificationType(notificationType));
        if (p == null) {
            throw new UnsupportedNotificationProcessorException(notificationType);
        }
        return p;
    }

    private static String normalizeNotificationType(String notificationType) {
        return notificationType.trim().toLowerCase(Locale.ROOT);
    }
}
