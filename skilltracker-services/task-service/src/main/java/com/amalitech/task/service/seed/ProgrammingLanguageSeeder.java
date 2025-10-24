package com.amalitech.task.service.seed;

import com.amalitech.task.service.model.ProgrammingLanguage;
import com.amalitech.task.service.repository.ProgrammingLanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ProgrammingLanguageSeeder implements CommandLineRunner {
    private final ProgrammingLanguageRepository languageRepository;

    @Override
    public void run(String... args) {
        if (languageRepository.count() > 0) {
            log.info("Programming languages already seeded. Skipping...");
            return;
        }

        log.info("Seeding programming languages...");

        List<ProgrammingLanguage> languages = List.of(
                createLanguage("JavaScript"),
                createLanguage("Python"),
                createLanguage("Java"),
                createLanguage("C++"),
                createLanguage("C#"),
                createLanguage("Go"),
                createLanguage("Rust"),
                createLanguage("TypeScript"),
                createLanguage("PHP"),
                createLanguage("Ruby")
        );

        languageRepository.saveAll(languages);

        log.info("Seeded {} programming languages successfully!", languages.size());
    }

    private ProgrammingLanguage createLanguage(String name) {
        return ProgrammingLanguage.builder()
                .name(name)
                .build();
    }
}