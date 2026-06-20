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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationAccessServiceTest {

    @Mock
    private StoredLocationRepositoryOutPort storedRepository;

    @Mock
    private AuditLogOutPort auditLog;

    private LocationAccessService service;

    @BeforeEach
    void setUp() {
        service = new LocationAccessService(storedRepository, auditLog);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_LEGAL_COMPLIANCE", "ROLE_EMERGENCY_RESPONDER"})
    void authorizedRoleReturnsDecryptedLocations(String role) {
        UUID eventId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        StoredLocation loc = StoredLocation.rehydrate(
                UUID.randomUUID(), eventId, UUID.randomUUID(),
                new GeoPoint(4.6, -74.0), Instant.now(), null,
                PersistenceReason.INCIDENT_REPORT, UUID.randomUUID());

        when(storedRepository.findDecryptedByEventId(eventId)).thenReturn(List.of(loc));

        List<StoredLocation> result = service.decryptStoredForEvent(eventId, requesterId, role);

        assertEquals(1, result.size());
        assertEquals(loc, result.get(0));
        verify(storedRepository).findDecryptedByEventId(eventId);

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLog).record(captor.capture());
        AuditEntry entry = captor.getValue();
        assertTrue(entry.granted());
        assertEquals(requesterId, entry.requesterId());
        assertEquals(role, entry.role());
        assertEquals(eventId, entry.eventId());
    }

    @Test
    void unauthorizedRoleThrowsAndAudits() {
        UUID eventId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        String role = "ROLE_USER";

        assertThrows(UnauthorizedLocationAccessException.class,
                () -> service.decryptStoredForEvent(eventId, requesterId, role));

        verify(storedRepository, never()).findDecryptedByEventId(any());

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLog).record(captor.capture());
        AuditEntry entry = captor.getValue();
        assertFalse(entry.granted());
        assertEquals(requesterId, entry.requesterId());
        assertEquals(role, entry.role());
        assertTrue(entry.detail().contains("Denied"));
    }

    @Test
    void nullRoleIsDenied() {
        UUID eventId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        assertThrows(UnauthorizedLocationAccessException.class,
                () -> service.decryptStoredForEvent(eventId, requesterId, null));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLog).record(captor.capture());
        assertFalse(captor.getValue().granted());
    }

    @Test
    void emptyResultForAuthorizedUser() {
        UUID eventId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(storedRepository.findDecryptedByEventId(eventId)).thenReturn(List.of());

        List<StoredLocation> result = service.decryptStoredForEvent(
                eventId, requesterId, "ROLE_LEGAL_COMPLIANCE");

        assertTrue(result.isEmpty());
        verify(auditLog).record(any());
    }

    @Test
    void auditDetailContainsDecryptedCount() {
        UUID eventId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        StoredLocation loc1 = StoredLocation.rehydrate(
                UUID.randomUUID(), eventId, UUID.randomUUID(),
                new GeoPoint(1, 1), Instant.now(), null,
                PersistenceReason.INCIDENT_REPORT, UUID.randomUUID());
        StoredLocation loc2 = StoredLocation.rehydrate(
                UUID.randomUUID(), eventId, UUID.randomUUID(),
                new GeoPoint(2, 2), Instant.now(), null,
                PersistenceReason.INCIDENT_REPORT, UUID.randomUUID());

        when(storedRepository.findDecryptedByEventId(eventId)).thenReturn(List.of(loc1, loc2));

        service.decryptStoredForEvent(eventId, requesterId, "ROLE_LEGAL_COMPLIANCE");

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLog).record(captor.capture());
        assertTrue(captor.getValue().detail().contains("2"));
    }
}
