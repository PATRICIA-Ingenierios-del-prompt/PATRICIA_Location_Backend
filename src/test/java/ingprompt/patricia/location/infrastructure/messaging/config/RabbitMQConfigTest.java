package ingprompt.patricia.location.infrastructure.messaging.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void exchangeIsDurableTopicExchange() {
        TopicExchange exchange = config.eventExchange();

        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.EVENT_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void queuesAreDurableAndNamed() {
        Queue started = config.eventStartedQueue();
        Queue ended = config.eventEndedQueue();
        Queue incident = config.eventIncidentQueue();

        assertThat(started.getName()).isEqualTo(RabbitMQConfig.EVENT_STARTED_QUEUE);
        assertThat(ended.getName()).isEqualTo(RabbitMQConfig.EVENT_ENDED_QUEUE);
        assertThat(incident.getName()).isEqualTo(RabbitMQConfig.EVENT_INCIDENT_QUEUE);
        assertThat(started.isDurable()).isTrue();
        assertThat(ended.isDurable()).isTrue();
        assertThat(incident.isDurable()).isTrue();
    }

    @Test
    void bindingsUseExpectedRoutingKeys() {
        Binding started = config.eventStartedBinding();
        Binding ended = config.eventEndedBinding();
        Binding incident = config.eventIncidentBinding();

        assertThat(started.getRoutingKey()).isEqualTo(RabbitMQConfig.EVENT_STARTED_ROUTING_KEY);
        assertThat(ended.getRoutingKey()).isEqualTo(RabbitMQConfig.EVENT_ENDED_ROUTING_KEY);
        assertThat(incident.getRoutingKey()).isEqualTo(RabbitMQConfig.EVENT_INCIDENT_REPORTED_ROUTING_KEY);
        assertThat(started.getDestination()).isEqualTo(RabbitMQConfig.EVENT_STARTED_QUEUE);
    }

    @Test
    void jsonMessageConverterIsConfigured() {
        MessageConverter converter = config.jsonMessageConverter();

        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
