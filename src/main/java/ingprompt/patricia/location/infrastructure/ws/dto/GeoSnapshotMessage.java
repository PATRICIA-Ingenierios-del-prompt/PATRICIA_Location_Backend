package ingprompt.patricia.location.infrastructure.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoSnapshotMessage {
    private UUID eventId;
    private List<GeoBroadcastMessage> positions;
}
