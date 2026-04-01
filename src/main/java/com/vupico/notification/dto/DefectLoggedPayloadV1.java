package com.vupico.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

/**
 * V1 ticket payload for email templates. Used for {@link NotificationMessageTypes#DEFECT_LOGGED} and
 * {@link NotificationMessageTypes#CHANGE_REQUEST_LOGGED} (same JSON shape from the ticket API; field
 * {@code defect_title} holds the ticket subject for both).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DefectLoggedPayloadV1 {

    private String ticketId;
    private String defectTitle;
    private String severity;
    private String reportedBy;
    private Instant reportedAt;
    private String applicationName;

    /** Full defect description from the ticket (optional for email templates). */
    private String description;

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getDefectTitle() {
        return defectTitle;
    }

    public void setDefectTitle(String defectTitle) {
        this.defectTitle = defectTitle;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(Instant reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
