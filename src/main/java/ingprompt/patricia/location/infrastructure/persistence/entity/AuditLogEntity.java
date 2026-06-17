package ingprompt.patricia.location.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "location_audit_log")
@Data
public class AuditLogEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID requesterId;

    private String role;

    @Column(nullable = false)
    private String action;

    private UUID eventId;

    @Column(nullable = false)
    private boolean granted;

    @Column(length = 1000)
    private String detail;

    @Column(nullable = false)
    private Instant occurredAt;
}
