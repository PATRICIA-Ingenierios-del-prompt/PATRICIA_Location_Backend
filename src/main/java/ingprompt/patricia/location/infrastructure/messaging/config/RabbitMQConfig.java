package ingprompt.patricia.location.infrastructure.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // ---------- Outbound exchange (owned by this service) ----------
    public static final String LOCATION_EXCHANGE = "location.events";
    public static final String INCIDENT_SNAPSHOT_ROUTING_KEY = "location.incident.snapshot";

    // ---------- Inbound exchange (owned by Event MS) ----------
    public static final String EVENT_EXCHANGE = "event.events";

    public static final String EVENT_STARTED_ROUTING_KEY = "event.started";
    public static final String EVENT_ENDED_ROUTING_KEY = "event.ended";
    public static final String EVENT_INCIDENT_REPORTED_ROUTING_KEY = "event.incident.reported";

    public static final String EVENT_STARTED_QUEUE = "location.event.started.queue";
    public static final String EVENT_ENDED_QUEUE = "location.event.ended.queue";
    public static final String EVENT_INCIDENT_QUEUE = "location.event.incident.queue";

    @Bean
    public TopicExchange locationExchange() {
        return new TopicExchange(LOCATION_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue eventStartedQueue() {
        return new Queue(EVENT_STARTED_QUEUE, true);
    }

    @Bean
    public Queue eventEndedQueue() {
        return new Queue(EVENT_ENDED_QUEUE, true);
    }

    @Bean
    public Queue eventIncidentQueue() {
        return new Queue(EVENT_INCIDENT_QUEUE, true);
    }

    @Bean
    public Binding eventStartedBinding() {
        return BindingBuilder.bind(eventStartedQueue()).to(eventExchange()).with(EVENT_STARTED_ROUTING_KEY);
    }

    @Bean
    public Binding eventEndedBinding() {
        return BindingBuilder.bind(eventEndedQueue()).to(eventExchange()).with(EVENT_ENDED_ROUTING_KEY);
    }

    @Bean
    public Binding eventIncidentBinding() {
        return BindingBuilder.bind(eventIncidentQueue()).to(eventExchange()).with(EVENT_INCIDENT_REPORTED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // Event MS publishes with its own __TypeId__ (its package); deserialize into our local types.
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
