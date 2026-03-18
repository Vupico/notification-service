package com.vupico.notification.processor;

import com.vupico.notification.dto.NotificationMessage;

/**
 * Handles a single ({@code notification_type}, {@code payload_version}) pair. Inner payload shape
 * is defined by the processor implementation.
 */
public interface NotificationProcessor {

    /** Matches JSON {@code notification_type}, e.g. {@code email}. */
    String getNotificationType();

    /** Matches JSON {@code payload_version}, e.g. {@code v1}. */
    String getPayloadVersion();

    /**
     * Converts the raw {@code payload} object (e.g. {@link com.fasterxml.jackson.databind.JsonNode}
     * or {@link java.util.Map}) to a typed model.
     */
    Object deserialize(Object payload);

    void process(NotificationMessage message, Object payload);
}
