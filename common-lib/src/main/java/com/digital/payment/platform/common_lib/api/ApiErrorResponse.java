package com.digital.payment.platform.common_lib.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        String instance,
        String correlationId,
        Instant timestamp,
        List<ValidationError> errors
) {
    public ApiErrorResponse {
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
                "VALIDATION_ERROR",
                "Validation failed",
                400,
                instance,
                correlationId,
                Instant.now(),
                errors
        );
    }
}
