package ingprompt.patricia.location.infrastructure.web.dto.response;

import ingprompt.patricia.location.domain.model.LiveLocation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LiveLocationResponse {
    private UUID userId;
    private double latitude;
    private double longitude;
    private Instant recordedAt;

    public static LiveLocationResponse from(LiveLocation live) {
        return new LiveLocationResponse(
                live.userId(),
                live.point().latitude(),
                live.point().longitude(),
                live.recordedAt());
    }
}
