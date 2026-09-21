package com.digital.payment.platform.common_lib.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().instance()).isEqualTo("/api/v1/payments");
        assertThat(response.getBody().correlationId()).isEqualTo("corr-123");
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().get(0).field()).isEqualTo("amount");
        assertThat(response.getBody().errors().get(0).reason()).isEqualTo("must not be blank");
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
