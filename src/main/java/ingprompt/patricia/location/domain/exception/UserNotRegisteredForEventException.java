package ingprompt.patricia.location.domain.exception;

public class UserNotRegisteredForEventException extends RuntimeException {
    public UserNotRegisteredForEventException() {
        super("user is not registered for this event");
    }
}
