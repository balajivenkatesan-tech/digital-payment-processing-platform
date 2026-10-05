package com.digital.payment.platform.common_lib.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiExceptionHandlerTest {

    @Test
    void shouldHandleValidationErrors() throws NoSuchMethodException {
        TestRequest requestBody = new TestRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(requestBody, "requestBody");
        bindingResult.rejectValue("amount", "NotBlank", "must not be blank");

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new MethodParameter(TestRequest.class.getDeclaredConstructor(), -1),
                bindingResult
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");

        ResponseEntity<ApiErrorResponse> response = new ApiExceptionHandler().handleMethodArgumentNotValid(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().type()).isEqualTo("about:blank");
        assertThat(response.getBody().title()).isEqualTo("Bad Request");
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().detail()).isEqualTo("Validation failed");
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().instance()).isEqualTo("/api/v1/payments");
        assertThat(response.getBody().correlationId()).isEqualTo("corr-123");
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().get(0).field()).isEqualTo("amount");
        assertThat(response.getBody().errors().get(0).reason()).isEqualTo("must not be blank");
    }

    @Test
    void shouldHideUnexpectedExceptionDetails() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/payments");

        ResponseEntity<ApiErrorResponse> response = new ApiExceptionHandler()
                .handleUnhandledException(new IllegalStateException("database password leaked"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().detail()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().correlationId()).isEqualTo("unknown");
    }

    static class TestRequest {
        @NotBlank
        private String amount;

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }
    }
}
