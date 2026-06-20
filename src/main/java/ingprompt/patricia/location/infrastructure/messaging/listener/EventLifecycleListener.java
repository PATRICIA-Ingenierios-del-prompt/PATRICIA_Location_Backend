package ingprompt.patricia.location.infrastructure.messaging.listener;

import ingprompt.patricia.location.application.port.in.TrackingLifecycleCase;
import ingprompt.patricia.location.infrastructure.messaging.config.RabbitMQConfig;
import ingprompt.patricia.location.infrastructure.messaging.event.BaseEvent;
import ingprompt.patricia.location.infrastructure.messaging.event.EventEndedEvent;
import ingprompt.patricia.location.infrastructure.messaging.event.EventStartedEvent;
import ingprompt.patricia.location.infrastructure.messaging.event.IncidentReportedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class EventLifecycleListener {
    private final TrackingLifecycleCase lifecycleCase;

    @RabbitListener(queues = RabbitMQConfig.EVENT_STARTED_QUEUE)
    public void onEventStarted(EventStartedEvent event) {
        handle("event.started", event,
                () -> lifecycleCase.startTracking(event.getEventId(), event.getParticipants()));
    }

    @RabbitListener(queues = RabbitMQConfig.EVENT_ENDED_QUEUE)
    public void onEventEnded(EventEndedEvent event) {
        handle("event.ended", event,
                () -> lifecycleCase.stopTracking(event.getEventId()));
    }

    @RabbitListener(queues = RabbitMQConfig.EVENT_INCIDENT_QUEUE)
    public void onIncidentReported(IncidentReportedEvent event) {
        handle("event.incident.reported", event,
                () -> lifecycleCase.captureIncidentSnapshot(event.getEventId(), event.getReportId()));
    }

    private void handle(String label, BaseEvent event, Runnable action) {
        log.info("{} received for event {}", label, event.getEventId());
        action.run();
    }
}
