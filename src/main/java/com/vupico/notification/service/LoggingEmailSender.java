package com.vupico.notification.service;

import com.vupico.notification.tenant.TenantConfigurationEntity;
import com.vupico.notification.tenant.TenantConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Logs outbound email until SES (or another provider) is integrated.
 */
@Service
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    private final TenantConfigurationService tenantConfigurationService;
    private final ConcurrentMap<String, RateLimitState> rateLimits = new ConcurrentHashMap<>();

    public LoggingEmailSender(TenantConfigurationService tenantConfigurationService) {
        this.tenantConfigurationService = tenantConfigurationService;
    }

    @Override
    public void send(String tenantId, String to, String subject, String body) {
        TenantConfigurationEntity cfg = tenantConfigurationService.require(tenantId);
        throttle(tenantId, cfg.getEmailRateLimit());

        log.info(
                "EMAIL host={} tenant={} to={} subject={} bodyChars={}",
                cfg.getEmailHost(),
                tenantId,
                to,
                subject,
                body.length());
        log.debug("EMAIL body:\n{}", body);
    }

    private void throttle(String tenantId, Integer perSecond) {
        if (perSecond == null || perSecond <= 0) {
            return;
        }
        long intervalNanos = 1_000_000_000L / Math.max(1, perSecond);
        RateLimitState state = rateLimits.computeIfAbsent(tenantId, t -> new RateLimitState());
        long sleepNanos;
        synchronized (state) {
            long now = System.nanoTime();
            long next = Math.max(state.nextAllowedNanos, now);
            state.nextAllowedNanos = next + intervalNanos;
            sleepNanos = next - now;
        }
        if (sleepNanos > 0) {
            try {
                Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class RateLimitState {
        private long nextAllowedNanos;
    }
}

