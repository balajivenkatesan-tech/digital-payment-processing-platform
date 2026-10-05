package com.digital.payment.platform.common_lib.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI platformOpenApi(Environment environment) {
        String serviceName = environment.getProperty("spring.application.name", "payment-platform");
        return new OpenAPI()
            .info(new Info()
                .title(serviceName + " API")
                .version("v1")
                .description("Version 1 API for " + serviceName + "."));
        }

        @Bean
        public OpenApiCustomizer platformProblemDetailsSchema() {
        return openApi -> {
            Schema<?> validationError = new Schema<>();
            validationError.setType("object");
            validationError.addProperty("field", new StringSchema());
            validationError.addProperty("reason", new StringSchema());
            validationError.addRequiredItem("field");
            validationError.addRequiredItem("reason");

            ArraySchema validationErrors = new ArraySchema();
            validationErrors.setItems(validationError);

            Schema<?> problemDetails = new Schema<>();
            problemDetails.setType("object");
            problemDetails.addProperty("type", new StringSchema().format("uri"));
            problemDetails.addProperty("title", new StringSchema());
            problemDetails.addProperty("status", new IntegerSchema());
            problemDetails.addProperty("detail", new StringSchema());
            problemDetails.addProperty("instance", new StringSchema());
            problemDetails.addProperty("code", new StringSchema());
            problemDetails.addProperty("message", new StringSchema());
            problemDetails.addProperty("correlationId", new StringSchema());
            problemDetails.addProperty("timestamp", new DateTimeSchema());
            problemDetails.addProperty("errors", validationErrors);
            problemDetails.addRequiredItem("type");
            problemDetails.addRequiredItem("title");
            problemDetails.addRequiredItem("status");
            problemDetails.addRequiredItem("detail");
            problemDetails.addRequiredItem("instance");
            problemDetails.addRequiredItem("code");
            problemDetails.addRequiredItem("message");
            problemDetails.addRequiredItem("correlationId");
            problemDetails.addRequiredItem("timestamp");

            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents()
                    .addSchemas("ProblemDetails", problemDetails)
                    .addSchemas("ValidationError", validationError);
        };
    }
}
