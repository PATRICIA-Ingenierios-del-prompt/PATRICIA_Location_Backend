package ingprompt.patricia.location.infrastructure.messaging.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // ---------- Inbound exchange (owned by Event MS) ----------
    public static final String EVENT_EXCHANGE = "event.events";

    public static final String EVENT_STARTED_ROUTING_KEY = "event.started";
    public static final String EVENT_ENDED_ROUTING_KEY = "event.ended";
    public static final String EVENT_INCIDENT_REPORTED_ROUTING_KEY = "event.incident.reported";

    public static final String EVENT_STARTED_QUEUE = "location.event.started.queue";
    public static final String EVENT_ENDED_QUEUE = "location.event.ended.queue";
    public static final String EVENT_INCIDENT_QUEUE = "location.event.incident.queue";

    private static final String[][] QUEUE_BINDINGS = {
            {EVENT_STARTED_QUEUE, EVENT_STARTED_ROUTING_KEY},
            {EVENT_ENDED_QUEUE, EVENT_ENDED_ROUTING_KEY},
            {EVENT_INCIDENT_QUEUE, EVENT_INCIDENT_REPORTED_ROUTING_KEY},
    };

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Declarables eventQueuesAndBindings() {
        TopicExchange exchange = eventExchange();
        Declarables declarables = new Declarables();
        for (String[] qb : QUEUE_BINDINGS) {
            Queue queue = new Queue(qb[0], true);
            declarables.getDeclarables().add(queue);
            declarables.getDeclarables().add(BindingBuilder.bind(queue).to(exchange).with(qb[1]));
        }
        return declarables;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
