package ingprompt.patricia.location.application.port.out;

import ingprompt.patricia.location.domain.model.LiveLocation;

import java.util.List;
import java.util.UUID;

public interface LocationEventPublisherOut {
    void publishIncidentSnapshot(UUID eventId, UUID reportId, List<LiveLocation> snapshot);
}
