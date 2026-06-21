package ingprompt.patricia.location.infrastructure.ws;

import ingprompt.patricia.location.application.port.in.TrackLocationCase;
import ingprompt.patricia.location.infrastructure.ws.dto.GeoUpdateMessage;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@AllArgsConstructor
public class LocationStompController {
    private final TrackLocationCase trackLocationCase;

    @MessageMapping("/geo/{eventId}")
    public void onLocation(@DestinationVariable UUID eventId, @Valid @Payload GeoUpdateMessage payload, Principal principal) {
        if (principal == null) {
            log.warn("Dropping geo update for event {}: no authenticated principal", eventId);
            return;
        }
        UUID userId;
        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            log.warn("Dropping geo update for event {}: principal '{}' is not a UUID", eventId, principal.getName());
            return;
        }
        // Delegates to the same use case as the REST controller — all business rules,
        // persistence, and broadcast happen there.
        trackLocationCase.updateLiveLocation(eventId, userId, payload.getLatitude(), payload.getLongitude());
    }
}
