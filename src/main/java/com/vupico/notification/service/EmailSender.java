package com.vupico.notification.service;

import java.util.List;

public interface EmailSender {

    void send(String tenantId, String to, String subject, String body);

    /**
     * Send one logical notification to many recipients. Implementations should prefer provider
     * batch APIs (e.g. SES {@code SendBulkEmail}) when possible.
     */
    default void sendBatch(String tenantId, List<String> addresses, String subject, String body) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        for (String to : addresses) {
            send(tenantId, to, subject, body);
        }
    }
}
