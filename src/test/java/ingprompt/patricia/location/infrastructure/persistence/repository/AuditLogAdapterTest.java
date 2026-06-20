package ingprompt.patricia.location.infrastructure.persistence.repository;

import ingprompt.patricia.location.domain.model.AuditEntry;
import ingprompt.patricia.location.infrastructure.persistence.entity.AuditLogEntity;
import ingprompt.patricia.location.infrastructure.persistence.postgre.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogAdapterTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditLogAdapter adapter;

    @Test
    void recordMapsAllFieldsToEntity() {
        UUID requesterId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        AuditEntry entry = AuditEntry.of(requesterId, "ROLE_LEGAL_COMPLIANCE",
                "DECRYPT_EVENT_LOCATIONS", eventId, true, "Decrypted 5 rows");

        adapter.record(entry);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        AuditLogEntity entity = captor.getValue();

        assertEquals(entry.id(), entity.getId());
        assertEquals(requesterId, entity.getRequesterId());
        assertEquals("ROLE_LEGAL_COMPLIANCE", entity.getRole());
        assertEquals("DECRYPT_EVENT_LOCATIONS", entity.getAction());
        assertEquals(eventId, entity.getEventId());
        assertTrue(entity.isGranted());
        assertEquals("Decrypted 5 rows", entity.getDetail());
        assertEquals(entry.occurredAt(), entity.getOccurredAt());
    }

    @Test
    void recordDeniedEntry() {
        UUID requesterId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        AuditEntry entry = AuditEntry.of(requesterId, "ROLE_USER",
                "DECRYPT_EVENT_LOCATIONS", eventId, false, "Denied: role not permitted");

        adapter.record(entry);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        AuditLogEntity entity = captor.getValue();

        assertFalse(entity.isGranted());
        assertEquals("ROLE_USER", entity.getRole());
    }
}
