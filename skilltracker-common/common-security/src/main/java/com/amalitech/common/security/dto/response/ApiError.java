package com.amalitech.common.security.dto.response;

import java.util.Map;

public record ApiError(String code, String message, Map<String, Object> details) {}