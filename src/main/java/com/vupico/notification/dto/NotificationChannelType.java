package com.vupico.notification.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Channel for dispatch (email, SMS, etc.).
 */
public enum NotificationChannelType {

    EMAIL("email"),
    SMS("sms");

    private final String value;

    NotificationChannelType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NotificationChannelType fromValue(String raw) {
        if (raw == null) {
            return null;
        }
        for (NotificationChannelType t : values()) {
            if (t.value.equalsIgnoreCase(raw)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown notification_type: " + raw);
    }
}
