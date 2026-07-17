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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventLifecycleListenerTest {

    @Mock
    private TrackingLifecycleCase lifecycleCase;

    @InjectMocks
    private EventLifecycleListener listener;

    @Test
    void onEventStarted_delegatesToLifecycleCase() {
        UUID eventId = UUID.randomUUID();
        Set<UUID> participants = Set.of(UUID.randomUUID(), UUID.randomUUID());

        listener.onEventStarted(new EventStartedEvent(eventId, participants));

        verify(lifecycleCase).startTracking(eventId, participants);
    }

    @Test
    void onEventStarted_rethrowsWhenLifecycleCaseFails() {
        UUID eventId = UUID.randomUUID();
        EventStartedEvent event = new EventStartedEvent(eventId, Set.of());
        doThrow(new RuntimeException("boom")).when(lifecycleCase).startTracking(eventId, Set.of());

        assertThatThrownBy(() -> listener.onEventStarted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
    }

    @Test
    void onEventEnded_delegatesToLifecycleCase() {
        UUID eventId = UUID.randomUUID();

        listener.onEventEnded(new EventEndedEvent(eventId));

        verify(lifecycleCase).stopTracking(eventId);
    }

    @Test
    void onEventEnded_rethrowsWhenLifecycleCaseFails() {
        UUID eventId = UUID.randomUUID();
        EventEndedEvent event = new EventEndedEvent(eventId);
        doThrow(new IllegalStateException("down")).when(lifecycleCase).stopTracking(eventId);

        assertThatThrownBy(() -> listener.onEventEnded(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("down");
    }

    @Test
    void onIncidentReported_delegatesToLifecycleCase() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        listener.onIncidentReported(new IncidentReportedEvent(eventId, reportId, UUID.randomUUID()));

        verify(lifecycleCase).captureIncidentSnapshot(eventId, reportId);
    }

    @Test
    void onIncidentReported_rethrowsWhenLifecycleCaseFails() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        IncidentReportedEvent event = new IncidentReportedEvent(eventId, reportId, UUID.randomUUID());
        doThrow(new RuntimeException("persist failed")).when(lifecycleCase).captureIncidentSnapshot(eventId, reportId);

        assertThatThrownBy(() -> listener.onIncidentReported(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("persist failed");
        verify(lifecycleCase).captureIncidentSnapshot(eventId, reportId);
    }
}
