package com.vupico.notification.consumer;

import com.rabbitmq.client.GetResponse;
import com.vupico.notification.config.NotificationRabbitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kubernetes CronJob mode: drain the worker queue until empty, then exit the JVM with code 0.
 * Set {@code notification.rabbit.cron-mode=true} (or env {@code NOTIFICATION_CRON_MODE=true}).
 */
@Component
@Order(Integer.MAX_VALUE)
@ConditionalOnProperty(prefix = "notification.rabbit", name = "cron-mode", havingValue = "true")
public class QueueDrainCronRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QueueDrainCronRunner.class);

    private final RabbitTemplate rabbitTemplate;
    private final NotificationMessageHandler messageHandler;
    private final NotificationRabbitProperties rabbitProps;
    private final ApplicationContext applicationContext;

    public QueueDrainCronRunner(
            RabbitTemplate rabbitTemplate,
            NotificationMessageHandler messageHandler,
            NotificationRabbitProperties rabbitProps,
            ApplicationContext applicationContext) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageHandler = messageHandler;
        this.rabbitProps = rabbitProps;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        String queue = rabbitProps.getQueue();
        log.info("Cron mode: draining queue={} until empty, then exiting.", queue);

        rabbitTemplate.execute(
                (ChannelCallback<Void>)
                        channel -> {
                            while (true) {
                                GetResponse gr = channel.basicGet(queue, false);
                                if (gr == null) {
                                    log.info("Queue {} is empty; shutdown.", queue);
                                    break;
                                }
                                Message msg = toMessage(gr, queue);
                                messageHandler.handle(msg, channel);
                            }
                            return null;
                        });

        int code = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(code);
    }

    private static Message toMessage(GetResponse gr, String queueName) {
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
