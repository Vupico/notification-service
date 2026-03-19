package com.vupico.notification.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RabbitMQ topic topology for the worker. Align exchange / routing keys with the ticket system publisher.
 */
@Validated
@ConfigurationProperties(prefix = "notification.rabbit")
public class NotificationRabbitProperties {

    @NotBlank
    private String topicExchange = "notifications.topic";

    @NotBlank
    private String queue = "notifications.worker.queue";

    @NotBlank
    private String routingKeyPattern = "ticket.#";

    @NotBlank
    private String retryQueue = "notifications.worker.retry.queue";

    @NotBlank
    private String dlqExchange = "notifications.worker.dlx";

    @NotBlank
    private String dlqQueue = "notifications.worker.dlq";

    @NotBlank
    private String dlqRoutingKey = "notifications.worker.dlq";

    public String getTopicExchange() {
        return topicExchange;
    }

    public void setTopicExchange(String topicExchange) {
        this.topicExchange = topicExchange;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getRoutingKeyPattern() {
        return routingKeyPattern;
    }

    public void setRoutingKeyPattern(String routingKeyPattern) {
        this.routingKeyPattern = routingKeyPattern;
    }

    public String getRetryQueue() {
        return retryQueue;
    }

    public void setRetryQueue(String retryQueue) {
        this.retryQueue = retryQueue;
    }

    public String getDlqExchange() {
        return dlqExchange;
    }

    public void setDlqExchange(String dlqExchange) {
        this.dlqExchange = dlqExchange;
    }

    public String getDlqQueue() {
        return dlqQueue;
    }

    public void setDlqQueue(String dlqQueue) {
        this.dlqQueue = dlqQueue;
    }

    public String getDlqRoutingKey() {
        return dlqRoutingKey;
    }

    public void setDlqRoutingKey(String dlqRoutingKey) {
        this.dlqRoutingKey = dlqRoutingKey;
    }
}
