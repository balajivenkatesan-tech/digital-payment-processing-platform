package com.digital.payment.platform.common_lib.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
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

        return problemResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", request, errors);
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

        return problemResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", request, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "INTERNAL_ERROR";
        String message = "An unexpected error occurred";

        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            status = errorResponse.getStatusCode();
            code = "HTTP_ERROR";
            String detail = errorResponse.getBody().getDetail();
            message = detail == null || detail.isBlank() ? "The request could not be processed" : detail;
        }

        return problemResponse(status, code, message, request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> problemResponse(
            HttpStatusCode status,
            String code,
            String message,
            HttpServletRequest request,
            List<ValidationError> errors
    ) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String title = knownStatus == null ? "HTTP Error" : knownStatus.getReasonPhrase();
        ApiErrorResponse response = new ApiErrorResponse(
                "about:blank",
                title,
                status.value(),
                message,
                request.getRequestURI(),
                code,
                message,
                resolveCorrelationId(request),
                java.time.Instant.now(),
                errors
        );

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
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
