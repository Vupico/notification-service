package com.vupico.notification.service;

public interface TemplateService {

    String render(String tenantId, String messageType, Object payload);
}
