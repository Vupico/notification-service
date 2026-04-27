package com.vupico.notification.service;

import java.util.Map;

public interface TemplateService {

    /**
     * Loads a template by tenant, channel, message kind, and version; replaces {@code {{placeholders}}}
     * from {@code payload}.
     */
    RenderedTemplate render(
            String notificationType,
            String messageType,
            String version,
            Map<String, Object> payload);
}
