package ingprompt.patricia.location.domain.exception;

/**
 * Raised when stored location data cannot be deserialized or parsed,
 * indicating data corruption or a key mismatch.
 */
public class LocationDataCorruptionException extends RuntimeException {
    public LocationDataCorruptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
