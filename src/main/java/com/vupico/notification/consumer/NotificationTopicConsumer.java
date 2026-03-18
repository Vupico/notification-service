package com.vupico.notification.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.vupico.notification.dto.NotificationChannelType;
import com.vupico.notification.dto.NotificationMessage;
import com.vupico.notification.processor.NotificationProcessor;
import com.vupico.notification.processor.NotificationProcessorRegistry;
import com.vupico.notification.processor.UnsupportedNotificationProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumes messages from the bound topic queue. Manual ack enables future retry/DLQ wiring.
 */
@Component
public class NotificationTopicConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationTopicConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationProcessorRegistry processorRegistry;

    public NotificationTopicConsumer(
            ObjectMapper objectMapper, NotificationProcessorRegistry processorRegistry) {
        this.objectMapper = objectMapper;
        this.processorRegistry = processorRegistry;
    }

    @RabbitListener(
            id = "notificationTopicListener",
            queues = "${notification.rabbit.queue}",
            ackMode = "MANUAL",
            concurrency = "${notification.rabbit.listener.concurrency:1-3}")
    public void onMessage(
            @Payload String body,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey)
            throws IOException {
        try {
            log.debug("Received routingKey={} payload={}", routingKey, body);
            NotificationMessage message = NotificationMessage.parse(body, objectMapper);
            NotificationChannelType channelType = message.getNotificationType();
            if (channelType == null || message.getPayloadVersion() == null) {
                log.error("Missing notification_type or payload_version routingKey={}", routingKey);
                channel.basicNack(deliveryTag, false, false);
                return;
            }
            String typeKey = channelType.getValue();
            String versionKey = message.getPayloadVersion();
            NotificationProcessor processor = processorRegistry.require(typeKey, versionKey);
            if (message.getPayload() == null || message.getPayload().isNull()) {
                log.error("Missing payload routingKey={}", routingKey);
                channel.basicNack(deliveryTag, false, false);
                return;
            }
            Object typedPayload = processor.deserialize(message.getPayload());
            processor.process(message, typedPayload);
            log.info(
                    "Processed notification tenantId={} notificationId={} processor={} routingKey={}",
                    message.getTenantId(),
                    message.getNotificationId(),
                    processor.getClass().getSimpleName(),
                    routingKey);
            channel.basicAck(deliveryTag, false);
        } catch (JsonProcessingException e) {
            log.error("Invalid JSON routingKey={}: {}", routingKey, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (UnsupportedNotificationProcessorException e) {
            log.error("Unsupported notification routingKey={}: {}", routingKey, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (IllegalArgumentException e) {
            log.error("Bad message routingKey={}: {}", routingKey, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("Processing failed routingKey={}: {}", routingKey, e.getMessage(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
