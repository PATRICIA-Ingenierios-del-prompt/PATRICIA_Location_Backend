package ingprompt.patricia.location.infrastructure.backplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
@ConditionalOnProperty(prefix = "backplane", name = "enabled", havingValue = "true")
public class BackplaneConfig {
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password) {
        return connectionFactory(host, port, password);
    }
    @Bean
    public LettuceConnectionFactory backplaneConnectionFactory(BackplaneProperties props) {
        return connectionFactory(props.getRedis().getHost(), props.getRedis().getPort(), props.getRedis().getPassword());
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(
            @Qualifier("redisConnectionFactory") RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public StringRedisTemplate backplaneRedisTemplate(
            @Qualifier("backplaneConnectionFactory") RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public RedisBackplanePublisher redisBackplanePublisher(
            @Qualifier("backplaneRedisTemplate") StringRedisTemplate backplaneRedisTemplate,
            ObjectMapper objectMapper,
            BackplaneProperties props) {
        return new RedisBackplanePublisher(backplaneRedisTemplate, objectMapper, props.getChannel());
    }

    @Bean
    public BackplaneStompRelay backplaneStompRelay(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        return new BackplaneStompRelay(messagingTemplate, objectMapper);
    }

    @Bean
    public RedisMessageListenerContainer backplaneListenerContainer(
            @Qualifier("backplaneConnectionFactory") RedisConnectionFactory factory,
            BackplaneStompRelay relay,
            BackplaneProperties props) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(relay, new ChannelTopic(props.getChannel()));
        return container;
    }

    private LettuceConnectionFactory connectionFactory(String host, int port, String password) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        return new LettuceConnectionFactory(config);
    }
}
