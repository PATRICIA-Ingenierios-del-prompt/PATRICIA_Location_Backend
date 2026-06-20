package ingprompt.patricia.location.infrastructure.web;

import ingprompt.patricia.location.domain.exception.UnauthorizedLocationAccessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleForbiddenReturnsForbiddenStatus() {
        UUID requesterId = UUID.randomUUID();
        UnauthorizedLocationAccessException ex =
                new UnauthorizedLocationAccessException(requesterId, "ROLE_USER");

        ResponseEntity<Map<String, String>> response = handler.handleForbidden(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").contains(requesterId.toString()));
        assertTrue(response.getBody().get("error").contains("ROLE_USER"));
    }

    @Test
    void handleBadRequestReturnsBadRequestStatus() {
        IllegalArgumentException ex = new IllegalArgumentException("latitude out of range: 91.0");

        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("latitude out of range: 91.0", response.getBody().get("error"));
    }
}
