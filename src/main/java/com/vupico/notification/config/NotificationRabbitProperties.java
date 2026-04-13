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
    private String topicExchange;

    @NotBlank
    private String queue;

    @NotBlank
    private String routingKeyPattern;

    @NotBlank
    private String retryQueue;

    @NotBlank
    private String dlqExchange;

    @NotBlank
    private String dlqQueue;

    @NotBlank
    private String dlqRoutingKey;

    /**
     * When true, do not run a long-lived listener; drain {@link #queue} once and exit (for CronJob).
     */
    private boolean cronMode;

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

    public boolean isCronMode() {
        return cronMode;
    }

    public void setCronMode(boolean cronMode) {
        this.cronMode = cronMode;
    }
}
