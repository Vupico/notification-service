package com.vupico.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares topic exchange, worker queue, and binding. Idempotent on broker restart.
 */
@Configuration
@EnableConfigurationProperties(NotificationRabbitProperties.class)
public class RabbitMqTopicConfig {

    private final NotificationRabbitProperties props;

    public RabbitMqTopicConfig(NotificationRabbitProperties props) {
        this.props = props;
    }

    @Bean
    public TopicExchange notificationTopicExchange() {
        return new TopicExchange(props.getTopicExchange(), true, false);
    }

    @Bean
    public Queue notificationWorkerQueue() {
        return new Queue(props.getQueue(), true);
    }

    @Bean
    public Binding notificationWorkerBinding(
            Queue notificationWorkerQueue,
            TopicExchange notificationTopicExchange) {
        return BindingBuilder.bind(notificationWorkerQueue)
                .to(notificationTopicExchange)
                .with(props.getRoutingKeyPattern());
    }
}
