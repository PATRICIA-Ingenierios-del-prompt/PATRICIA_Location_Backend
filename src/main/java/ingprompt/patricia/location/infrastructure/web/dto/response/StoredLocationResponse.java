package ingprompt.patricia.location.infrastructure.web.dto.response;

import ingprompt.patricia.location.domain.enums.PersistenceReason;
import ingprompt.patricia.location.domain.model.StoredLocation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Decrypted view — only ever returned through the audited, RBAC-guarded endpoint. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoredLocationResponse {
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private double latitude;
    private double longitude;
    private Instant recordedAt;
    private Instant expiresAt;
    private PersistenceReason reason;
    private UUID reportId;

    public static StoredLocationResponse from(StoredLocation s) {
        return new StoredLocationResponse(
                s.getId(), s.getEventId(), s.getUserId(),
                s.getPoint().latitude(), s.getPoint().longitude(),
                s.getRecordedAt(), s.getExpiresAt(), s.getReason(), s.getReportId());
    }
}
