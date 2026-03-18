package com.vupico.notification.processor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationProcessorRegistry {

    private final Map<String, NotificationProcessor> byKey = new ConcurrentHashMap<>();

    public NotificationProcessorRegistry(List<NotificationProcessor> processors) {
        for (NotificationProcessor p : processors) {
            String key = key(p.getNotificationType(), p.getPayloadVersion());
            NotificationProcessor existing = byKey.putIfAbsent(key, p);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate processor for key=%s: %s vs %s"
                                .formatted(key, existing.getClass().getName(), p.getClass().getName()));
            }
        }
    }

    public Optional<NotificationProcessor> find(String notificationType, String payloadVersion) {
        if (notificationType == null || payloadVersion == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byKey.get(key(notificationType, payloadVersion)));
    }

    public NotificationProcessor require(String notificationType, String payloadVersion) {
        return find(notificationType, payloadVersion)
                .orElseThrow(() -> new UnsupportedNotificationProcessorException(
                        notificationType, payloadVersion));
    }

    private static String key(String notificationType, String payloadVersion) {
        return notificationType.trim().toLowerCase(Locale.ROOT)
                + "|"
                + payloadVersion.trim().toLowerCase(Locale.ROOT);
    }
}
