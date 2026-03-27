package com.vupico.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AwsSesProperties.class, NotificationRabbitProperties.class})
public class PropertiesConfig {}

