package com.vupico.notification.service;

public interface TemplateService {

    RenderedTemplate renderEmail(String tenantId, String templateName, Object payload);
}
