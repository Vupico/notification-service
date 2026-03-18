package com.vupico.notification.service;

import org.springframework.stereotype.Service;

/**
 * Placeholder renderer until multi-tenant templates are wired.
 */
@Service
public class SimpleTemplateService implements TemplateService {

    @Override
    public String render(String tenantId, String messageType, Object payload) {
        return String.format(
                "[tenant=%s messageType=%s]\n%s", tenantId, messageType, String.valueOf(payload));
    }
}
