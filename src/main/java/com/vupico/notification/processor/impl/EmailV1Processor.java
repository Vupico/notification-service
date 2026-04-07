package com.vupico.notification.processor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupico.notification.dto.DefectLoggedPayloadV1;
import com.vupico.notification.dto.NotificationMessage;
import com.vupico.notification.dto.NotificationMessageTypes;
import com.vupico.notification.processor.NotificationProcessor;
import com.vupico.notification.service.EmailSender;
import com.vupico.notification.service.RenderedTemplate;
import com.vupico.notification.service.TemplateService;
import org.springframework.stereotype.Component;

@Component
public class EmailV1Processor implements NotificationProcessor {

    private final ObjectMapper mapper;
    private final TemplateService templateService;
    private final EmailSender emailSender;

    public EmailV1Processor(
            ObjectMapper mapper, TemplateService templateService, EmailSender emailSender) {
        this.mapper = mapper;
        this.templateService = templateService;
        this.emailSender = emailSender;
    }

    @Override
    public String getNotificationType() {
        return "email";
    }

    @Override
    public String getPayloadVersion() {
        return "v1";
    }

    @Override
    public Object deserialize(Object payload) {
        return mapper.convertValue(payload, DefectLoggedPayloadV1.class);
    }

    @Override
    public void process(NotificationMessage message, Object payloadObj) {
        DefectLoggedPayloadV1 payload = (DefectLoggedPayloadV1) payloadObj;
        String messageType = message.getMessageType();
        if (!isSupportedEmailV1MessageType(messageType)) {
            throw new IllegalArgumentException(
                    "EmailV1Processor unsupported message_type=%s (supported: %s, %s)"
                            .formatted(
                                    messageType,
                                    NotificationMessageTypes.DEFECT_LOGGED,
                                    NotificationMessageTypes.CHANGE_REQUEST_LOGGED));
        }
        RenderedTemplate rendered =
                templateService.renderEmail(message.getTenantId(), messageType, payload);
        String subject = rendered.getSubject();
        String body = rendered.getBody();
        String fromDisplay = payload.getReportedByDisplay();
        if (fromDisplay == null || fromDisplay.isBlank()) {
            fromDisplay = payload.getReportedBy();
        }
        boolean highImportance =
                NotificationMessageTypes.DEFECT_LOGGED.equalsIgnoreCase(messageType)
                        && isHighSeverity(payload.getSeverity());
        emailSender.sendBatch(
                message.getTenantId(),
                message.getAddressList(),
                subject,
                body,
                blankToNull(fromDisplay),
                highImportance);
    }

    private static boolean isSupportedEmailV1MessageType(String messageType) {
        if (messageType == null) {
            return false;
        }
        return NotificationMessageTypes.DEFECT_LOGGED.equalsIgnoreCase(messageType)
                || NotificationMessageTypes.CHANGE_REQUEST_LOGGED.equalsIgnoreCase(messageType);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** Matches ticket UI priority {@code High} (payload field {@code severity} is ticket priority). */
    private static boolean isHighSeverity(String severity) {
        return severity != null && "high".equalsIgnoreCase(severity.trim());
    }
}
