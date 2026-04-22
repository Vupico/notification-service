package com.vupico.notification.processor;

import com.vupico.notification.dto.NotificationMessage;

/**
 * Handles a single {@code notification_type} (e.g. email). Dispatch by {@code message_type} or payload
 * fields is up to each implementation.
 */
public interface NotificationProcessor {

    /** Matches JSON {@code notification_type}, e.g. {@code email}. */
    String getNotificationType();

    void process(NotificationMessage message);
}
