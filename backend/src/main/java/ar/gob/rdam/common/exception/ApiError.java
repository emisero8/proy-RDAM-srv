package ar.gob.rdam.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiError {
    private String code;
    private String message;
    private String field;
    private LocalDateTime timestamp;
    private String requestId;
}
