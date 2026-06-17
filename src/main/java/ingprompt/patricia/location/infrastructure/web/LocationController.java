package ingprompt.patricia.location.infrastructure.web;

import ingprompt.patricia.location.application.port.in.TrackLocationCase;
import ingprompt.patricia.location.infrastructure.web.dto.request.UpdateLocationRequest;
import ingprompt.patricia.location.infrastructure.web.dto.response.LiveLocationResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
@AllArgsConstructor
public class LocationController {
    private final TrackLocationCase trackLocationCase;

    @PostMapping("/{eventId}")
    public ResponseEntity<Void> updateLocation(@PathVariable UUID eventId, @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody UpdateLocationRequest request) {
        trackLocationCase.updateLiveLocation(eventId, userId, request.getLatitude(), request.getLongitude());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{eventId}/live")
    public ResponseEntity<List<LiveLocationResponse>> liveSnapshot(@PathVariable UUID eventId) {
        List<LiveLocationResponse> snapshot = trackLocationCase.liveSnapshot(eventId).stream().map(LiveLocationResponse::from).toList();
        return ResponseEntity.ok(snapshot);
    }
}
