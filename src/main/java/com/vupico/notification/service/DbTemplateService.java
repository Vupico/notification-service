package com.vupico.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupico.notification.template.EmailTemplateEntity;
import com.vupico.notification.template.EmailTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DbTemplateService implements TemplateService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\.\\-]+)\\s*}}");

    private final EmailTemplateRepository emailTemplateRepository;
    private final ObjectMapper objectMapper;

    public DbTemplateService(EmailTemplateRepository emailTemplateRepository, ObjectMapper objectMapper) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public RenderedTemplate renderEmail(String tenantId, String templateName, Object payload) {
        EmailTemplateEntity template = emailTemplateRepository
                .findByTemplateName(templateName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No email template for template_name=%s".formatted(templateName)));

        JsonNode payloadNode = objectMapper.valueToTree(payload);
        String subject = apply(template.getSubject(), payloadNode);
        String body = apply(template.getBody(), payloadNode);
        return new RenderedTemplate(subject, body);
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

