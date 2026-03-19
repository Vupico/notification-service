package com.vupico.notification.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantConfigurationRepository extends JpaRepository<TenantConfigurationEntity, String> {}

