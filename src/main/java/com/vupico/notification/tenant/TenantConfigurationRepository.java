package com.vupico.notification.tenant;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TenantConfigurationRepository extends MongoRepository<TenantConfigurationEntity, String> {}

