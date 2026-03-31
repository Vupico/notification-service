package com.vupico.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

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
    public Queue notificationWorkerRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        // Fixed retry routing key that will match the worker's "ticket.#" binding by default.
        args.put("x-dead-letter-exchange", props.getTopicExchange());
        args.put("x-dead-letter-routing-key", "ticket.retry");
        return new Queue(props.getRetryQueue(), true, false, false, args);
    }

    @Bean
    public DirectExchange notificationDlqExchange() {
        return new DirectExchange(props.getDlqExchange(), true, false);
    }

    @Bean
    public Queue notificationDlqQueue() {
        return new Queue(props.getDlqQueue(), true);
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDlqQueue, DirectExchange notificationDlqExchange) {
        return BindingBuilder.bind(notificationDlqQueue).to(notificationDlqExchange).with(props.getDlqRoutingKey());
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
