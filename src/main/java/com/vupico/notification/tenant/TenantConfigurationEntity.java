package com.vupico.notification.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_configuration")
public class TenantConfigurationEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "email_host", length = 100)
    private String emailHost;

    @Column(name = "email_rate_limit")
    private Integer emailRateLimit;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "retry_interval")
    private Integer retryInterval;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmailHost() {
        return emailHost;
    }

    public void setEmailHost(String emailHost) {
        this.emailHost = emailHost;
    }

    public Integer getEmailRateLimit() {
        return emailRateLimit;
    }

    public void setEmailRateLimit(Integer emailRateLimit) {
        this.emailRateLimit = emailRateLimit;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }
}

