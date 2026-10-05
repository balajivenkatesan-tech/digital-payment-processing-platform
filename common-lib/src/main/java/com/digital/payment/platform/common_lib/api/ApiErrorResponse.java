package com.digital.payment.platform.common_lib.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ApiErrorResponse(
    String type,
    String title,
        int status,
    String detail,
        String instance,
    String code,
    String message,
        String correlationId,
        Instant timestamp,
        List<ValidationError> errors
) {
    public ApiErrorResponse {
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(title, "title cannot be null");
    Objects.requireNonNull(detail, "detail cannot be null");
        Objects.requireNonNull(code, "code cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(instance, "instance cannot be null");
        Objects.requireNonNull(correlationId, "correlationId cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ApiErrorResponse validation(
            String instance,
            String correlationId,
            List<ValidationError> errors
    ) {
        return new ApiErrorResponse(
                "about:blank",
                "Bad Request",
                400,
                "Validation failed",
                instance,
                "VALIDATION_ERROR",
                "Validation failed",
                correlationId,
                Instant.now(),
                errors
        );
    }
}
