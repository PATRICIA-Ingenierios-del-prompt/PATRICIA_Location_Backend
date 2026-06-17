package ingprompt.patricia.location.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/** Inbound from Event MS: an event began — start tracking its participants. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventStartedEvent {
    private UUID eventId;
    private Set<UUID> participants;
}
