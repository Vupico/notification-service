package com.vupico.notification.consumer;

import com.rabbitmq.client.GetResponse;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.Map;

/**
 * Builds a Spring {@link Message} from {@link GetResponse} (e.g. {@code basicGet}) with delivery tag
 * for manual ack/nack.
 */
public final class AmqpGetResponseMessageFactory {

    private AmqpGetResponseMessageFactory() {}

    public static Message toMessage(GetResponse gr, String queueName) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(gr.getEnvelope().getDeliveryTag());
        props.setReceivedExchange(gr.getEnvelope().getExchange());
        props.setReceivedRoutingKey(gr.getEnvelope().getRoutingKey());
        props.setConsumerQueue(queueName);
        if (gr.getProps() != null) {
            com.rabbitmq.client.BasicProperties bp = gr.getProps();
            props.setContentType(bp.getContentType());
            props.setContentEncoding(bp.getContentEncoding());
            props.setCorrelationId(bp.getCorrelationId());
            props.setReplyTo(bp.getReplyTo());
            Map<String, Object> headers = bp.getHeaders();
            if (headers != null) {
                headers.forEach(props::setHeader);
            }
        }
        return new Message(gr.getBody(), props);
    }
}
