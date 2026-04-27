package com.vupico.notification.monitoring;

import org.springframework.stereotype.Component;

@Component
public class MonitoringClientPropertiesHolder {

    private static volatile MonitoringClientProperties properties;

    public MonitoringClientPropertiesHolder(MonitoringClientProperties properties) {
        MonitoringClientPropertiesHolder.properties = properties;
    }

    public static String integrationOrDefault(String defaultValue) {
        MonitoringClientProperties current = properties;
        if (current == null || current.getIntegration() == null || current.getIntegration().isBlank()) {
            return defaultValue;
        }
        return current.getIntegration();
    }
}
