package com.amalitech.user.service.dto;

import java.util.Map;

public record ApiError(String code, String message, Map<String, Object> details) {}