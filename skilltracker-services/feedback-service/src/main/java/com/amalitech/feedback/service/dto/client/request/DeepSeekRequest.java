package com.amalitech.feedback.service.dto.client.request;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * CLIENT DTO we send TO DeepSeek (POST /chat/completions).
 */
@Data
@Builder
public class DeepSeekRequest {
    private String model;
    private List<Message> messages;

    @Data
    @Builder
    public static class Message {
        private String role; // "system", "user", or "assistant"
        private String content;
    }
}
