package ingprompt.patricia.location.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Inbound from Event MS: a participant reported an incident — secure the snapshot as evidence. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentReportedEvent {
    private UUID eventId;
    private UUID reportId;
    private UUID reporterId;
}
