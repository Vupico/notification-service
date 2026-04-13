package com.vupico.notification.dto;

/**
 * Known {@code message_type} values. Extend as new events are added.
 */
public final class NotificationMessageTypes {

    public static final String DEFECT_LOGGED = "defect_logged";

    public static final String CHANGE_REQUEST_LOGGED = "change_request_logged";

    private NotificationMessageTypes() {}
}
