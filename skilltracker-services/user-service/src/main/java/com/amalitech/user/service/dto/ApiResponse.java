package com.amalitech.user.service.dto;
import java.util.Map;

public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, Map<String, Object> details) {
        return new ApiResponse<>(false, null, new ApiError(code, message, details));
    }
}
