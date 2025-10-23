package com.amalitech.common.security.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiError(
        int status,
        String message,
        String detail,
        String instance,
        List<FieldError> errors,
        Map<String, Object> metadata
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(
            int status,
            String message,
            String detail,
            String instance,
            List<FieldError> errors,
            String traceId
    ) {
        return new ApiError(
                status,
                message,
                detail,
                instance,
                errors,
                Map.of(
                        "traceId", traceId != null ? traceId : "....",
                        "timestamp", Instant.now().toString()
                )
        );
    }
}
