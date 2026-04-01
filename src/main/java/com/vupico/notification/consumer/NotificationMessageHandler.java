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
import com.vupico.notification.service.FailureHttpStatus;
import com.vupico.notification.tenant.TenantConfigurationEntity;
import com.vupico.notification.tenant.TenantConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Single-message processing (parse → processor → ack / DLQ / retry). Used by both the batch
 * listener and the cron drain runner.
 */
@Service
public class NotificationMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageHandler.class);

    private final ObjectMapper objectMapper;
    private final NotificationProcessorRegistry processorRegistry;
    private final TenantConfigurationService tenantConfigurationService;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationRabbitProperties rabbitProps;

    public NotificationMessageHandler(
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

    public void handle(Message raw, Channel channel) throws IOException {
        long deliveryTag = raw.getMessageProperties().getDeliveryTag();
        String body = new String(raw.getBody(), StandardCharsets.UTF_8);
        try {
            NotificationMessage message = NotificationMessage.parse(body, objectMapper);
            NotificationChannelType channelType = message.getNotificationType();
            if (channelType == null || message.getPayloadVersion() == null) {
                log.error("Missing notification_type or payload_version deliveryTag={}", deliveryTag);
                sendToDlq(raw, "missing notification_type or payload_version");
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (channelType == NotificationChannelType.EMAIL
                    && (message.getAddressList() == null || message.getAddressList().isEmpty())) {
                log.info(
                        "Skipping email notification with empty address_list tenantId={} notificationId={} deliveryTag={}",
                        message.getTenantId(),
                        message.getNotificationId(),
                        deliveryTag);
                channel.basicAck(deliveryTag, false);
                return;
            }
            String typeKey = channelType.getValue();
            String versionKey = message.getPayloadVersion();
            NotificationProcessor processor = processorRegistry.require(typeKey, versionKey);
            if (message.getPayload() == null || message.getPayload().isNull()) {
                log.error("Missing payload deliveryTag={}", deliveryTag);
                sendToDlq(raw, "missing payload");
                channel.basicAck(deliveryTag, false);
                return;
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
            handleRetryOrDlq(raw, body, e);
            channel.basicAck(deliveryTag, false);
        }
    }

    private void handleRetryOrDlq(Message raw, String body, Exception error) {
        if (isDlqReplay(raw)) {
            // DLQ replays should not enter the normal retry queue loop; if they fail again,
            // keep them in DLQ with the latest failure context.
            sendToDlq(raw, "dlq replay failed: " + error.getClass().getSimpleName(), error);
            return;
        }

        String tenantId;
        try {
            NotificationMessage parsed = NotificationMessage.parse(body, objectMapper);
            tenantId = parsed.getTenantId();
        } catch (Exception parseEx) {
            sendToDlq(raw, "failed and tenant_id not parseable: " + error.getMessage());
            return;
        }

        TenantConfigurationEntity cfg;
        try {
            cfg = tenantConfigurationService.require(tenantId);
        } catch (Exception cfgEx) {
            log.error("No tenant config tenantId={} (cannot apply retry/DLQ policy)", tenantId);
            sendToDlq(raw, "No tenant config tenantId= " + tenantId + ": " + error.getMessage());
            return;
        }

        int maxRetries = cfg.getRetryCount() != null ? Math.max(0, cfg.getRetryCount()) : 0;
        int retryIntervalSec = cfg.getRetryInterval() != null ? Math.max(0, cfg.getRetryInterval()) : 0;
        int currentRetries = getRetryCount(raw);

        if (currentRetries >= maxRetries) {
            sendToDlq(raw, "retry attempts exceeded: " + error.getClass().getSimpleName(), error);
            return;
        }

        int next = currentRetries + 1;
        long delayMs = (long) retryIntervalSec * 1000L;
        sendToRetry(raw, next, delayMs, error);
    }

    private static boolean isDlqReplay(Message raw) {
        Object v = raw.getMessageProperties().getHeaders().get("x-dlq-replay");
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        return "true".equalsIgnoreCase(String.valueOf(v));
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
        sendToDlq(raw, reason, null);
    }

    private void sendToDlq(Message raw, String reason, Throwable error) {
        MessageProperties props = new MessageProperties();
        props.setContentType(raw.getMessageProperties().getContentType());
        props.setContentEncoding(raw.getMessageProperties().getContentEncoding());
        props.setHeaders(raw.getMessageProperties().getHeaders());
        props.setHeader("x-dlq-reason", reason);
        if (error != null) {
            Integer status = FailureHttpStatus.findServerErrorCode(error);
            if (status != null) {
                props.setHeader("x-dlq-http-status", status);
            }
        }

        Message dlqMsg = MessageBuilder.withBody(raw.getBody()).andProperties(props).build();
        rabbitTemplate.send(rabbitProps.getDlqExchange(), rabbitProps.getDlqRoutingKey(), dlqMsg);
    }
}
