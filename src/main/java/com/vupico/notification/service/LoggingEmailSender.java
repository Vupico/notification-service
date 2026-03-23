package com.vupico.notification.service;

import com.vupico.notification.tenant.TenantConfigurationEntity;
import com.vupico.notification.tenant.TenantConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Logs outbound email when SES is disabled ({@code aws.ses.enabled=false}).
 */
@Service
@ConditionalOnProperty(prefix = "aws.ses", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    private final TenantConfigurationService tenantConfigurationService;
    private final TenantEmailThrottle tenantEmailThrottle;

    public LoggingEmailSender(
            TenantConfigurationService tenantConfigurationService, TenantEmailThrottle tenantEmailThrottle) {
        this.tenantConfigurationService = tenantConfigurationService;
        this.tenantEmailThrottle = tenantEmailThrottle;
    }

    @Override
    public void send(String tenantId, String to, String subject, String body) {
        tenantEmailThrottle.beforeSend(tenantId, 1);
        TenantConfigurationEntity cfg = tenantConfigurationService.require(tenantId);
        log.info(
                "EMAIL host={} tenant={} to={} subject={} bodyChars={}",
                cfg.getEmailHost(),
                tenantId,
                to,
                subject,
                body.length());
        log.debug("EMAIL body:\n{}", body);
    }

    @Override
    public void sendBatch(String tenantId, List<String> addresses, String subject, String body) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        tenantEmailThrottle.beforeSend(tenantId, addresses.size());
        TenantConfigurationEntity cfg = tenantConfigurationService.require(tenantId);
        log.info(
                "EMAIL BATCH host={} tenant={} recipients={} subject={} bodyChars={}",
                cfg.getEmailHost(),
                tenantId,
                addresses.size(),
                subject,
                body.length());
        log.debug("EMAIL body:\n{}", body);
    }
}

