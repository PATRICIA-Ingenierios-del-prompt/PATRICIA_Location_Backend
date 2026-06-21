package ingprompt.patricia.location.infrastructure.web;

import ingprompt.patricia.location.domain.exception.UnauthorizedLocationAccessException;
import ingprompt.patricia.location.domain.exception.UserNotRegisteredForEventException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedLocationAccessException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(UnauthorizedLocationAccessException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UserNotRegisteredForEventException.class)
    public ResponseEntity<Map<String, String>> handleNotRegistered(UserNotRegisteredForEventException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
