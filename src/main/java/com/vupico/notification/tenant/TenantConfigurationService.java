package com.vupico.notification.tenant;

import org.springframework.stereotype.Service;

@Service
public class TenantConfigurationService {

    private final TenantConfigurationRepository repository;

    public TenantConfigurationService(TenantConfigurationRepository repository) {
        this.repository = repository;
    }

    public TenantConfigurationEntity require(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenant_id is required");
        }
        return repository
                .findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("No tenant_configuration for tenant_id=" + tenantId));
    }
}

