package ingprompt.patricia.location.infrastructure.web;

import ingprompt.patricia.location.application.port.in.LocationAccessCase;
import ingprompt.patricia.location.domain.enums.PersistenceReason;
import ingprompt.patricia.location.domain.exception.UnauthorizedLocationAccessException;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.StoredLocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationAccessController.class)
class LocationAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationAccessCase locationAccessCase;

    @Test
    void decryptedReturnsLocationsForAuthorizedRole() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        String role = "ROLE_LEGAL_COMPLIANCE";

        StoredLocation stored = StoredLocation.rehydrate(
                UUID.randomUUID(), eventId, UUID.randomUUID(),
                new GeoPoint(4.6097, -74.0817), Instant.now(), null,
                PersistenceReason.INCIDENT_REPORT, reportId);

        when(locationAccessCase.decryptStoredForEvent(eq(eventId), eq(requesterId), eq(role)))
                .thenReturn(List.of(stored));

        mockMvc.perform(get("/api/locations/{eventId}/decrypted", eventId)
                        .header("X-User-Id", requesterId.toString())
                        .header("X-User-Role", role))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latitude").value(4.6097))
                .andExpect(jsonPath("$[0].longitude").value(-74.0817))
                .andExpect(jsonPath("$[0].reason").value("INCIDENT_REPORT"))
                .andExpect(jsonPath("$[0].reportId").value(reportId.toString()));
    }

    @Test
    void decryptedReturnsForbiddenForUnauthorizedRole() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        String role = "ROLE_USER";

        when(locationAccessCase.decryptStoredForEvent(eq(eventId), eq(requesterId), eq(role)))
                .thenThrow(new UnauthorizedLocationAccessException(requesterId, role));

        mockMvc.perform(get("/api/locations/{eventId}/decrypted", eventId)
                        .header("X-User-Id", requesterId.toString())
                        .header("X-User-Role", role))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void decryptedReturnsEmptyListWhenNoData() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        String role = "ROLE_EMERGENCY_RESPONDER";

        when(locationAccessCase.decryptStoredForEvent(eq(eventId), eq(requesterId), eq(role)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/locations/{eventId}/decrypted", eventId)
                        .header("X-User-Id", requesterId.toString())
                        .header("X-User-Role", role))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
