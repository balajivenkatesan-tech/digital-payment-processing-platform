package com.digital.payment.platform.common_lib.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ApiErrorResponseTest {

    @Test
    void shouldCreateValidationErrorResponse() {
        ApiErrorResponse response = ApiErrorResponse.validation(
                "/api/v1/payments",
                "corr-123",
                List.of(new ValidationError("amount", "must be greater than 0"))
        );

        assertThat(response.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.instance()).isEqualTo("/api/v1/payments");
        assertThat(response.correlationId()).isEqualTo("corr-123");
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).field()).isEqualTo("amount");
        assertThat(response.timestamp()).isNotNull();
    }
}
