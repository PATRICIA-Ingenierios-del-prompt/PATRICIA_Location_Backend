package ingprompt.patricia.location.domain.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnauthorizedLocationAccessExceptionTest {

    @Test
    void messageContainsRequesterIdAndRole() {
        UUID requesterId = UUID.randomUUID();
        String role = "ROLE_USER";

        UnauthorizedLocationAccessException ex =
                new UnauthorizedLocationAccessException(requesterId, role);

        assertTrue(ex.getMessage().contains(requesterId.toString()));
        assertTrue(ex.getMessage().contains(role));
        assertTrue(ex.getMessage().contains("not authorized"));
    }

    @Test
    void messageWithNullRole() {
        UUID requesterId = UUID.randomUUID();

        UnauthorizedLocationAccessException ex =
                new UnauthorizedLocationAccessException(requesterId, null);

        assertTrue(ex.getMessage().contains(requesterId.toString()));
        assertTrue(ex.getMessage().contains("null"));
    }
}
