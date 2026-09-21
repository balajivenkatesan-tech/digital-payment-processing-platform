package com.digital.payment.platform.common_lib.api;

import java.util.Objects;

public record ValidationError(String field, String reason) {
    public ValidationError {
        Objects.requireNonNull(field, "field cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
    }
}
