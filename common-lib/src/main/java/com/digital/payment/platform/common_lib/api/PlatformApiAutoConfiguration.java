package com.digital.payment.platform.common_lib.api;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({ApiExceptionHandler.class, ApiVersionConfiguration.class, OpenApiConfig.class})
public class PlatformApiAutoConfiguration {
}