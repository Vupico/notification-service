package com.vupico.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logs outbound email until SES (or another provider) is integrated.
 */
@Service
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String tenantId, String to, String subject, String body) {
        log.info("EMAIL tenant={} to={} subject={} bodyChars={}", tenantId, to, subject, body.length());
        log.debug("EMAIL body:\n{}", body);
    }
}
