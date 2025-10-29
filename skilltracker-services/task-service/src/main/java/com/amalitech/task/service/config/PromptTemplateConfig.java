package com.amalitech.task.service.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class PromptTemplateConfig {

    @Bean
    public PromptTemplate codingPromptTemplate(
            @Value("classpath:prompts/coding/coding_generation_prompt.txt") Resource codingPromptResource
    ) {
        return new PromptTemplate(codingPromptResource);
    }

    @Bean
    public PromptTemplate codingEvaluationPromptTemplate(
            @Value("classpath:prompts/coding/coding_evaluation_prompt.txt") Resource codingEvaluationPromptResource
    ) {
        return new PromptTemplate(codingEvaluationPromptResource);
    }
}