package ingprompt.patricia.location.infrastructure.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Inbound from Event MS: an event ended — flush the final snapshot and stop tracking. */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EventEndedEvent extends BaseEvent {

    public EventEndedEvent(UUID eventId) {
        super(eventId);
    }
}
