package ingprompt.patricia.location.infrastructure.web;

import ingprompt.patricia.location.application.port.in.LocationAccessCase;
import ingprompt.patricia.location.infrastructure.web.dto.response.StoredLocationResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
@AllArgsConstructor
public class LocationAccessController {
    private final LocationAccessCase locationAccessCase;

    @GetMapping("/{eventId}/decrypted")
    public ResponseEntity<List<StoredLocationResponse>> decrypted(@PathVariable UUID eventId, @RequestHeader("X-User-Id") UUID requesterId, @RequestHeader("X-User-Roles") String roles) {
        List<StoredLocationResponse> result = locationAccessCase
                .decryptStoredForEvent(eventId, requesterId, roles).stream()
                .map(StoredLocationResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }
}
