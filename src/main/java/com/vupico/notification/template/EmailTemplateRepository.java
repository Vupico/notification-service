package com.vupico.notification.template;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EmailTemplateRepository extends MongoRepository<EmailTemplateEntity, String> {

    Optional<EmailTemplateEntity> findByTenantIdAndTemplateName(String tenantId, String templateName);

    Optional<EmailTemplateEntity> findByTemplateName(String templateName);
}

