package ingprompt.patricia.location.application.service;

import ingprompt.patricia.location.application.port.in.LocationAccessCase;
import ingprompt.patricia.location.application.port.out.AuditLogOutPort;
import ingprompt.patricia.location.application.port.out.StoredLocationRepositoryOutPort;
import ingprompt.patricia.location.domain.exception.UnauthorizedLocationAccessException;
import ingprompt.patricia.location.domain.model.AuditEntry;
import ingprompt.patricia.location.domain.model.StoredLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocationAccessService implements LocationAccessCase {
    private static final Set<String> AUTHORIZED_ROLES = Set.of(
            "ROLE_LEGAL_COMPLIANCE",
            "ROLE_EMERGENCY_RESPONDER"
    );

    private static final String ACTION = "DECRYPT_EVENT_LOCATIONS";

    private final StoredLocationRepositoryOutPort storedRepository;
    private final AuditLogOutPort auditLog;

    public LocationAccessService(StoredLocationRepositoryOutPort storedRepository, AuditLogOutPort auditLog) {
        this.storedRepository = storedRepository;
        this.auditLog = auditLog;
    }

    @Override
    public List<StoredLocation> decryptStoredForEvent(UUID eventId, UUID requesterId, String role) {
        boolean authorized = role != null && AUTHORIZED_ROLES.contains(role);

        // Audit BOTH the denial and the grant — the attempt itself is legally relevant.
        if (!authorized) {
            auditLog.record(AuditEntry.of(requesterId, role, ACTION, eventId, false, "Denied: role not permitted to decrypt location data"));
            throw new UnauthorizedLocationAccessException(requesterId, role);
        }

        List<StoredLocation> decrypted = storedRepository.findDecryptedByEventId(eventId);
        auditLog.record(AuditEntry.of(requesterId, role, ACTION, eventId, true, "Decrypted " + decrypted.size() + " location rows for event " + eventId));
        return decrypted;
    }
}
