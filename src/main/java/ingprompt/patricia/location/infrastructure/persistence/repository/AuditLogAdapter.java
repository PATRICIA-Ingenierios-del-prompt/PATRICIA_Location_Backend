package ingprompt.patricia.location.infrastructure.persistence.repository;

import ingprompt.patricia.location.application.port.out.AuditLogOutPort;
import ingprompt.patricia.location.domain.model.AuditEntry;
import ingprompt.patricia.location.infrastructure.persistence.entity.AuditLogEntity;
import ingprompt.patricia.location.infrastructure.persistence.postgre.AuditLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuditLogAdapter implements AuditLogOutPort {
    private final AuditLogRepository repository;

    @Override
    public void record(AuditEntry entry) {
        AuditLogEntity e = new AuditLogEntity();
        e.setId(entry.id());
        e.setRequesterId(entry.requesterId());
        e.setRole(entry.role());
        e.setAction(entry.action());
        e.setEventId(entry.eventId());
        e.setGranted(entry.granted());
        e.setDetail(entry.detail());
        e.setOccurredAt(entry.occurredAt());
        repository.save(e);
    }
}
