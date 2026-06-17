package ingprompt.patricia.location.application.port.in;

import ingprompt.patricia.location.domain.model.StoredLocation;

import java.util.List;
import java.util.UUID;

public interface LocationAccessCase {
    List<StoredLocation> decryptStoredForEvent(UUID eventId, UUID requesterId, String role);
}
