package ingprompt.patricia.location.infrastructure.ws.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoUpdateMessage {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
