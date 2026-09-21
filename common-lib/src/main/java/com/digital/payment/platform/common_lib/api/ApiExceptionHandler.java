package com.digital.payment.platform.common_lib.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();

        ApiErrorResponse response = ApiErrorResponse.validation(
                request.getRequestURI(),
                resolveCorrelationId(request),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        ApiErrorResponse response = ApiErrorResponse.validation(
                request.getRequestURI(),
                resolveCorrelationId(request),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private ValidationError toValidationError(FieldError fieldError) {
        String reason = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";
        return new ValidationError(fieldError.getField(), reason);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        return correlationId == null || correlationId.isBlank() ? "unknown" : correlationId;
    }
}
