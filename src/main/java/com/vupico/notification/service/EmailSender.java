package com.vupico.notification.service;

public interface EmailSender {

    void send(String tenantId, String to, String subject, String body);
}
