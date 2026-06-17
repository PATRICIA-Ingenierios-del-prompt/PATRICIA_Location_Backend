package ingprompt.patricia.location.infrastructure.persistence.entity;

import ingprompt.patricia.location.domain.enums.PersistenceReason;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "stored_locations")
@Data
public class StoredLocationEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String latitudeCipher;

    @Column(nullable = false)
    private String longitudeCipher;

    @Column(nullable = false)
    private Instant recordedAt;

    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PersistenceReason reason;

    private UUID reportId;
}
