package ingprompt.patricia.location.application.service;

import ingprompt.patricia.location.application.port.out.AuditLogOutPort;
import ingprompt.patricia.location.application.port.out.StoredLocationRepositoryOutPort;
import ingprompt.patricia.location.domain.enums.PersistenceReason;
import ingprompt.patricia.location.domain.exception.UnauthorizedLocationAccessException;
import ingprompt.patricia.location.domain.model.AuditEntry;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.StoredLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationAccessServiceTest {

    @Mock
    private StoredLocationRepositoryOutPort storedRepository;
    @Mock
    private AuditLogOutPort auditLog;

    private LocationAccessService service;

    private UUID eventId;
    private UUID requesterId;

    @BeforeEach
    void setUp() {
        service = new LocationAccessService(storedRepository, auditLog);
        eventId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
    }

    private StoredLocation stored() {
        return StoredLocation.rehydrate(UUID.randomUUID(), eventId, UUID.randomUUID(),
                new GeoPoint(4.6, -74.0), Instant.now(), null, PersistenceReason.INCIDENT_REPORT, UUID.randomUUID());
    }

    @Test
    void decrypt_withAuthorizedRole_returnsAndAuditsGrant() {
        when(storedRepository.findDecryptedByEventId(eventId)).thenReturn(List.of(stored()));

        List<StoredLocation> result = service.decryptStoredForEvent(eventId, requesterId, "ROLE_LEGAL_COMPLIANCE");

        assertThat(result).hasSize(1);
        ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLog).record(entry.capture());
        assertThat(entry.getValue().granted()).isTrue();
    }

    @Test
    void decrypt_withEmergencyResponderRole_isAllowed() {
        when(storedRepository.findDecryptedByEventId(eventId)).thenReturn(List.of());

        service.decryptStoredForEvent(eventId, requesterId, "ROLE_EMERGENCY_RESPONDER");

        verify(storedRepository).findDecryptedByEventId(eventId);
    }

    @Test
    void decrypt_withUnauthorizedRole_auditsDenialAndThrows() {
        assertThatThrownBy(() -> service.decryptStoredForEvent(eventId, requesterId, "ROLE_USER"))
                .isInstanceOf(UnauthorizedLocationAccessException.class);

        ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLog).record(entry.capture());
        assertThat(entry.getValue().granted()).isFalse();
        verify(storedRepository, never()).findDecryptedByEventId(any());
    }

    @Test
    void decrypt_withNullRole_throws() {
        assertThatThrownBy(() -> service.decryptStoredForEvent(eventId, requesterId, null))
                .isInstanceOf(UnauthorizedLocationAccessException.class);
    }
}
