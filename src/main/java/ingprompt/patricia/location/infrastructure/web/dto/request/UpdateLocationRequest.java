package ingprompt.patricia.location.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLocationRequest {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
