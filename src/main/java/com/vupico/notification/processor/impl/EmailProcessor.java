package com.vupico.notification.processor.impl;

import com.vupico.notification.dto.NotificationMessage;
import com.vupico.notification.processor.NotificationProcessor;
import com.vupico.notification.service.EmailSender;
import com.vupico.notification.service.RenderedTemplate;
import com.vupico.notification.service.TemplateService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Processor for {@code notification_type=email}. Renders {@link TemplateService} output from
 * {@code notification_template} using {@code notification_type}, {@code message_type}, and
 * {@code payload_version}.
 */
@Component
public class EmailProcessor implements NotificationProcessor {

    private final TemplateService templateService;
    private final EmailSender emailSender;

    public EmailProcessor(TemplateService templateService, EmailSender emailSender) {
        this.templateService = templateService;
        this.emailSender = emailSender;
    }

    @Override
    public String getNotificationType() {
        return "email";
    }

    @Override
    public void process(NotificationMessage message) {
        Map<String, Object> payload = message.getPayload();
        if (message.getNotificationType() == null) {
            throw new IllegalArgumentException("missing notification_type");
        }
        RenderedTemplate rendered =
                templateService.render(
                        message.getNotificationType().getValue(),
                        message.getMessageType(),
                        message.getPayloadVersion(),
                        payload);
        String fromDisplay = null;
        boolean highImportance = false;
        String display = stringField(payload, "display_name");
        fromDisplay = blankToNull(display);
        highImportance = isHighSeverity(stringField(payload, "severity"));

        emailSender.sendBatch(
                message.getTenantId(),
                message.getAddressList(),
                rendered.getSubject(),
                rendered.getBody(),
                fromDisplay,
                highImportance);
    }

    private static String stringField(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof String s) {
            return s;
        }
        return String.valueOf(v);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static boolean isHighSeverity(String severity) {
        if (severity == null) {
            return false;
        }
        String v = severity.trim();
        return "high".equalsIgnoreCase(v) || "1".equals(v) || "2".equals(v);
    }
}
