package com.vupico.notification;

import com.vupico.monitoring.config.MonitoringAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@Import(MonitoringAutoConfiguration.class)
@EnableMongoRepositories(basePackages = {
        "com.vupico.notification",
        "com.vupico.monitoring.repository"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
