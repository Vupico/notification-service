package com.vupico.notification.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.vupico.notification.config.NotificationRabbitProperties;
import com.vupico.notification.dto.NotificationChannelType;
import com.vupico.notification.dto.NotificationMessage;
import com.vupico.notification.processor.NotificationProcessor;
import com.vupico.notification.processor.NotificationProcessorRegistry;
import com.vupico.notification.processor.UnsupportedNotificationProcessorException;
import com.vupico.notification.tenant.TenantConfigurationEntity;
import com.vupico.notification.tenant.TenantConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Consumes messages from the bound topic queue. Manual ack enables future retry/DLQ wiring.
 */
@Component
public class NotificationTopicConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationTopicConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationProcessorRegistry processorRegistry;
    private final TenantConfigurationService tenantConfigurationService;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationRabbitProperties rabbitProps;

    public NotificationTopicConsumer(
            ObjectMapper objectMapper,
            NotificationProcessorRegistry processorRegistry,
            TenantConfigurationService tenantConfigurationService,
            RabbitTemplate rabbitTemplate,
            NotificationRabbitProperties rabbitProps) {
        this.objectMapper = objectMapper;
        this.processorRegistry = processorRegistry;
        this.tenantConfigurationService = tenantConfigurationService;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProps = rabbitProps;
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
            long deliveryTag = raw.getMessageProperties().getDeliveryTag();
            String body = new String(raw.getBody(), StandardCharsets.UTF_8);
            try {
                NotificationMessage message = NotificationMessage.parse(body, objectMapper);
                NotificationChannelType channelType = message.getNotificationType();
                if (channelType == null || message.getPayloadVersion() == null) {
                    log.error("Missing notification_type or payload_version deliveryTag={}", deliveryTag);
                    sendToDlq(raw, "missing notification_type or payload_version");
                    channel.basicAck(deliveryTag, false);
                    continue;
                }
                String typeKey = channelType.getValue();
                String versionKey = message.getPayloadVersion();
                NotificationProcessor processor = processorRegistry.require(typeKey, versionKey);
                if (message.getPayload() == null || message.getPayload().isNull()) {
                    log.error("Missing payload deliveryTag={}", deliveryTag);
                    sendToDlq(raw, "missing payload");
                    channel.basicAck(deliveryTag, false);
                    continue;
                }
                Object typedPayload = processor.deserialize(message.getPayload());
                processor.process(message, typedPayload);
                log.info(
                        "Processed notification tenantId={} notificationId={} processor={} deliveryTag={}",
                        message.getTenantId(),
                        message.getNotificationId(),
                        processor.getClass().getSimpleName(),
                        deliveryTag);
                channel.basicAck(deliveryTag, false);
            } catch (JsonProcessingException e) {
                log.error("Invalid JSON deliveryTag={}: {}", deliveryTag, e.getMessage());
                sendToDlq(raw, "invalid json: " + e.getMessage());
                channel.basicAck(deliveryTag, false);
            } catch (UnsupportedNotificationProcessorException e) {
                log.error("Unsupported notification deliveryTag={}: {}", deliveryTag, e.getMessage());
                sendToDlq(raw, "unsupported processor: " + e.getMessage());
                channel.basicAck(deliveryTag, false);
            } catch (IllegalArgumentException e) {
                log.error("Bad message deliveryTag={}: {}", deliveryTag, e.getMessage());
                sendToDlq(raw, "bad message: " + e.getMessage());
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                log.error("Processing failed deliveryTag={}: {}", deliveryTag, e.getMessage(), e);
                if (handleRetryOrDlq(raw, body, e)) {
                    channel.basicAck(deliveryTag, false);
                } else {
                    // If we can't determine tenant config / retry policy, fall back to requeue.
                    channel.basicNack(deliveryTag, false, true);
                }
            }
        }
    }

    /**
     * @return true if message was republished to retry or DLQ (and should be acked); false if caller should requeue.
     */
    private boolean handleRetryOrDlq(Message raw, String body, Exception error) {
        String tenantId;
        try {
            NotificationMessage parsed = NotificationMessage.parse(body, objectMapper);
            tenantId = parsed.getTenantId();
        } catch (Exception parseEx) {
            sendToDlq(raw, "failed and tenant_id not parseable: " + error.getMessage());
            return true;
        }

        TenantConfigurationEntity cfg;
        try {
            cfg = tenantConfigurationService.require(tenantId);
        } catch (Exception cfgEx) {
            log.error("No tenant config tenantId={} (cannot apply retry/DLQ policy)", tenantId);
            return false;
        }

        int maxRetries = cfg.getRetryCount() != null ? Math.max(0, cfg.getRetryCount()) : 0;
        int retryIntervalSec = cfg.getRetryInterval() != null ? Math.max(0, cfg.getRetryInterval()) : 0;
        int currentRetries = getRetryCount(raw);

        if (currentRetries >= maxRetries) {
            sendToDlq(raw, "retry attempts exceeded: " + error.getClass().getSimpleName());
            return true;
        }

        int next = currentRetries + 1;
        long delayMs = (long) retryIntervalSec * 1000L;
        sendToRetry(raw, next, delayMs, error);
        return true;
    }

    private int getRetryCount(Message raw) {
        Object v = raw.getMessageProperties().getHeaders().get("x-retry-count");
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void sendToRetry(Message raw, int retryCount, long delayMs, Exception error) {
        MessageProperties props = new MessageProperties();
        props.setContentType(raw.getMessageProperties().getContentType());
        props.setContentEncoding(raw.getMessageProperties().getContentEncoding());
        props.setHeaders(raw.getMessageProperties().getHeaders());
        props.setHeader("x-retry-count", retryCount);
        props.setHeader("x-last-error", error.getClass().getSimpleName());
        if (delayMs > 0) {
            props.setExpiration(String.valueOf(delayMs));
        }

        Message retryMsg = MessageBuilder.withBody(raw.getBody()).andProperties(props).build();
        rabbitTemplate.send("", rabbitProps.getRetryQueue(), retryMsg);
        log.info("Republished to retryQueue={} retryCount={} delayMs={}", rabbitProps.getRetryQueue(), retryCount, delayMs);
    }

    private void sendToDlq(Message raw, String reason) {
        MessageProperties props = new MessageProperties();
        props.setContentType(raw.getMessageProperties().getContentType());
        props.setContentEncoding(raw.getMessageProperties().getContentEncoding());
        props.setHeaders(raw.getMessageProperties().getHeaders());
        props.setHeader("x-dlq-reason", reason);

        Message dlqMsg = MessageBuilder.withBody(raw.getBody()).andProperties(props).build();
        rabbitTemplate.send(rabbitProps.getDlqExchange(), rabbitProps.getDlqRoutingKey(), dlqMsg);
    }
}
