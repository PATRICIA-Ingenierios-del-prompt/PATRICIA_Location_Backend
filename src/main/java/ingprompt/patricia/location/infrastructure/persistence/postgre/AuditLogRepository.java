package ingprompt.patricia.location.infrastructure.persistence.postgre;

import ingprompt.patricia.location.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}
