package com.amalitech.task.service.seed;

import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.SkillViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Order(1) // Run before task generation
@RequiredArgsConstructor
@Slf4j
public class SkillDataSeeder implements CommandLineRunner {

    private final SkillViewRepository skillViewRepository;

    @Override
    public void run(String... args) {
        if (skillViewRepository.count() > 0) {
            log.info("Skills already seeded. Skipping...");
            return;
        }

        log.info("Seeding skills...");

        List<SkillView> skills = List.of(
                createSkill("JavaScript", "Core JavaScript programming fundamentals"),
                createSkill("Python", "Python programming and data structures"),
                createSkill("Java", "Java programming and OOP concepts"),
                createSkill("HTML & CSS", "Web design and styling fundamentals"),
                createSkill("React", "Modern React development"),
                createSkill("Node.js", "Server-side JavaScript with Node.js"),
                createSkill("SQL", "Database querying and management"),
                createSkill("Git", "Version control with Git"),
                createSkill("Communication", "Professional communication skills"),
                createSkill("Problem Solving", "Algorithmic thinking and problem solving")
        );

        skillViewRepository.saveAll(skills);

        log.info("Seeded {} skills successfully!", skills.size());
    }

    private SkillView createSkill(String name, String description) {
        return SkillView.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description(description)
                .build();
    }
}