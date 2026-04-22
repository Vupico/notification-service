package com.vupico.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Envelope from the ticket system. {@code payload} is a JSON object as a map (nested values may be
 * maps, lists, or scalars).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NotificationMessage {

    private String tenantId;
    private NotificationChannelType notificationType;
    private String messageType;
    private String notificationId;
    private List<String> addressList = new ArrayList<>();
    private String payloadVersion;
    private Map<String, Object> payload;

    public static NotificationMessage parse(String json, ObjectMapper mapper) throws JsonProcessingException {
        return mapper.readValue(json, NotificationMessage.class);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public NotificationChannelType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationChannelType notificationType) {
        this.notificationType = notificationType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public List<String> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<String> addressList) {
        this.addressList = addressList != null ? addressList : new ArrayList<>();
    }

    public String getPayloadVersion() {
        return payloadVersion;
    }

    public void setPayloadVersion(String payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
