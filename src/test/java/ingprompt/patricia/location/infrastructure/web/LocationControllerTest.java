package ingprompt.patricia.location.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ingprompt.patricia.location.application.port.in.TrackLocationCase;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.infrastructure.web.dto.request.UpdateLocationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackLocationCase trackLocationCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void updateLocationReturnsAccepted() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setLatitude(4.6097);
        request.setLongitude(-74.0817);

        mockMvc.perform(post("/api/locations/{eventId}", eventId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(trackLocationCase).updateLiveLocation(eq(eventId), eq(userId), eq(4.6097), eq(-74.0817));
    }

    @Test
    void updateLocationRejectsMissingLatitude() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String body = "{\"longitude\": -74.0817}";

        mockMvc.perform(post("/api/locations/{eventId}", eventId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void liveSnapshotReturnsLocations() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-06-15T12:00:00Z");

        when(trackLocationCase.liveSnapshot(eventId)).thenReturn(List.of(
                new LiveLocation(eventId, userId, new GeoPoint(4.6097, -74.0817), recordedAt)));

        mockMvc.perform(get("/api/locations/{eventId}/live", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].latitude").value(4.6097))
                .andExpect(jsonPath("$[0].longitude").value(-74.0817));
    }

    @Test
    void liveSnapshotReturnsEmptyList() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(trackLocationCase.liveSnapshot(eventId)).thenReturn(List.of());

        mockMvc.perform(get("/api/locations/{eventId}/live", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
