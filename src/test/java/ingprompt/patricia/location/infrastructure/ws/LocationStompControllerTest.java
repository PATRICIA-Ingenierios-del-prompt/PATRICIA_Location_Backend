package ingprompt.patricia.location.infrastructure.ws;

import ingprompt.patricia.location.application.port.in.TrackLocationCase;
import ingprompt.patricia.location.infrastructure.ws.dto.GeoUpdateMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LocationStompControllerTest {

    @Mock
    private TrackLocationCase trackLocationCase;
    @InjectMocks
    private LocationStompController controller;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Principal principal(String name) {
        return () -> name;
    }

    @Test
    void onLocation_withValidPrincipal_delegates() {
        controller.onLocation(eventId, new GeoUpdateMessage(4.6, -74.0), principal(userId.toString()));
        verify(trackLocationCase).updateLiveLocation(eventId, userId, 4.6, -74.0);
    }

    @Test
    void onLocation_withNullPrincipal_isDropped() {
        controller.onLocation(eventId, new GeoUpdateMessage(4.6, -74.0), null);
        verifyNoInteractions(trackLocationCase);
    }

    @Test
    void onLocation_withNonUuidPrincipal_isDropped() {
        controller.onLocation(eventId, new GeoUpdateMessage(4.6, -74.0), principal("not-a-uuid"));
        verifyNoInteractions(trackLocationCase);
    }
}
