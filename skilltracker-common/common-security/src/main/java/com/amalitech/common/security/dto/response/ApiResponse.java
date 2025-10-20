package com.amalitech.common.security.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private long timestamp = Instant.now().toEpochMilli();

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
