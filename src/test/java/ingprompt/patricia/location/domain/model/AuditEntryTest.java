package ingprompt.patricia.location.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditEntryTest {

    @Test
    void factoryMethodSetsAllFields() {
        UUID requesterId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String role = "ROLE_LEGAL_COMPLIANCE";
        String action = "DECRYPT_EVENT_LOCATIONS";
        String detail = "Decrypted 5 rows";

        AuditEntry entry = AuditEntry.of(requesterId, role, action, eventId, true, detail);

        assertNotNull(entry.id());
        assertEquals(requesterId, entry.requesterId());
        assertEquals(role, entry.role());
        assertEquals(action, entry.action());
        assertEquals(eventId, entry.eventId());
        assertTrue(entry.granted());
        assertEquals(detail, entry.detail());
        assertNotNull(entry.occurredAt());
    }

    @Test
    void factoryMethodGeneratesUniqueIds() {
        UUID requesterId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        AuditEntry a = AuditEntry.of(requesterId, "ROLE", "ACTION", eventId, true, "detail");
        AuditEntry b = AuditEntry.of(requesterId, "ROLE", "ACTION", eventId, true, "detail");

        assertNotEquals(a.id(), b.id());
    }

    @Test
    void deniedAuditEntry() {
        UUID requesterId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        AuditEntry entry = AuditEntry.of(requesterId, "ROLE_USER", "DECRYPT_EVENT_LOCATIONS",
                eventId, false, "Denied: role not permitted");

        assertFalse(entry.granted());
        assertTrue(entry.detail().contains("Denied"));
    }

    @Test
    void nullRoleIsAllowed() {
        UUID requesterId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        AuditEntry entry = AuditEntry.of(requesterId, null, "ACTION", eventId, false, "No role");
        assertNull(entry.role());
    }
}
