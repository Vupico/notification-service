package com.vupico.notification.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.vupico.notification.config.NotificationRabbitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * After the worker queue is processed, drains the DLQ and replays messages that were dead-lettered
 * due to an HTTP 5xx failure ({@code x-dlq-http-status} header). Other DLQ messages are published
 * back to the DLQ and acked so order is preserved without losing poison messages.
 */
@Service
public class NotificationDlqReplayService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDlqReplayService.class);

    static final String DLQ_HTTP_STATUS_HEADER = "x-dlq-http-status";

    private final NotificationRabbitProperties rabbitProps;
    private final NotificationMessageHandler messageHandler;

    private final Object dlqDrainLock = new Object();

    public NotificationDlqReplayService(
            NotificationRabbitProperties rabbitProps, NotificationMessageHandler messageHandler) {
        this.rabbitProps = rabbitProps;
        this.messageHandler = messageHandler;
    }

    /**
     * Pulls the current contents of the DLQ into a batch, then either replays 5xx-tagged messages
     * through {@link NotificationMessageHandler} or republishes other messages to the tail of the DLQ.
     * Serialized so concurrent listener threads do not race on the same DLQ.
     */
    public void drainDlqFor5xxReplay(Channel channel) throws IOException {
        synchronized (dlqDrainLock) {
            drainDlqFor5xxReplayUnsynchronized(channel);
        }
    }

    private void drainDlqFor5xxReplayUnsynchronized(Channel channel) throws IOException {
        String dlq = rabbitProps.getDlqQueue();
        log.info("Draining DLQ queue={} for HTTP 5xx replay candidates.", dlq);
        List<GetResponse> batch = new ArrayList<>();
        while (true) {
            GetResponse gr = channel.basicGet(dlq, false);
            if (gr == null) {
                break;
            }
            batch.add(gr);
        }
        int replayed = 0;
        int republished = 0;
        for (GetResponse gr : batch) {
            Message msg = AmqpGetResponseMessageFactory.toMessage(gr, dlq);
            long deliveryTag = gr.getEnvelope().getDeliveryTag();
            if (!isHttp5xxDlqCandidate(msg)) {
                com.rabbitmq.client.AMQP.BasicProperties props =
                        gr.getProps() != null
                                ? gr.getProps()
                                : new com.rabbitmq.client.AMQP.BasicProperties.Builder().build();
                channel.basicPublish(
                        rabbitProps.getDlqExchange(), rabbitProps.getDlqRoutingKey(), props, gr.getBody());
                channel.basicAck(deliveryTag, false);
                republished++;
                continue;
            }
            Message replay = prepareDlq5xxReplay(msg);
            messageHandler.handle(replay, channel);
            replayed++;
        }
        log.info(
                "DLQ batch finished queue={} pulled={} replayAttempts={} republishedNon5xx={}",
                dlq,
                batch.size(),
                replayed,
                republished);
    }

    static boolean isHttp5xxDlqCandidate(Message msg) {
        Object v = msg.getMessageProperties().getHeaders().get(DLQ_HTTP_STATUS_HEADER);
        if (v == null) {
            return false;
        }
        try {
            int code = headerToInt(v);
            return code >= 500 && code <= 599;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int headerToInt(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }

    /**
     * Clears DLQ-only headers and resets retry budget for a fresh pass through the worker policy.
     */
    static Message prepareDlq5xxReplay(Message raw) {
        return MessageBuilder.fromMessage(raw)
                .removeHeader("x-dlq-reason")
                .removeHeader(DLQ_HTTP_STATUS_HEADER)
                .setHeader("x-retry-count", 0)
                .build();
    }
}
