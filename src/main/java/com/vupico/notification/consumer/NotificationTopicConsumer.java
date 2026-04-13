package com.vupico.notification.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Long-running listener (default). Disabled when {@code notification.rabbit.cron-mode=true}
 * (use {@link QueueDrainCronRunner} for Kubernetes CronJob).
 */
@Component
@ConditionalOnProperty(prefix = "notification.rabbit", name = "cron-mode", havingValue = "false", matchIfMissing = true)
public class NotificationTopicConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationTopicConsumer.class);

    private final NotificationMessageHandler messageHandler;
    private final NotificationDlqReplayService dlqReplayService;

    public NotificationTopicConsumer(
            NotificationMessageHandler messageHandler, NotificationDlqReplayService dlqReplayService) {
        this.messageHandler = messageHandler;
        this.dlqReplayService = dlqReplayService;
    }

    @RabbitListener(
            id = "notificationTopicListener",
            queues = "${notification.rabbit.queue}",
            ackMode = "MANUAL",
            concurrency = "${notification.rabbit.listener.concurrency:1-3}")
    public void onMessages(
            List<Message> messages,
            Channel channel,
            @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey)
            throws IOException {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        log.debug("Received batch size={} routingKey={}", messages.size(), routingKey);

        for (Message raw : messages) {
            messageHandler.handle(raw, channel);
        }
        dlqReplayService.drainDlqFor5xxReplay(channel);
    }
}
