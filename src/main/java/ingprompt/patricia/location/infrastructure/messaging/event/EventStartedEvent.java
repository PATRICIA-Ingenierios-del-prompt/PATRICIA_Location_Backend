package ingprompt.patricia.location.infrastructure.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/** Inbound from Event MS: an event began — start tracking its participants. */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EventStartedEvent extends BaseEvent {
    private Set<UUID> participants;

    public EventStartedEvent(UUID eventId, Set<UUID> participants) {
        super(eventId);
        this.participants = participants;
    }
}
