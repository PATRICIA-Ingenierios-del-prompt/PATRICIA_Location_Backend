package ingprompt.patricia.location.infrastructure.backplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

@Slf4j
public class BackplaneStompRelay implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public BackplaneStompRelay(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            BackplaneEnvelope envelope = objectMapper.readValue(json, BackplaneEnvelope.class);
            if (envelope.destination() == null || envelope.payload() == null) {
                log.warn("Backplane message dropped: missing destination or payload");
                return;
            }
            messagingTemplate.convertAndSend(envelope.destination(), envelope.payload());
        } catch (Exception ex) {
            // Nunca tumbar el listener container: un mensaje malformado se descarta.
            log.warn("Failed to relay backplane message: {}", ex.getMessage());
        }
    }
}
