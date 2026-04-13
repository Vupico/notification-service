package com.vupico.notification.tenant;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TenantConfigurationRepository extends MongoRepository<TenantConfigurationEntity, String> {

    Optional<TenantConfigurationEntity> findByTenantId(String tenantId);
}

