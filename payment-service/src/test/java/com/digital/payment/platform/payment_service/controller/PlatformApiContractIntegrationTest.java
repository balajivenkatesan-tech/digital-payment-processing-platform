package com.digital.payment.platform.payment_service.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PlatformApiContractIntegrationTest.ContractTestController.class)
class PlatformApiContractIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void shouldPrefixControllerRoutesWithApiVersion() throws Exception {
        HttpResponse<String> response = get("/api/v1/contract-test");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("ok");
    }

    @Test
    void shouldReturnProblemDetailsForValidationErrors() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/contract-test/validate"))
                .header("Content-Type", "application/json")
                .header("X-Correlation-ID", "corr-123")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(response.body()).contains(
                "\"type\":\"about:blank\"",
                "\"title\":\"Bad Request\"",
                "\"status\":400",
                "\"detail\":\"Validation failed\"",
                "\"code\":\"VALIDATION_ERROR\"",
                "\"message\":\"Validation failed\"",
                "\"correlationId\":\"corr-123\"",
                "\"field\":\"name\""
        );
    }

    @Test
    void shouldPublishServiceSpecificOpenApiWithProblemSchema() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(
                "\"title\":\"payment-service API\"",
                "\"version\":\"v1\"",
                "\"ProblemDetails\"",
                "\"correlationId\""
        );
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @RestController
    static class ContractTestController {

        @GetMapping("/contract-test")
        String get() {
            return "ok";
        }

        @PostMapping("/contract-test/validate")
        void validate(@Valid @RequestBody ValidationPayload payload) {
        }
    }

    record ValidationPayload(@NotBlank String name) {
    }
}