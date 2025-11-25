# Implementation Plan: MCQ `correct_answer` as Index

## Overview
Change `correct_answer` field in MCQ tasks from a string value to an integer index.

**Before:**
```json
"correct_answer": "To evaluate the security of a system by simulating an attack"
```

**After:**
```json
"correct_answer": 1
```

---

## Files to Modify

### 1. **Task Service - Model Layer**

#### File: `skilltracker-services/task-service/src/main/java/com/amalitech/task/service/model/content/impl/McqTaskContent.java`

**Change:**
```java
// OLD
private String correct_answer;

// NEW
private int correct_answer;
```

**Why:**
- This is the entity that holds the task structure
- Must support integer type for Jackson serialization/deserialization
- Will be persisted as JSONB in database

---

### 2. **Task Service - Content Generation**

#### File: `skilltracker-services/task-service/src/main/java/com/amalitech/task/service/service/impl/ContentGeneratorServiceImpl.java`

**Current State:** Generates MCQ content from AI prompts

**What needs to change:**
- The MCQ generation prompt instructs AI to generate `correct_answer` as the actual string
- We need to **parse** the AI response to convert the answer string to its index

**Implementation approach:**

```java
private McqTaskContent.Question processMcqQuestion(/* ... */) {
    // ... existing code ...
    
    // AI returns: correct_answer = "Software Development Pentest"
    String correctAnswerString = aiResponse.getCorrect_answer();
    List<String> options = aiResponse.getOptions();
    
    // Convert string to index
    int correctAnswerIndex = options.indexOf(correctAnswerString);
    
    if (correctAnswerIndex == -1) {
        log.error("AI generated invalid correct_answer: {} not in options", correctAnswerString);
        // Handle error - throw exception or use default
        throw new IllegalArgumentException("Generated correct_answer not in options");
    }
    
    // Build question with index
    return McqTaskContent.Question.builder()
        .question_number(/* ... */)
        .options(options)
        .correct_answer(correctAnswerIndex)  // Store as int!
        .explanation(/* ... */)
        .build();
}
```

**Files involved:**
- `ContentGeneratorServiceImpl.java` - main generation logic
- Possibly `MCQMapper.java` - if there's a mapper converting AI response to Question

---

### 3. **Task Service - Validation**

#### File: `skilltracker-services/task-service/src/main/java/com/amalitech/task/service/validation/McqContentValidator.java` (create if doesn't exist)

**What to add:**
Validate that `correct_answer` index is within valid range for each question.

```java
@Component
public class McqContentValidator {
    
    public void validateMcqContent(McqTaskContent content) {
        if (content == null || content.getQuestions() == null) {
            throw new ValidationException("MCQ content or questions are null");
        }
        
        for (McqTaskContent.Question question : content.getQuestions()) {
            validateQuestion(question);
        }
    }
    
    private void validateQuestion(McqTaskContent.Question question) {
        List<String> options = question.getOptions();
        int correctAnswer = question.getCorrect_answer();
        
        if (options == null || options.isEmpty()) {
            throw new ValidationException(
                "Question " + question.getQuestion_number() + " has no options"
            );
        }
        
        if (correctAnswer < 0 || correctAnswer >= options.size()) {
            throw new ValidationException(
                String.format(
                    "Invalid correct_answer index %d for question %s (valid range: 0-%d)",
                    correctAnswer,
                    question.getQuestion_number(),
                    options.size() - 1
                )
            );
        }
    }
}
```

---

### 4. **Feedback Service - Evaluator**

#### File: `skilltracker-services/feedback-service/src/main/java/com/amalitech/feedback/service/evaluator/MCQTaskEvaluator.java`

**Current state:** Not implemented (TODO)

**Implementation:**

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class MCQTaskEvaluator implements TaskEvaluator {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.info("Evaluating MCQ task submission: {}", event.getSubmissionId());
        
        try {
            // 1. Parse answer and task content
            McqSubmissionAnswer answer = parseAnswer(event.getAnswer());
            McqTaskContent taskContent = parseTaskContent(event.getTaskContent());
            
            // 2. Validate content
            validateContent(taskContent);
            
            // 3. Evaluate each question
            List<McqSubmissionFeedback.QuestionFeedback> feedbacks = new ArrayList<>();
            int totalCorrect = 0;
            
            for (McqSubmissionAnswer.QuestionAnswer qa : answer.getAnswers()) {
                McqTaskContent.Question question = findQuestion(taskContent, qa.getQuestionNumber());
                
                // Direct integer comparison!
                int correctOption = question.getCorrect_answer();
                boolean isCorrect = qa.getSelectedOption() == correctOption;
                
                if (isCorrect) totalCorrect++;
                
                String explanation = question.getExplanation() != null 
                    ? question.getExplanation() 
                    : "No explanation available.";
                
                feedbacks.add(new McqSubmissionFeedback.QuestionFeedback(
                    qa.getQuestionNumber(),
                    isCorrect,
                    correctOption,
                    explanation
                ));
            }
            
            // 4. Calculate score
            int totalQuestions = answer.getAnswers().size();
            double scorePercentage = (totalCorrect * 100.0) / totalQuestions;
            
            McqSubmissionFeedback feedback = new McqSubmissionFeedback(
                feedbacks,
                totalCorrect,
                totalQuestions,
                scorePercentage
            );
            
            // 5. Return event
            return Mono.just(SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score((int) scorePercentage)
                .isCorrect(totalCorrect == totalQuestions)
                .feedbackType("MCQ")
                .detailedFeedback(objectMapper.writeValueAsString(feedback))
                .overallFeedback(String.format(
                    "You got %d out of %d questions correct (%.0f%%)",
                    totalCorrect, totalQuestions, scorePercentage
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCQ evaluation failed: {}", e.getMessage(), e);
            return buildFallbackEvent(event);
        }
    }
    
    @Override
    public String getTaskType() {
        return "MCQ";
    }
    
    private void validateContent(McqTaskContent content) {
        if (content == null || content.getQuestions() == null) {
            throw new IllegalArgumentException("Invalid MCQ content");
        }
        for (McqTaskContent.Question question : content.getQuestions()) {
            if (question.getCorrect_answer() < 0 || 
                question.getCorrect_answer() >= question.getOptions().size()) {
                throw new IllegalArgumentException(
                    "Invalid correct_answer index for question: " + question.getQuestion_number()
                );
            }
        }
    }
    
    private McqSubmissionAnswer parseAnswer(String answerJson) throws Exception {
        return objectMapper.readValue(answerJson, McqSubmissionAnswer.class);
    }
    
    private McqTaskContent parseTaskContent(String contentJson) throws Exception {
        return objectMapper.readValue(contentJson, McqTaskContent.class);
    }
    
    private McqTaskContent.Question findQuestion(
        McqTaskContent content, 
        String questionNumber
    ) {
        return content.getQuestions().stream()
            .filter(q -> q.getQuestion_number().equals(questionNumber))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Question not found: " + questionNumber
            ));
    }
    
    private Mono<SubmissionEvaluatedEvent> buildFallbackEvent(SubmissionCreatedEvent event) {
        return Mono.just(SubmissionEvaluatedEvent.builder()
            .submissionId(event.getSubmissionId())
            .userId(event.getUserId())
            .status("ERROR")
            .score(0)
            .isCorrect(false)
            .feedbackType("MCQ")
            .overallFeedback("MCQ evaluation failed. Please contact support.")
            .build());
    }
}
```

---

### 5. **Feedback Service - DTOs**

#### File: `skilltracker-services/feedback-service/src/main/java/com/amalitech/feedback/service/dto/client/content/McqTaskContent.java`

**Change:** Same as task-service - `correct_answer` should be `int`

```java
// OLD
private String correct_answer;

// NEW
private int correct_answer;
```

---

## Implementation Steps (in order)

### Step 1: Update Models
1. Modify `McqTaskContent.java` in task-service (change `correct_answer` to `int`)
2. Modify `McqTaskContent.java` in feedback-service (change `correct_answer` to `int`)

### Step 2: Update Content Generation
1. Find where AI response is processed in `ContentGeneratorServiceImpl.java`
2. Add logic to convert `correct_answer` string to index
3. Add validation to ensure index is within bounds
4. Test with existing MCQ generation tests

### Step 3: Create/Update Validation
1. Create `McqContentValidator.java` if it doesn't exist
2. Add validation logic for `correct_answer` index bounds
3. Call this validator after content generation

### Step 4: Implement MCQ Evaluator
1. Implement `MCQTaskEvaluator.evaluate()` method
2. Ensure direct integer comparison (no string parsing)
3. Build feedback with per-question results
4. Create unit tests

### Step 5: Update/Create Answer & Feedback Models
1. Ensure `McqSubmissionAnswer.java` has `List<QuestionAnswer>` structure
2. Ensure `McqSubmissionFeedback.java` has proper feedback structure
3. Both should already exist, just verify they match the design

### Step 6: Test & Verify
1. Run existing MCQ generation tests
2. Create new tests for evaluator
3. Test full flow: generation → submission → evaluation

---

## Potential Issues & How to Handle

### Issue 1: Backward Compatibility
**Problem:** If there are existing MCQ tasks in DB with `correct_answer` as string

**Solution:**
- Option A: Add migration script to convert existing data
- Option B: Only apply to new tasks, accept both formats temporarily
- Option C: Create new task definitions, deprecate old ones

**For now:** Assume clean slate (new branch, no existing data)

### Issue 2: AI Prompt Needs Update
**Problem:** Current MCQ prompt tells AI to generate answer as string

**Solution:**
- Update the prompt to still generate string (easier for AI)
- Convert string to index in code (ContentGeneratorServiceImpl)
- This way, AI doesn't need changes

**Implementation:** The `processMcqQuestion()` method handles conversion

### Issue 3: Type Safety
**Problem:** JSON deserialization might fail if `correct_answer` field is wrong type

**Solution:**
- Jackson handles int/string conversion automatically
- Add explicit validation in validator
- Return clear error messages if validation fails

---

## Testing Strategy

### Unit Tests

#### Task-Service Tests
```java
// ContentGeneratorServiceImpl Test
@Test
void testMcqContentGenerationConvertsCorrectAnswerToIndex() {
    // Given: AI response with correct_answer as string
    // When: processQuestion() is called
    // Then: correct_answer should be converted to valid index
}

@Test
void testMcqContentValidationRejectsInvalidIndex() {
    // Given: MCQ content with correct_answer = 5 but only 4 options
    // When: validate() is called
    // Then: ValidationException is thrown
}
```

#### Feedback-Service Tests
```java
// MCQTaskEvaluator Test
@Test
void testMcqEvaluationComparesByIndex() {
    // Given: User selects option 1, correct_answer is 1
    // When: evaluate() is called
    // Then: isCorrect should be true
}

@Test
void testMcqEvaluationCalculatesScoreCorrectly() {
    // Given: 10 questions, 8 correct
    // When: evaluate() is called
    // Then: scorePercentage should be 80
}
```

### Integration Tests
- Create full MCQ task
- Generate content
- Submit answer
- Verify evaluation result

---

## Branch Strategy

```bash
git checkout dev
git pull origin dev
git checkout -b feature/mcq-correct-answer-index
```

After implementation:
```bash
git push origin feature/mcq-correct-answer-index --force-with-lease
# Create PR for code review
# After approval, merge to dev
```

---

## Files Summary

| File | Change | Type |
|------|--------|------|
| `McqTaskContent.java` (task-service) | `String → int` | Model |
| `McqTaskContent.java` (feedback-service) | `String → int` | DTO |
| `ContentGeneratorServiceImpl.java` | Add string→index conversion | Logic |
| `McqContentValidator.java` | Create/Update | Validation |
| `MCQTaskEvaluator.java` | Implement full evaluation | Logic |
| `McqSubmissionAnswer.java` | Verify structure | Model |
| `McqSubmissionFeedback.java` | Verify structure | Model |

