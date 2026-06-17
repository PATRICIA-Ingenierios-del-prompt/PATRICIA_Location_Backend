package ingprompt.patricia.location.application.port.out;

import ingprompt.patricia.location.domain.model.AuditEntry;

public interface AuditLogOutPort {
    void record(AuditEntry entry);
}
