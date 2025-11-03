package com.amalitech.feedback.service.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuration for AI prompt templates.
 * All prompts are externalized to text files for easy maintenance.
 */
@Configuration
public class PromptTemplateConfig {
    
    /**
     * Detailed coding evaluation prompt with comprehensive scoring.
     */
    @Bean
    public PromptTemplate codingEvaluationPromptTemplate(
            @Value("classpath:prompts/coding/coding_evaluation_prompt.txt") Resource codingEvaluationPromptResource
    ) {
        return new PromptTemplate(codingEvaluationPromptResource);
    }

    /**
     * Simple feedback prompt as fallback when detailed evaluation fails.
     */
    @Bean
    public PromptTemplate simpleFeedbackPromptTemplate(
            @Value("classpath:prompts/coding/simple_feedback_prompt.txt") Resource simpleFeedbackPromptResource
    ) {
        return new PromptTemplate(simpleFeedbackPromptResource);
    }
}