package ingprompt.patricia.location.infrastructure.backplane;

import com.fasterxml.jackson.databind.JsonNode;

public record BackplaneEnvelope(String destination, JsonNode payload) {
}
