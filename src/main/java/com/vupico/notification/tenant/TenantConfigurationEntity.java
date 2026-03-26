package com.vupico.notification.tenant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tenant_configuration")
public class TenantConfigurationEntity {

    @Id
    private String tenantId;

    private String emailHost;

    private Integer emailRateLimit;

    private Integer retryCount;

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

