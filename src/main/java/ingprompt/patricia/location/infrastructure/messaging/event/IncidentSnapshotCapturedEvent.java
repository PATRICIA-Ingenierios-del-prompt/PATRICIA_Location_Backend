package ingprompt.patricia.location.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbound: confirms incident evidence was secured. Coordinates are carried ONLY
 * as AES ciphertext — consumers without the decryption key cannot read positions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSnapshotCapturedEvent {
    private UUID eventId;
    private UUID reportId;
    private Instant capturedAt;
    private List<EncryptedPoint> points;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EncryptedPoint {
        private UUID userId;
        private String latitudeCipher;
        private String longitudeCipher;
        private Instant recordedAt;
    }
}
