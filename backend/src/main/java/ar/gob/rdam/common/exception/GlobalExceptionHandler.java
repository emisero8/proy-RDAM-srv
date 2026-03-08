package ar.gob.rdam.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, ApiError>> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException [{}]: {}", ex.getCode(), ex.getMessage());
        HttpStatus status = mapCodeToStatus(ex.getCode());
        return ResponseEntity.status(status).body(errorBody(ex.getCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, ApiError>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("UNAUTHORIZED", "Credenciales inválidas.", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, ApiError>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody("FORBIDDEN", "Sin permisos para esta acción.", null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, ApiError>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String field = first != null ? first.getField() : null;
        String msg = first != null ? first.getDefaultMessage() : "Datos de entrada inválidos";
        return ResponseEntity.badRequest().body(errorBody("VALIDATION_ERROR", msg, field));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, ApiError>> handleGeneral(Exception ex) {
        log.error("Error no manejado: ", ex);
        return ResponseEntity.internalServerError()
                .body(errorBody("INTERNAL_ERROR", "Error interno del servidor.", null));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, ApiError> errorBody(String code, String message, String field) {
        return Map.of("error", ApiError.builder()
                .code(code)
                .message(message)
                .field(field)
                .timestamp(LocalDateTime.now())
                .requestId("req_" + UUID.randomUUID().toString().substring(0, 8))
                .build());
    }

    private HttpStatus mapCodeToStatus(String code) {
        return switch (code) {
            case "VALIDATION_ERROR", "INVALID_TRANSITION" -> HttpStatus.BAD_REQUEST;
            case "UNAUTHORIZED", "TOKEN_EXPIRED" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN", "USER_INACTIVE" -> HttpStatus.FORBIDDEN;
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CONFLICT" -> HttpStatus.CONFLICT;
            case "BUSINESS_RULE" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
