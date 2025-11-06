package com.amalitech.task.service.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuration class for creating Spring AI {@link PromptTemplate} beans.
 * <p>
 * This class defines beans that load prompt templates from external resources
 * (e.g., classpath files) to be used by AI services within the application.
 */
@Configuration
public class PromptTemplateConfig {

    /**
     * Creates a {@link PromptTemplate} bean specifically for coding generation tasks.
     * <p>
     * This bean loads its template content from the classpath resource specified by the
     * {@code @Value} annotation ({@code "classpath:prompts/coding/coding_generation_prompt.txt"}).
     *
     * @param codingPromptResource The {@link Resource} injected by Spring, pointing to the
     * prompt text file on the classpath.
     * @return A {@link PromptTemplate} instance initialized with the content of the
     * provided resource.
     */
    @Bean
    public PromptTemplate codingPromptTemplate(
            @Value("classpath:prompts/coding/coding_generation_prompt.txt") Resource codingPromptResource
    ) {
        return new PromptTemplate(codingPromptResource);
    }

//    @Bean
//    public PromptTemplate essayPromptTemplate(
//            @Value("classpath:prompts/essay/essay_generation_prompt.txt") Resource essayPromptResource
//    ) {
//        return new PromptTemplate(essayPromptResource);
//    }

}