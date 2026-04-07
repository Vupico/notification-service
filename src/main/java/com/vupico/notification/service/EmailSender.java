package com.vupico.notification.service;

import java.util.List;

public interface EmailSender {

    default void send(String tenantId, String to, String subject, String body) {
        sendBatch(tenantId, List.of(to), subject, body, null);
    }

    /**
     * Send one logical notification to many recipients. Implementations should prefer provider
     * batch APIs (e.g. SES {@code SendBulkEmail}) when possible.
     */
    default void sendBatch(String tenantId, List<String> addresses, String subject, String body) {
        sendBatch(tenantId, addresses, subject, body, null);
    }

    /**
     * @param fromDisplayName optional personal name for the {@code From} header (e.g. {@code Name (username)});
     *                        the mailbox address still comes from {@code spring.mail.from}.
     */
    void sendBatch(
            String tenantId, List<String> addresses, String subject, String body, String fromDisplayName);
}
