package ingprompt.patricia.location.infrastructure.web;

import ingprompt.patricia.location.application.port.in.LocationAccessCase;
import ingprompt.patricia.location.infrastructure.web.dto.response.StoredLocationResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SECURITY NOTE: {@code X-User-Id} and {@code X-User-Role} MUST be injected
 * by a trusted API gateway after JWT validation. The gateway must strip any
 * client-supplied values for these headers before forwarding. Without this
 * guarantee, callers can trivially escalate privileges.
 */
@RestController
@RequestMapping("/api/locations")
@AllArgsConstructor
public class LocationAccessController {

    private static final Set<String> VALID_ROLES = Set.of(
            "ROLE_LEGAL_COMPLIANCE",
            "ROLE_EMERGENCY_RESPONDER",
            "ROLE_USER",
            "ROLE_ADMIN"
    );

    private final LocationAccessCase locationAccessCase;

    @GetMapping("/{eventId}/decrypted")
    public ResponseEntity<List<StoredLocationResponse>> decrypted(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader("X-User-Role") String role) {

        if (role == null || role.isBlank() || !VALID_ROLES.contains(role)) {
            return ResponseEntity.badRequest().build();
        }

        List<StoredLocationResponse> result = locationAccessCase
                .decryptStoredForEvent(eventId, requesterId, role).stream()
                .map(StoredLocationResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }
}
