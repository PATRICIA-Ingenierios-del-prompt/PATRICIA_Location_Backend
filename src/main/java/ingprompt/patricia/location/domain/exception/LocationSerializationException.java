package ingprompt.patricia.location.domain.exception;

/**
 * Raised when a location payload cannot be serialized or deserialized
 * to/from the live store (Redis).
 */
public class LocationSerializationException extends RuntimeException {
    public LocationSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
