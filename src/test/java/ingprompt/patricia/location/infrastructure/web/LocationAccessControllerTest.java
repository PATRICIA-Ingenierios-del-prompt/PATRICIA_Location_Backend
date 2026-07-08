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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationAccessController.class)
class LocationAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationAccessCase locationAccessCase;

    private final UUID eventId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();

    private StoredLocation stored() {
        return StoredLocation.rehydrate(UUID.randomUUID(), eventId, UUID.randomUUID(),
                new GeoPoint(4.6, -74.0), Instant.now(), null, PersistenceReason.INCIDENT_REPORT, UUID.randomUUID());
    }

    @Test
    void decrypted_withAuthorizedRole_returns200() throws Exception {
        when(locationAccessCase.decryptStoredForEvent(eq(eventId), eq(requesterId), eq("ROLE_LEGAL_COMPLIANCE")))
                .thenReturn(List.of(stored()));

        mockMvc.perform(get("/api/locations/{eventId}/decrypted", eventId)
                        .header("X-User-Id", requesterId)
                        .header("X-User-Roles", "ROLE_LEGAL_COMPLIANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latitude").value(4.6));
    }

    @Test
    void decrypted_withUnauthorizedRole_returns403() throws Exception {
        when(locationAccessCase.decryptStoredForEvent(any(), any(), eq("ROLE_USER")))
                .thenThrow(new UnauthorizedLocationAccessException(requesterId, "ROLE_USER"));

        mockMvc.perform(get("/api/locations/{eventId}/decrypted", eventId)
                        .header("X-User-Id", requesterId)
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void decrypted_withoutRoleHeader_returns400() throws Exception {
        mockMvc.perform(get("/api/locations/{eventId}/decrypted", eventId)
                        .header("X-User-Id", requesterId))
                .andExpect(status().isBadRequest());
    }
}
