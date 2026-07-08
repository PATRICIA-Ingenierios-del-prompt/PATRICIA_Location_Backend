package ingprompt.patricia.location.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ingprompt.patricia.location.application.port.in.TrackLocationCase;
import ingprompt.patricia.location.domain.exception.UserNotRegisteredForEventException;
import ingprompt.patricia.location.domain.model.GeoPoint;
import ingprompt.patricia.location.domain.model.LiveLocation;
import ingprompt.patricia.location.infrastructure.web.dto.request.UpdateLocationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrackLocationCase trackLocationCase;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private UpdateLocationRequest request(Double lat, Double lng) {
        UpdateLocationRequest r = new UpdateLocationRequest();
        r.setLatitude(lat);
        r.setLongitude(lng);
        return r;
    }

    @Test
    void updateLocation_withHeader_returns202() throws Exception {
        mockMvc.perform(post("/api/locations/{eventId}", eventId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(4.6, -74.0))))
                .andExpect(status().isAccepted());
        verify(trackLocationCase).updateLiveLocation(eq(eventId), eq(userId), eq(4.6), eq(-74.0));
    }

    @Test
    void updateLocation_withoutHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/locations/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(4.6, -74.0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLocation_withMissingCoordinates_returns400() throws Exception {
        mockMvc.perform(post("/api/locations/{eventId}", eventId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(null, -74.0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLocation_whenNotRegistered_returns400() throws Exception {
        doThrow(new UserNotRegisteredForEventException())
                .when(trackLocationCase).updateLiveLocation(eq(eventId), eq(userId), anyDouble(), anyDouble());

        mockMvc.perform(post("/api/locations/{eventId}", eventId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(4.6, -74.0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void liveSnapshot_returns200() throws Exception {
        LiveLocation live = new LiveLocation(eventId, userId, new GeoPoint(4.6, -74.0), Instant.now());
        when(trackLocationCase.liveSnapshot(eventId, userId)).thenReturn(List.of(live));

        mockMvc.perform(get("/api/locations/{eventId}/live", eventId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }
}
