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

    @Test
    void onEventStartedDelegatesToStartTracking() {
        UUID eventId = UUID.randomUUID();
        Set<UUID> participants = Set.of(UUID.randomUUID(), UUID.randomUUID());
        EventStartedEvent event = new EventStartedEvent(eventId, participants);

        listener.onEventStarted(event);

        verify(lifecycleCase).startTracking(eventId, participants);
    }

    @Test
    void onEventEndedDelegatesToStopTracking() {
        UUID eventId = UUID.randomUUID();
        EventEndedEvent event = new EventEndedEvent(eventId);

        listener.onEventEnded(event);

        verify(lifecycleCase).stopTracking(eventId);
    }

    @Test
    void onIncidentReportedDelegatesToCaptureSnapshot() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        IncidentReportedEvent event = new IncidentReportedEvent(eventId, reportId, reporterId);

        listener.onIncidentReported(event);

        verify(lifecycleCase).captureIncidentSnapshot(eventId, reportId);
    }
}
