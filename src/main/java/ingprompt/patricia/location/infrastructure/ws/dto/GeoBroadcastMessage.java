package ingprompt.patricia.location.infrastructure.ws.dto;

import ingprompt.patricia.location.domain.model.LiveLocation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoBroadcastMessage {
    private UUID userId;
    private double latitude;
    private double longitude;
    private Instant recordedAt;

    public static GeoBroadcastMessage from(LiveLocation live) {
        return new GeoBroadcastMessage(
                live.userId(),
                live.point().latitude(),
                live.point().longitude(),
                live.recordedAt()
        );
    }
}
