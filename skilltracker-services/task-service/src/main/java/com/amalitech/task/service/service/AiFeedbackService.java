//package com.amalitech.task.service.service;
//
//import com.amalitech.task.service.model.feedback.impl.EssaySubmissionFeedback;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.ChatClient;
//import org.springframework.ai.chat.messages.UserMessage;
//import org.springframework.ai.chat.prompt.Prompt;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AiFeedbackService {
//
//    private final ChatClient chatClient;
//
//    public EssaySubmissionFeedback generateEssayFeedback(
//            String topic, String essayText, List<String> guidelines) {
//
//        log.info("Generating AI feedback for essay on topic: {}", topic);
//
//        String guidelinesStr = String.join("\n- ", guidelines);
//
//        String promptText = String.format("""
//            You are an expert writing instructor. Evaluate this essay and provide scores and suggestions.
//
//            Topic: %s
//
//            Guidelines:
//            - %s
//
//            Essay:
//            %s
//
//            Respond in this format:
//            GRAMMAR_SCORE: [0.0-1.0]
//             RELEVANCE_SCORE: [0.0-1.0]
//            TONE: [professional/casual/academic/etc]
//            SUGGESTIONS:
//            - [original text] -> [suggested change]: [comment]
//            - [original text] -> [suggested change]: [comment]
//            """, topic, guidelinesStr, essayText);
//
//        try {
//            Prompt prompt = new Prompt(new UserMessage(promptText));
//            String response = chatClient.call(prompt).getResult().getOutput().getContent();
//
//            return parseEssayFeedback(response);
//
//        } catch (Exception e) {
//            log.error("Error generating AI feedback: {}", e.getMessage());
//            return createDefaultFeedback();
//        }
//    }
//
//    private EssaySubmissionFeedback parseEssayFeedback(String response) {
//        // Simple parsing - in production, use structured output or JSON
//        double grammarScore = 0.8;
//        double relevanceScore = 0.8;
//        String tone = "academic";
//        List<EssaySubmissionFeedback.FeedbackSuggestion> suggestions = new ArrayList<>();
//
//        try {
//            String[] lines = response.split("\n");
//            for (String line : lines) {
//                if (line.startsWith("GRAMMAR_SCORE:")) {
//                    grammarScore = Double.parseDouble(line.split(":")[1].trim());
//                } else if (line.startsWith("RELEVANCE_SCORE:")) {
//                    relevanceScore = Double.parseDouble(line.split(":")[1].trim());
//                } else if (line.startsWith("TONE:")) {
//                    tone = line.split(":")[1].trim();
//                } else if (line.startsWith("-") && line.contains("->")) {
//                    // Parse suggestion
//                    String[] parts = line.substring(1).split("->");
//                    if (parts.length == 2) {
//                        String[] secondPart = parts[1].split(":");
//                        if (secondPart.length == 2) {
//                            EssaySubmissionFeedback.FeedbackSuggestion suggestion =
//                                    new EssaySubmissionFeedback.FeedbackSuggestion();
//                            suggestion.setOriginalText(parts[0].trim());
//                            suggestion.setSuggestedChange(secondPart[0].trim());
//                            suggestion.setComment(secondPart[1].trim());
//                            suggestions.add(suggestion);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error parsing feedback: {}", e.getMessage());
//        }
//
//        return new EssaySubmissionFeedback(grammarScore, relevanceScore, tone, suggestions);
//    }
//
//    private EssaySubmissionFeedback createDefaultFeedback() {
//        return new EssaySubmissionFeedback(
//                0.7,
//                0.7,
//                "Unable to analyze",
//                new ArrayList<>()
//        );
//    }
//}