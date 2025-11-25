package com.amalitech.task.service.util;

import com.amalitech.task.service.model.content.impl.McqTaskContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility component for shuffling MCQ options and tracking correct answer position.
 * Eliminates AI bias towards placing correct answers at the beginning by randomizing
 * option order after generation while maintaining correct answer tracking.
 */
@Component
@Slf4j
public class McqOptionShuffler {

    private static final Random random = new Random();

    /**
     * Shuffles all options in MCQ content and updates correct_answer indices.
     * For each question, the options array is randomly reordered and the correct_answer
     * index is updated to reflect the new position.
     *
     * @param content the MCQ task content to shuffle
     * @return the shuffled MCQ content
     */
    public McqTaskContent shuffleOptions(McqTaskContent content) {
        if (content == null || content.getQuestions() == null) {
            return content;
        }

        List<McqTaskContent.Question> shuffledQuestions = new ArrayList<>();

        for (McqTaskContent.Question question : content.getQuestions()) {
            McqTaskContent.Question shuffledQuestion = shuffleQuestion(question);
            shuffledQuestions.add(shuffledQuestion);
        }

        content.setQuestions(shuffledQuestions);
        return content;
    }

    /**
     * Shuffles options for a single MCQ question.
     * Creates a mapping of original indices to new indices, updates the correct_answer
     * index accordingly, then reorders the options list.
     *
     * @param question the question to shuffle
     * @return a new question with shuffled options and updated correct_answer index
     */
    private McqTaskContent.Question shuffleQuestion(McqTaskContent.Question question) {
        List<String> originalOptions = question.getOptions();
        int originalCorrectIndex = question.getCorrect_answer();

        if (originalOptions == null || originalOptions.size() <= 1) {
            // No shuffling needed for 0 or 1 option
            return question;
        }

        // Create list of indices: [0, 1, 2, 3]
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < originalOptions.size(); i++) {
            indices.add(i);
        }

        // Fisher-Yates shuffle of indices
        for (int i = indices.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            // Swap
            Integer temp = indices.get(i);
            indices.set(i, indices.get(j));
            indices.set(j, temp);
        }

        // Build new options list based on shuffled indices
        List<String> shuffledOptions = new ArrayList<>();
        for (Integer index : indices) {
            shuffledOptions.add(originalOptions.get(index));
        }

        // Find new position of correct answer
        int newCorrectIndex = indices.indexOf(originalCorrectIndex);

        // Create new question with shuffled options
        return McqTaskContent.Question.builder()
                .question_number(question.getQuestion_number())
                .question_title(question.getQuestion_title())
                .question_description(question.getQuestion_description())
                .question_text(question.getQuestion_text())
                .question_duration(question.getQuestion_duration())
                .question_difficulty(question.getQuestion_difficulty())
                .options(shuffledOptions)
                .hint(question.getHint())
                .correct_answer(newCorrectIndex)
                .xpReward(question.getXpReward())
                .explanation(question.getExplanation())
                .build();
    }
}
