package com.vupico.notification.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitoringComponent {
    /**
     * Key for {@code monitoring.client.domains.*} and {@code monitoring.client.services.*}.
     */
    String value();
}
