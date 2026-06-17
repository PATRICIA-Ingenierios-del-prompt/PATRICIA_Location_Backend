package ingprompt.patricia.location.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuditEntry(
        UUID id,
        UUID requesterId,
        String role,
        String action,
        UUID eventId,
        boolean granted,
        String detail,
        Instant occurredAt
) {
    public static AuditEntry of(UUID requesterId, String role, String action, UUID eventId, boolean granted, String detail) {
        return new AuditEntry(UUID.randomUUID(), requesterId, role, action, eventId, granted, detail, Instant.now());
    }
}
