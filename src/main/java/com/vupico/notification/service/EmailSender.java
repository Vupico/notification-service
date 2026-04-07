package com.vupico.notification.service;

import java.util.List;

public interface EmailSender {

    default void send(String tenantId, String to, String subject, String body) {
        sendBatch(tenantId, List.of(to), subject, body, null, false);
    }

    /**
     * Send one logical notification to many recipients. Implementations should prefer provider
     * batch APIs (e.g. SES {@code SendBulkEmail}) when possible.
     */
    default void sendBatch(String tenantId, List<String> addresses, String subject, String body) {
        sendBatch(tenantId, addresses, subject, body, null, false);
    }

    /**
     * @param fromDisplayName optional personal name for the {@code From} header (e.g. {@code Name (username)});
     *                        the mailbox address still comes from {@code spring.mail.from}.
     */
    default void sendBatch(
            String tenantId, List<String> addresses, String subject, String body, String fromDisplayName) {
        sendBatch(tenantId, addresses, subject, body, fromDisplayName, false);
    }

    /**
     * @param highImportance when true, sets headers so clients can show the message as high importance
     *                       (e.g. {@code X-Priority}, {@code Importance}).
     */
    void sendBatch(
            String tenantId,
            List<String> addresses,
            String subject,
            String body,
            String fromDisplayName,
            boolean highImportance);
}
