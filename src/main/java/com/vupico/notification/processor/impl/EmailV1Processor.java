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
        if (!NotificationMessageTypes.DEFECT_LOGGED.equalsIgnoreCase(message.getMessageType())) {
            throw new IllegalArgumentException(
                    "EmailV1Processor only supports message_type=%s, got %s"
                            .formatted(NotificationMessageTypes.DEFECT_LOGGED, message.getMessageType()));
        }
        RenderedTemplate rendered =
                templateService.renderEmail(message.getTenantId(), message.getMessageType(), payload);
        String subject = rendered.getSubject();
        String body = rendered.getBody();
        emailSender.sendBatch(message.getTenantId(), message.getAddressList(), subject, body);
    }
}
