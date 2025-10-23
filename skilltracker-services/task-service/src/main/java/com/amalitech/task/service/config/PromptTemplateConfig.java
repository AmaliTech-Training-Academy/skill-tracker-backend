package com.amalitech.task.service.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class PromptTemplateConfig {
    @Value("classpath:prompts/mcq_prompt.txt")
    private Resource mcqPromptResource;

    /**
     * Creates a Spring AI PromptTemplate bean from the resource file.
     * This bean can now be safely injected into any service.
     */
    @Bean
    public PromptTemplate mcqPromptTemplate() {
        return new PromptTemplate(mcqPromptResource);
    }
}