package com.amalitech.task.service.util;

import com.amalitech.task.service.model.content.impl.McqTaskContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class McqOptionShufflerTest {

    private McqOptionShuffler shuffler;

    @BeforeEach
    void setUp() {
        shuffler = new McqOptionShuffler();
    }

    @Test
    void testShuffleOptionsPreservesAllOptions() {
        // Arrange
        List<String> options = List.of("Option A", "Option B", "Option C", "Option D");
        McqTaskContent.Question question = McqTaskContent.Question.builder()
                .question_number("1")
                .question_text("What is 2+2?")
                .options(options)
                .correct_answer(2)  // "Option C" is correct
                .build();

        McqTaskContent content = McqTaskContent.builder()
                .questions(List.of(question))
                .build();

        // Act
        McqTaskContent shuffled = shuffler.shuffleOptions(content);

        // Assert
        McqTaskContent.Question shuffledQuestion = shuffled.getQuestions().get(0);
        
        // All original options should still be present
        assertEquals(4, shuffledQuestion.getOptions().size());
        assertTrue(shuffledQuestion.getOptions().containsAll(options));
    }

    @Test
    void testCorrectAnswerTrackingAfterShuffle() {
        // Arrange
        List<String> options = List.of("Wrong1", "Wrong2", "Correct", "Wrong3");
        McqTaskContent.Question question = McqTaskContent.Question.builder()
                .question_number("1")
                .question_text("Which is correct?")
                .options(options)
                .correct_answer(2)  // "Correct" is at index 2
                .build();

        McqTaskContent content = McqTaskContent.builder()
                .questions(List.of(question))
                .build();

        // Act
        McqTaskContent shuffled = shuffler.shuffleOptions(content);

        // Assert
        McqTaskContent.Question shuffledQuestion = shuffled.getQuestions().get(0);
        String correctOption = shuffledQuestion.getOptions().get(shuffledQuestion.getCorrect_answer());
        
        assertEquals("Correct", correctOption, "The correct answer should still be at the correct_answer index");
    }

    @Test
    void testShuffleMultipleQuestions() {
        // Arrange
        McqTaskContent.Question q1 = McqTaskContent.Question.builder()
                .question_number("1")
                .question_text("Q1?")
                .options(List.of("A", "B", "C", "D"))
                .correct_answer(0)
                .build();

        McqTaskContent.Question q2 = McqTaskContent.Question.builder()
                .question_number("2")
                .question_text("Q2?")
                .options(List.of("W", "X", "Y", "Z"))
                .correct_answer(3)
                .build();

        McqTaskContent content = McqTaskContent.builder()
                .questions(List.of(q1, q2))
                .build();

        // Act
        McqTaskContent shuffled = shuffler.shuffleOptions(content);

        // Assert
        assertEquals(2, shuffled.getQuestions().size());
        
        // Verify first question
        assertEquals("A", shuffled.getQuestions().get(0).getOptions().get(shuffled.getQuestions().get(0).getCorrect_answer()));
        
        // Verify second question
        assertEquals("Z", shuffled.getQuestions().get(1).getOptions().get(shuffled.getQuestions().get(1).getCorrect_answer()));
    }

    @Test
    void testShuffleHandlesNullContent() {
        // Act & Assert - should not throw
        McqTaskContent result = shuffler.shuffleOptions(null);
        assertNull(result);
    }

    @Test
    void testShuffleHandlesEmptyQuestions() {
        // Arrange
        McqTaskContent content = McqTaskContent.builder()
                .questions(List.of())
                .build();

        // Act
        McqTaskContent result = shuffler.shuffleOptions(content);

        // Assert
        assertTrue(result.getQuestions().isEmpty());
    }

    @Test
    void testShufflePreservesOtherQuestionFields() {
        // Arrange
        List<String> options = List.of("A", "B", "C", "D");
        McqTaskContent.Question question = McqTaskContent.Question.builder()
                .question_number("1")
                .question_title("Title")
                .question_text("Question text?")
                .question_description("Description")
                .question_duration(5)
                .question_difficulty("EASY")
                .options(options)
                .correct_answer(1)
                .xpReward(10)
                .hint("This is a hint")
                .explanation("This is the explanation")
                .build();

        McqTaskContent content = McqTaskContent.builder()
                .questions(List.of(question))
                .build();

        // Act
        McqTaskContent shuffled = shuffler.shuffleOptions(content);
        McqTaskContent.Question shuffledQuestion = shuffled.getQuestions().get(0);

        // Assert
        assertEquals("1", shuffledQuestion.getQuestion_number());
        assertEquals("Title", shuffledQuestion.getQuestion_title());
        assertEquals("Question text?", shuffledQuestion.getQuestion_text());
        assertEquals("Description", shuffledQuestion.getQuestion_description());
        assertEquals(5, shuffledQuestion.getQuestion_duration());
        assertEquals("EASY", shuffledQuestion.getQuestion_difficulty());
        assertEquals(10, shuffledQuestion.getXpReward());
        assertEquals("This is a hint", shuffledQuestion.getHint());
        assertEquals("This is the explanation", shuffledQuestion.getExplanation());
    }

    @Test
    void testShuffleDistributesAnswerPositions() {
        // Arrange - run multiple shuffles and collect positions
        List<String> options = List.of("Wrong", "Wrong", "Correct", "Wrong");
        int[] positionCounts = new int[4];

        // Act - shuffle 100 times
        for (int i = 0; i < 100; i++) {
            McqTaskContent.Question question = McqTaskContent.Question.builder()
                    .question_number("1")
                    .question_text("Q?")
                    .options(options)
                    .correct_answer(2)
                    .build();

            McqTaskContent content = McqTaskContent.builder()
                    .questions(List.of(question))
                    .build();

            McqTaskContent shuffled = shuffler.shuffleOptions(content);
            int correctPos = shuffled.getQuestions().get(0).getCorrect_answer();
            positionCounts[correctPos]++;
        }

        // Assert - all positions should be used (rough distribution check)
        int positionsUsed = 0;
        for (int count : positionCounts) {
            if (count > 0) {
                positionsUsed++;
            }
        }
        
        assertTrue(positionsUsed >= 3, "Shuffling should use at least 3 different positions out of 4 in 100 iterations");
    }
}
