package com.amalitech.common.security.dto.response;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Map<String, Object> metadata;

    public ApiResponse(boolean success, String message, T data, Map<String, Object> metadata) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.metadata = metadata;
    }

    public static <T> ApiResponse<T> success(String message, T data, String traceId) {
        return new ApiResponse<>(
                true,
                message,
                data,
                Map.of(
                        "traceId", traceId != null ? traceId : "....",
                        "timestamp", Instant.now().toString()
                )
        );
    }

    public static <T> ApiResponse<T> error(String message, String traceId) {
        return new ApiResponse<> (
                false,
                message,
                null,
                Map.of(
                        "traceId", traceId != null ? traceId : "...",
                        "timestamp", Instant.now().toString()
                )
        );
    }
}
