package ingprompt.patricia.location.infrastructure.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Inbound from Event MS: a participant reported an incident — secure the snapshot as evidence. */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class IncidentReportedEvent extends BaseEvent {
    private UUID reportId;
    private UUID reporterId;

    public IncidentReportedEvent(UUID eventId, UUID reportId, UUID reporterId) {
        super(eventId);
        this.reportId = reportId;
        this.reporterId = reporterId;
    }
}
