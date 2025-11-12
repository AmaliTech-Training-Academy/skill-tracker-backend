package com.amalitech.task.service.validation;

import com.amalitech.common.event.events.TaskCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskCompletedEventValidator.
 *
 * <p>Tests validation of required fields, data ranges, and consistency
 * of TaskCompletedEvent objects.</p>
 */
class TaskCompletedEventValidatorTest {

    private TaskCompletedEventValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TaskCompletedEventValidator();
    }

    @Test
    void testValidEventPassesValidation() {
        // Arrange
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(100)
                .passed(true)
                .completedAt(Instant.now())
                .rubricsScores(Map.of(
                        "correctness", TaskCompletedEvent.RubricScoreData.builder()
                                .score(50.0)
                                .maxScore(50)
                                .percentage(100)
                                .build(),
                        "efficiency", TaskCompletedEvent.RubricScoreData.builder()
                                .score(30.0)
                                .maxScore(30)
                                .percentage(100)
                                .build(),
                        "style", TaskCompletedEvent.RubricScoreData.builder()
                                .score(20.0)
                                .maxScore(20)
                                .percentage(100)
                                .build()
                ))
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertTrue(errors.isEmpty(), "Valid event should have no errors");
        assertTrue(validator.isValid(event));
    }

    @Test
    void testNullEventFails() {
        // Act
        List<String> errors = validator.validate(null);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("TaskCompletedEvent is null"));
    }

    @Test
    void testMissingRequiredFieldsFail() {
        // Arrange
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                // All required fields missing
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("userId")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("taskId")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillId")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("taskType")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("completedAt")));
    }

    @Test
    void testXpEarnedOutOfRangeFails() {
        // Arrange - XP too high
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(15000)  // Exceeds max 10000
                .completedAt(Instant.now())
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("totalXpEarned")));
    }

    @Test
    void testNegativeXpEarnedFails() {
        // Arrange
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(-50)  // Negative XP
                .completedAt(Instant.now())
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("totalXpEarned")));
    }

    @Test
    void testRubricScoreExceedsMaxFails() {
        // Arrange
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(100)
                .passed(true)
                .completedAt(Instant.now())
                .rubricsScores(Map.of(
                        "correctness", TaskCompletedEvent.RubricScoreData.builder()
                                .score(55.0)  // Exceeds max 50
                                .maxScore(50)
                                .percentage(110)
                                .build()
                ))
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("score exceeds maxScore")));
    }

    @Test
    void testNegativeRubricScoreFails() {
        // Arrange
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(100)
                .completedAt(Instant.now())
                .rubricsScores(Map.of(
                        "correctness", TaskCompletedEvent.RubricScoreData.builder()
                                .score(-10.0)  // Negative score
                                .maxScore(50)
                                .percentage(-20)
                                .build()
                ))
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("score is negative")));
    }

    @Test
    void testPercentageOutOfRangeFails() {
        // Arrange
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(100)
                .completedAt(Instant.now())
                .rubricsScores(Map.of(
                        "correctness", TaskCompletedEvent.RubricScoreData.builder()
                                .score(50.0)
                                .maxScore(50)
                                .percentage(150)  // Exceeds 100%
                                .build()
                ))
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("percentage must be 0-100")));
    }

    @Test
    void testMissingExpectedCodingRubricsFails() {
        // Arrange
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(100)
                .completedAt(Instant.now())
                .rubricsScores(Map.of(
                        "correctness", TaskCompletedEvent.RubricScoreData.builder()
                                .score(50.0)
                                .maxScore(50)
                                .percentage(100)
                                .build()
                        // Missing 'efficiency' and 'style'
                ))
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("efficiency") && e.contains("missing")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("style") && e.contains("missing")));
    }

    @Test
    void testMissingExpectedEssayRubricsFails() {
        // Arrange
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("ESSAY")
                .totalXpEarned(100)
                .completedAt(Instant.now())
                .rubricsScores(Map.of(
                        "completeness", TaskCompletedEvent.RubricScoreData.builder()
                                .score(25.0)
                                .maxScore(25)
                                .percentage(100)
                                .build()
                        // Missing accuracy, clarity, depth
                ))
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("accuracy") && e.contains("missing")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("clarity") && e.contains("missing")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("depth") && e.contains("missing")));
    }

    @Test
    void testMCQTaskWithoutRubricsIsValid() {
        // Arrange - MCQ tasks may not have detailed rubrics
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("MCQ")
                .totalXpEarned(100)
                .passed(true)
                .completedAt(Instant.now())
                .rubricsScores(Map.of())  // Empty rubrics for MCQ
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    void testNullRubricDataFails() {
        // Arrange
        Map<String, TaskCompletedEvent.RubricScoreData> rubrics = new HashMap<>();
        rubrics.put("correctness", null);  // Null rubric data
        
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(100)
                .completedAt(Instant.now())
                .rubricsScores(rubrics)
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("null score data")));
    }

    @Test
    void testDecimalPrecisionPreserved() {
        // Arrange - Test that decimal scores are accepted
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID().toString())
                .skillId(UUID.randomUUID())
                .taskType("CODING")
                .totalXpEarned(100)
                .completedAt(Instant.now())
                .rubricsScores(Map.of(
                        "correctness", TaskCompletedEvent.RubricScoreData.builder()
                                .score(45.5)  // Decimal score
                                .maxScore(50)
                                .percentage(91)
                                .build(),
                        "efficiency", TaskCompletedEvent.RubricScoreData.builder()
                                .score(28.75)  // Decimal score
                                .maxScore(30)
                                .percentage(95)
                                .build(),
                        "style", TaskCompletedEvent.RubricScoreData.builder()
                                .score(19.25)  // Decimal score
                                .maxScore(20)
                                .percentage(96)
                                .build()
                ))
                .build();

        // Act
        List<String> errors = validator.validate(event);

        // Assert
        assertTrue(errors.isEmpty(), "Decimal scores should be valid");
    }
}
