package ingprompt.patricia.location.infrastructure.messaging.publisher;

import ingprompt.patricia.location.application.port.out.EncryptionPort;
import ingprompt.patricia.location.application.port.out.LocationEventPublisherOut;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.infrastructure.messaging.config.RabbitMQConfig;
import ingprompt.patricia.location.infrastructure.messaging.event.IncidentSnapshotCapturedEvent;
import ingprompt.patricia.location.infrastructure.messaging.event.IncidentSnapshotCapturedEvent.EncryptedPoint;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
public class LocationEventPublisherAdapter implements LocationEventPublisherOut {

    private final RabbitTemplate rabbitTemplate;
    private final EncryptionPort encryption;

    @Override
    public void publishIncidentSnapshot(UUID eventId, UUID reportId, List<LiveLocation> snapshot) {
        List<EncryptedPoint> points = snapshot.stream()
                .map(live -> new EncryptedPoint(
                        live.userId(),
                        encryption.encrypt(Double.toString(live.point().latitude())),
                        encryption.encrypt(Double.toString(live.point().longitude())),
                        live.recordedAt()))
                .toList();

        IncidentSnapshotCapturedEvent payload =
                new IncidentSnapshotCapturedEvent(eventId, reportId, Instant.now(), points);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LOCATION_EXCHANGE,
                RabbitMQConfig.INCIDENT_SNAPSHOT_ROUTING_KEY,
                payload);
    }
}
