package ingprompt.patricia.location.domain.exception;

import java.util.UUID;

public class UnauthorizedLocationAccessException extends RuntimeException {
    public UnauthorizedLocationAccessException(UUID requesterId, String role) {
        super("Requester " + requesterId + " with role '" + role
                + "' is not authorized to decrypt location data");
    }
}
