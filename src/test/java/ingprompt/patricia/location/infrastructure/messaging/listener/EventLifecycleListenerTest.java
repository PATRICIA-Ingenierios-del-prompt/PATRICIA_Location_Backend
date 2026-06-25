package ingprompt.patricia.location.infrastructure.messaging.listener;

import ingprompt.patricia.location.application.port.in.TrackingLifecycleCase;
import ingprompt.patricia.location.infrastructure.messaging.event.EventEndedEvent;
import ingprompt.patricia.location.infrastructure.messaging.event.EventStartedEvent;
import ingprompt.patricia.location.infrastructure.messaging.event.IncidentReportedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventLifecycleListenerTest {

    @Mock
    private TrackingLifecycleCase lifecycleCase;
    @InjectMocks
    private EventLifecycleListener listener;

    private final UUID eventId = UUID.randomUUID();

    @Test
    void onEventStarted_startsTracking() {
        Set<UUID> participants = Set.of(UUID.randomUUID());
        listener.onEventStarted(new EventStartedEvent(eventId, participants));
        verify(lifecycleCase).startTracking(eventId, participants);
    }

    @Test
    void onEventEnded_stopsTracking() {
        listener.onEventEnded(new EventEndedEvent(eventId));
        verify(lifecycleCase).stopTracking(eventId);
    }

    @Test
    void onIncidentReported_capturesSnapshot() {
        UUID reportId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        listener.onIncidentReported(new IncidentReportedEvent(eventId, reportId, reporterId));
        verify(lifecycleCase).captureIncidentSnapshot(eventId, reportId, reporterId);
    }
}
