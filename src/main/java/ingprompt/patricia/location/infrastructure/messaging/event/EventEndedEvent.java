package ingprompt.patricia.location.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Inbound from Event MS: an event ended — flush the final snapshot and stop tracking. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventEndedEvent {
    private UUID eventId;
}
