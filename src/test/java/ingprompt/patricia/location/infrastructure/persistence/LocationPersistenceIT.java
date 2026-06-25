package ingprompt.patricia.location.infrastructure.persistence;

import ingprompt.patricia.location.application.port.out.EncryptionPort;
import ingprompt.patricia.location.domain.enums.PersistenceReason;
import ingprompt.patricia.location.domain.model.AuditEntry;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.domain.model.StoredLocation;
import ingprompt.patricia.location.infrastructure.crypto.AesGcmEncryptionAdapter;
import ingprompt.patricia.location.infrastructure.persistence.postgre.AuditLogRepository;
import ingprompt.patricia.location.infrastructure.persistence.postgre.StoredLocationRepository;
import ingprompt.patricia.location.infrastructure.persistence.repository.AuditLogAdapter;
import ingprompt.patricia.location.infrastructure.persistence.repository.StoredLocationRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the encrypted Postgres persistence (stored locations +
 * audit log) against a real PostgreSQL via Testcontainers, using the real
 * AES-GCM encryption adapter. Requires Docker — executed by Failsafe in CI.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class LocationPersistenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private StoredLocationRepository storedRepository;
    @Autowired
    private AuditLogRepository auditRepository;
    @Autowired
    private TestEntityManager entityManager;

    private StoredLocationRepositoryAdapter stored;
    private AuditLogAdapter audit;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        EncryptionPort encryption = new AesGcmEncryptionAdapter("test-secret");
        ReflectionTestUtils.invokeMethod(encryption, "init");
        stored = new StoredLocationRepositoryAdapter(storedRepository, encryption);
        audit = new AuditLogAdapter(auditRepository);
    }

    private LiveLocation live() {
        return new LiveLocation(eventId, userId, new GeoPoint(4.65, -74.05), Instant.now());
    }

    @Test
    void incident_savedEncrypted_isDecryptedOnRead() {
        stored.save(StoredLocation.incident(live(), UUID.randomUUID()));

        // Coordinates are stored as ciphertext, never as the plain value.
        assertThat(storedRepository.findByEventId(eventId).get(0).getLatitudeCipher())
                .isNotEqualTo("4.65");

        List<StoredLocation> decrypted = stored.findDecryptedByEventId(eventId);
        assertThat(decrypted).hasSize(1);
        assertThat(decrypted.get(0).getPoint().latitude()).isEqualTo(4.65);
        assertThat(decrypted.get(0).getReason()).isEqualTo(PersistenceReason.INCIDENT_REPORT);
    }

    @Test
    void upsertRoutineLastKnown_keepsOneRowPerUser() {
        stored.upsertRoutineLastKnown(StoredLocation.routineFlush(live(), Duration.ofHours(12)));

        LiveLocation moved = new LiveLocation(eventId, userId, new GeoPoint(5.0, -75.0), Instant.now());
        stored.upsertRoutineLastKnown(StoredLocation.routineFlush(moved, Duration.ofHours(12)));

        List<StoredLocation> rows = stored.findDecryptedByEventId(eventId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getPoint().latitude()).isEqualTo(5.0);
    }

    @Test
    void deleteExpired_removesPastRetentionRows() {
        StoredLocation expired = StoredLocation.rehydrate(UUID.randomUUID(), eventId, userId,
                new GeoPoint(4.65, -74.05), Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600), PersistenceReason.ROUTINE_FLUSH, null);
        stored.save(expired);
        entityManager.flush();
        entityManager.clear();

        int removed = stored.deleteExpired(Instant.now());

        assertThat(removed).isEqualTo(1);
        assertThat(storedRepository.findByEventId(eventId)).isEmpty();
    }

    @Test
    void auditLog_recordPersistsImmutableRow() {
        audit.record(AuditEntry.of(userId, "ROLE_LEGAL_COMPLIANCE", "DECRYPT_EVENT_LOCATIONS", eventId, true, "ok"));

        assertThat(auditRepository.count()).isEqualTo(1);
    }
}
