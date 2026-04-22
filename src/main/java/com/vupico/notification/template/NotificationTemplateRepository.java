package com.vupico.notification.template;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplateEntity, String> {

    Optional<NotificationTemplateEntity> findByNotificationTypeAndMessageTypeAndVersion(String notificationType, String messageType, String version);
}
