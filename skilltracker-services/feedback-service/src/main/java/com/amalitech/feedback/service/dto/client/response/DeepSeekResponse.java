package com.amalitech.feedback.service.dto.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * CLIENT DTO we get FROM DeepSeek (POST /chat/completions).
 */
@Data
public class DeepSeekResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private String content; // AI's JSON-formatted feedback
    }

    /**
     * Helper to get the first message's content directly.
     * @return The AI's response text, or null if no choices are available.
     */
    public String getFirstChoiceContent() {
        if (choices != null && !choices.isEmpty()) {
            Message message = choices.get(0).getMessage();
            if (message != null) {
                return message.getContent();
            }
        }
        return null;
    }
}