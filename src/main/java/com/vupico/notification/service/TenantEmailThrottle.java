package com.vupico.notification.service;

import com.vupico.notification.tenant.TenantConfigurationEntity;
import com.vupico.notification.tenant.TenantConfigurationService;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-tenant spacing for outbound email based on {@code email_rate_limit} (emails per second).
 */
@Component
public class TenantEmailThrottle {

    private final TenantConfigurationService tenantConfigurationService;
    private final ConcurrentMap<String, RateLimitState> rateLimits = new ConcurrentHashMap<>();

    public TenantEmailThrottle(TenantConfigurationService tenantConfigurationService) {
        this.tenantConfigurationService = tenantConfigurationService;
    }

    public void beforeSend(String tenantId, int emailCount) {
        if (emailCount <= 0) {
            return;
        }
        TenantConfigurationEntity cfg = tenantConfigurationService.require(tenantId);
        Integer perSecond = cfg.getEmailRateLimit();
        if (perSecond == null || perSecond <= 0) {
            return;
        }
        long intervalNanos = 1_000_000_000L / Math.max(1, perSecond);
        long totalWaitNanos = intervalNanos * emailCount;
        RateLimitState state = rateLimits.computeIfAbsent(tenantId, t -> new RateLimitState());
        long sleepNanos;
        synchronized (state) {
            long now = System.nanoTime();
            long next = Math.max(state.nextAllowedNanos, now);
            state.nextAllowedNanos = next + totalWaitNanos;
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
