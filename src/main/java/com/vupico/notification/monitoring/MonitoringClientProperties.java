package com.vupico.notification.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "monitoring.client")
public class MonitoringClientProperties {

    private String integration = "notification-service";
    private Map<String, String> domains = new HashMap<>();
    private Map<String, String> services = new HashMap<>();

    public String getIntegration() {
        return integration;
    }

    public void setIntegration(String integration) {
        this.integration = integration;
    }

    public Map<String, String> getDomains() {
        return domains;
    }

    public void setDomains(Map<String, String> domains) {
        this.domains = domains;
    }

    public Map<String, String> getServices() {
        return services;
    }

    public void setServices(Map<String, String> services) {
        this.services = services;
    }

    public String domain(String key, String defaultValue) {
        String resolved = domains.get(key);
        return (resolved == null || resolved.isBlank()) ? defaultValue : resolved;
    }

    public String service(String key, String defaultValue) {
        String resolved = services.get(key);
        return (resolved == null || resolved.isBlank()) ? defaultValue : resolved;
    }
}
