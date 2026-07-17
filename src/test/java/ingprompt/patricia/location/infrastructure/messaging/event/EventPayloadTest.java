package ingprompt.patricia.location.infrastructure.messaging.event;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventPayloadTest {

    @Test
    void eventStartedEvent_allArgsAndAccessors() {
        UUID eventId = UUID.randomUUID();
        Set<UUID> participants = Set.of(UUID.randomUUID());

        EventStartedEvent event = new EventStartedEvent(eventId, participants);

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getParticipants()).isEqualTo(participants);
    }

    @Test
    void eventStartedEvent_noArgsSettersEqualsHashCodeToString() {
        UUID eventId = UUID.randomUUID();
        Set<UUID> participants = Set.of(UUID.randomUUID());

        EventStartedEvent a = new EventStartedEvent();
        a.setEventId(eventId);
        a.setParticipants(participants);

        EventStartedEvent b = new EventStartedEvent(eventId, participants);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains(eventId.toString());
    }

    @Test
    void eventEndedEvent_allBehaviour() {
        UUID eventId = UUID.randomUUID();

        EventEndedEvent a = new EventEndedEvent(eventId);
        EventEndedEvent b = new EventEndedEvent();
        b.setEventId(eventId);

        assertThat(a.getEventId()).isEqualTo(eventId);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains(eventId.toString());
    }

    @Test
    void incidentReportedEvent_allBehaviour() {
        UUID eventId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();

        IncidentReportedEvent a = new IncidentReportedEvent(eventId, reportId, reporterId);
        IncidentReportedEvent b = new IncidentReportedEvent();
        b.setEventId(eventId);
        b.setReportId(reportId);
        b.setReporterId(reporterId);

        assertThat(a.getEventId()).isEqualTo(eventId);
        assertThat(a.getReportId()).isEqualTo(reportId);
        assertThat(a.getReporterId()).isEqualTo(reporterId);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains(reportId.toString());
    }

    @Test
    void events_notEqualWhenFieldsDiffer() {
        EventEndedEvent a = new EventEndedEvent(UUID.randomUUID());
        EventEndedEvent b = new EventEndedEvent(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("other-type");
    }
}
