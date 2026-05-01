package com.vupico.notification.dto;

/**
 * Known {@code message_type} values. Extend as new events are added.
 */
public final class NotificationMessageTypes {

    public static final String DEFECT_LOGGED = "defect_logged";

    public static final String CHANGE_REQUEST_LOGGED = "change_request_logged";

    public static final String DEFECT_UPDATED = "defect_updated";

    public static final String SURVEY_ASSIGNED = "survey_assigned";

    public static final String SURVEY_COMPLETED = "survey_completed";

    public static final String SURVEY_REMINDER = "survey_reminder";

    private NotificationMessageTypes() {}
}
