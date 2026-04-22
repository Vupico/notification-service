package com.vupico.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupico.notification.template.NotificationTemplateEntity;
import com.vupico.notification.template.NotificationTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\.\\-]+)\\s*}}");

    private static final String DEFAULT_VERSION = "v1";

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final ObjectMapper objectMapper;

    public TemplateServiceImpl(
            NotificationTemplateRepository notificationTemplateRepository, ObjectMapper objectMapper) {
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public RenderedTemplate render(
            String notificationType,
            String messageType,
            String version,
            Map<String, Object> payload) {
        String nt = normalizeNotificationType(notificationType);
        String mt = normalizeMessageType(messageType);
        String ver = normalizeVersion(version);
        NotificationTemplateEntity template =
                notificationTemplateRepository
                        .findByNotificationTypeAndMessageTypeAndVersion(nt, mt, ver)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                ("No notification_template for notification_type=%s "
                                                                + "message_type=%s version=%s")
                                                        .formatted(nt, mt, ver)));

        JsonNode payloadNode = objectMapper.valueToTree(payload != null ? payload : Map.of());
        String subject = apply(template.getSubject(), payloadNode);
        String body = apply(template.getBody(), payloadNode);
        return new RenderedTemplate(subject, body);
    }

    private static String normalizeNotificationType(String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            return "";
        }
        return notificationType.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return "";
        }
        return messageType.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return DEFAULT_VERSION;
        }
        return version.trim().toLowerCase(Locale.ROOT);
    }

    private static String apply(String template, JsonNode payload) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder(template.length() + 32);
        int last = 0;
        while (m.find()) {
            out.append(template, last, m.start());
            String key = m.group(1);
            String value = resolve(payload, key);
            out.append(value);
            last = m.end();
        }
        out.append(template, last, template.length());
        return out.toString();
    }

    /**
     * Supports simple dotted paths, e.g. {@code ticket_id} or {@code nested.field}.
     */
    private static String resolve(JsonNode root, String path) {
        if (root == null || root.isNull() || path == null || path.isBlank()) {
            return "";
        }
        JsonNode cur = root;
        for (String part : path.split("\\.")) {
            if (cur == null || cur.isNull()) {
                return "";
            }
            cur = cur.get(part);
        }
        if (cur == null || cur.isNull()) {
            return "";
        }
        if (cur.isTextual()) {
            return cur.asText();
        }
        if (cur.isNumber() || cur.isBoolean()) {
            return cur.asText();
        }
        return cur.toString();
    }
}
