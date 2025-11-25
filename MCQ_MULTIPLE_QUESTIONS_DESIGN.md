# MCQ Task with Multiple Questions - Detailed Design

## Current State (Broken)

**Problem**: One `TaskSubmission` stores only ONE answer, but a Task can have MULTIPLE Questions.

Currently:
```java
// McqSubmissionAnswer.java
@Data
public class McqSubmissionAnswer implements SubmissionAnswer {
    @Min(0)
    private int selectedOption;  // Only ONE answer!
}
```

This means:
- A task with 5 MCQ questions can only track 1 answer
- No way to know which question was answered
- No way to track score per question vs. overall score

---

## Proposed Solution

### 0. Update Task Content Structure

```java
// McqTaskContent.java - Question class
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public static class Question {
    private String question_number;
    private String question_title;
    private String question_description;
    private String question_text;
    private int question_duration;
    private String question_difficulty;
    private List<String> options;
    private String hint;
    private int correct_answer;  // CHANGED: Now an index (0, 1, 2, 3...) instead of string
    private int xpReward;
    private String explanation;
}
```

**Why index?**
- Direct integer comparison during evaluation
- No string matching bugs (whitespace, encoding, etc.)
- Smaller payload
- Clearer intent

### 1. Updated Answer Structure

```java
// McqSubmissionAnswer.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McqSubmissionAnswer implements SubmissionAnswer {
    private List<QuestionAnswer> answers;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnswer {
        private String questionNumber;     // e.g., "1", "2", "3" (matches question_number from task)
        @Min(0)
        private int selectedOption;        // Index of selected option in options array (0, 1, 2, 3...)
    }
}
```

### 2. Updated Feedback Structure

```java
// McqSubmissionFeedback.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McqSubmissionFeedback implements SubmissionFeedback {
    private List<QuestionFeedback> feedbacks;
    private int totalCorrect;
    private int totalQuestions;
    private double scorePercentage;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionFeedback {
        private String questionNumber;
        private boolean isCorrect;
        private int correctOption;
        private String explanation;
    }
}
```

---

## Data Flow

### 1. Client Submission (JSON)

Based on the actual MCQ task with 10 questions, client submits:

```json
{
  "taskId": "1335f072-03eb-4619-adea-4dff3d09cc90",
  "answer": {
    "answers": [
      {
        "questionNumber": "1",
        "selectedOption": 1
      },
      {
        "questionNumber": "2",
        "selectedOption": 3
      },
      {
        "questionNumber": "3",
        "selectedOption": 0
      },
      {
        "questionNumber": "4",
        "selectedOption": 2
      },
      {
        "questionNumber": "5",
        "selectedOption": 2
      },
      {
        "questionNumber": "6",
        "selectedOption": 0
      },
      {
        "questionNumber": "7",
        "selectedOption": 1
      },
      {
        "questionNumber": "8",
        "selectedOption": 0
      },
      {
        "questionNumber": "9",
        "selectedOption": 1
      },
      {
        "questionNumber": "10",
        "selectedOption": 0
      }
    ]
  }
}
```

**What the indices mean:**
- Question 1 has 4 options → `selectedOption: 1` = user selected "To evaluate the security of a system by simulating an attack"
- Question 2 has 4 options → `selectedOption: 3` = user selected "Software Development Pentest"
- etc.

### 2. Controller Layer (No Changes Required)

The `SubmissionController` remains unchanged:
```java
@PostMapping
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<SubmissionResponse>> submitTask(
    @AuthenticationPrincipal String userIdPrincipal,
    @Valid @RequestBody SubmitAnswerRequest request
) {
    // Jackson polymorphic deserialization handles it
    TaskSubmissionDTO submissionDTO = submissionService.createSubmission(request, userId);
    // ...
}
```

Jackson automatically deserializes based on the task type (via `@JsonSubTypes`).

### 3. Database Storage

The `TaskSubmission` entity remains unchanged - it already uses JSONB:
```java
@Type(JsonType.class)
@Column(columnDefinition = "jsonb")
private SubmissionAnswer answer;
```

**In PostgreSQL** (what gets stored):
```json
{
  "answers": [
    {"questionNumber": "1", "selectedOption": 1},
    {"questionNumber": "2", "selectedOption": 3},
    {"questionNumber": "3", "selectedOption": 0},
    ...
    {"questionNumber": "10", "selectedOption": 0}
  ]
}
```

**After evaluation - feedback stored:**
```json
{
  "totalCorrect": 8,
  "totalQuestions": 10,
  "scorePercentage": 80.0,
  "feedbacks": [
    {
      "questionNumber": "1",
      "isCorrect": true,
      "correctOption": 1,
      "explanation": "Penetration testing is conducted to evaluate the security of a system by simulating an attack from malicious outsiders and insiders."
    },
    {
      "questionNumber": "2",
      "isCorrect": false,
      "correctOption": 3,
      "explanation": "Software Development Pentest is not a recognized type of pentesting. Common types include network, social engineering, and database pentests."
    },
    {
      "questionNumber": "3",
      "isCorrect": true,
      "correctOption": 0,
      "explanation": "Nmap is a widely used tool for network scanning, helping identify open ports and services on a network."
    },
    ...
    {
      "questionNumber": "10",
      "isCorrect": true,
      "correctOption": 0,
      "explanation": "The post-exploitation phase aims to gather further data, maintain access, and assess the potential impact of the breach."
    }
  ]
}
```

**Key Point**: `correctOption` is the index (0, 1, 2, 3...) that corresponds to the position of `correct_answer` in the `options` array.

---

## Evaluation Logic (feedback-service)

### Current MCQTaskEvaluator (TODO - not implemented)

```java
@Component
@Slf4j
public class MCQTaskEvaluator implements TaskEvaluator {
    
    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.warn("MCQ evaluation not yet implemented");
        // Returns ERROR status
    }
}
```

### Proposed Implementation

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class MCQTaskEvaluator implements TaskEvaluator {
    
    private final ObjectMapper objectMapper;
    private final AIFeedbackClient aiFeedbackClient;  // For explanations via AI
    
    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.info("Evaluating MCQ task submission: {}", event.getSubmissionId());
        
        try {
            // 1. Extract answer and task content
            McqSubmissionAnswer answer = parseAnswer(event.getAnswer());
            McqTaskContent taskContent = parseTaskContent(event.getTaskContent());
            
            // 2. Evaluate each answer
            List<McqSubmissionFeedback.QuestionFeedback> feedbacks = new ArrayList<>();
            int totalCorrect = 0;
            
            for (McqSubmissionAnswer.QuestionAnswer qa : answer.getAnswers()) {
                McqTaskContent.Question question = findQuestion(taskContent, qa.getQuestionNumber());
                
                // Validate correct_answer is within valid range
                validateCorrectAnswer(question);
                
                // Get correct answer index (already stored as int)
                int correctOption = question.getCorrect_answer();
                boolean isCorrect = qa.getSelectedOption() == correctOption;
                
                if (isCorrect) totalCorrect++;
                
                // Use pre-generated explanation from task content
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
            
            // 3. Build feedback
            int totalQuestions = answer.getAnswers().size();
            double scorePercentage = (totalCorrect * 100.0) / totalQuestions;
            
            McqSubmissionFeedback feedback = new McqSubmissionFeedback(
                feedbacks,
                totalCorrect,
                totalQuestions,
                scorePercentage
            );
            
            // 4. Return evaluated event
            return Mono.just(SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score((int) scorePercentage)
                .isCorrect(totalCorrect == totalQuestions)  // Only true if ALL correct
                .feedbackType("MCQ")
                .detailedFeedback(objectMapper.writeValueAsString(feedback))
                .overallFeedback(String.format(
                    "You got %d out of %d questions correct (%.0f%%)",
                    totalCorrect, totalQuestions, scorePercentage
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCQ evaluation failed: {}", e.getMessage());
            return buildFallbackEvent(event);
        }
    }
    
    @Override
    public String getTaskType() {
        return "MCQ";
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
    
    // No need to parse - correct_answer is already an integer index!
    private void validateCorrectAnswer(McqTaskContent.Question question) {
        int correctOption = question.getCorrect_answer();
        int optionsCount = question.getOptions().size();
        
        if (correctOption < 0 || correctOption >= optionsCount) {
            log.error("Invalid correct_answer index {} for question {} (valid range: 0-{})",
                correctOption, question.getQuestion_number(), optionsCount - 1);
            throw new IllegalArgumentException(
                "Invalid correct_answer index for question: " + 
                question.getQuestion_number()
            );
        }
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

## API Response Example

### Submission Created (202 Accepted)
```json
{
  "success": true,
  "message": "Submission accepted for evaluation.",
  "data": {
    "id": "a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6",
    "status": "PENDING"
  }
}
```

### Submission Retrieved After Evaluation (200 OK)
```json
{
  "success": true,
  "message": "Submission retrieved successfully.",
  "data": {
    "id": "a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "taskId": "1335f072-03eb-4619-adea-4dff3d09cc90",
    "taskType": "MULTIPLE_CHOICE",
    "status": "COMPLETED",
    "isCorrect": false,
    "scoreEarned": 80,
    "submittedAt": "2025-11-23T21:00:00Z",
    "evaluatedAt": "2025-11-23T21:00:03Z",
    "answer": {
      "answers": [
        {"questionNumber": "1", "selectedOption": 1},
        {"questionNumber": "2", "selectedOption": 2},
        {"questionNumber": "3", "selectedOption": 0},
        {"questionNumber": "4", "selectedOption": 2},
        {"questionNumber": "5", "selectedOption": 2},
        {"questionNumber": "6", "selectedOption": 0},
        {"questionNumber": "7", "selectedOption": 1},
        {"questionNumber": "8", "selectedOption": 0},
        {"questionNumber": "9", "selectedOption": 1},
        {"questionNumber": "10", "selectedOption": 0}
      ]
    },
    "feedback": {
      "totalCorrect": 8,
      "totalQuestions": 10,
      "scorePercentage": 80.0,
      "feedbacks": [
        {
          "questionNumber": "1",
          "isCorrect": true,
          "correctOption": 1,
          "explanation": "Penetration testing is conducted to evaluate the security of a system by simulating an attack from malicious outsiders and insiders."
        },
        {
          "questionNumber": "2",
          "isCorrect": false,
          "correctOption": 3,
          "explanation": "Software Development Pentest is not a recognized type of pentesting. Common types include network, social engineering, and database pentests."
        },
        {
          "questionNumber": "3",
          "isCorrect": true,
          "correctOption": 0,
          "explanation": "Nmap is a widely used tool for network scanning, helping identify open ports and services on a network."
        },
        {
          "questionNumber": "4",
          "isCorrect": true,
          "correctOption": 2,
          "explanation": "Reconnaissance is the first phase where information gathering and planning for the pentest are performed."
        },
        {
          "questionNumber": "5",
          "isCorrect": true,
          "correctOption": 2,
          "explanation": "Authorization from relevant stakeholders is essential to ensure the legality and ethicality of the pentest."
        },
        {
          "questionNumber": "6",
          "isCorrect": true,
          "correctOption": 0,
          "explanation": "SQL Injection is a common technique used to exploit vulnerabilities in web applications by injecting malicious SQL code."
        },
        {
          "questionNumber": "7",
          "isCorrect": true,
          "correctOption": 1,
          "explanation": "A penetration testing report should include a summary of findings and recommendations to help stakeholders understand and mitigate risks."
        },
        {
          "questionNumber": "8",
          "isCorrect": true,
          "correctOption": 0,
          "explanation": "A vulnerability assessment identifies and reports vulnerabilities without exploiting them, whereas pentesting involves exploitation to assess impact."
        },
        {
          "questionNumber": "9",
          "isCorrect": true,
          "correctOption": 1,
          "explanation": "Social engineering involves manipulating people into divulging confidential information, such as passwords."
        },
        {
          "questionNumber": "10",
          "isCorrect": false,
          "correctOption": 0,
          "explanation": "The post-exploitation phase aims to gather further data, maintain access, and assess the potential impact of the breach."
        }
      ]
    }
  }
}
```

**Score calculation**: 8 out of 10 correct = 80% (scorePercentage). The `isCorrect` flag is `false` because not all questions were answered correctly.

---

## Validation

### Client-Side Validation
- All questions must have answers
- Selected option must be >= 0
- Selected option must be < number of options for that question

### Server-Side Validation

```java
@Component
public class McqSubmissionValidator {
    
    public void validate(
        McqSubmissionAnswer answer,
        McqTaskContent taskContent
    ) {
        if (answer.getAnswers() == null || answer.getAnswers().isEmpty()) {
            throw new ValidationException("At least one answer required");
        }
        
        if (answer.getAnswers().size() != taskContent.getQuestions().size()) {
            throw new ValidationException(
                String.format(
                    "Expected %d answers, got %d",
                    taskContent.getQuestions().size(),
                    answer.getAnswers().size()
                )
            );
        }
        
        for (McqSubmissionAnswer.QuestionAnswer qa : answer.getAnswers()) {
            McqTaskContent.Question question = taskContent.getQuestions().stream()
                .filter(q -> q.getQuestion_number().equals(qa.getQuestionNumber()))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                    "Invalid question number: " + qa.getQuestionNumber()
                ));
            
            if (qa.getSelectedOption() < 0 || 
                qa.getSelectedOption() >= question.getOptions().size()) {
                throw new ValidationException(
                    String.format(
                        "Invalid option %d for question %s (valid range: 0-%d)",
                        qa.getSelectedOption(),
                        qa.getQuestionNumber(),
                        question.getOptions().size() - 1
                    )
                );
            }
        }
    }
}
```

---

## Changes Summary

| Component | Current | Changes |
|-----------|---------|---------|
| `McqSubmissionAnswer` | Single `int selectedOption` | List of `QuestionAnswer` |
| `McqSubmissionFeedback` | Single correctness + explanation | List of per-question feedback + overall stats |
| `SubmissionController` | ✅ No changes | Uses polymorphic deserialization |
| `SubmissionService` | ✅ No changes | Already handles polymorphic types |
| `TaskSubmission` | ✅ No changes | JSONB already flexible |
| `MCQTaskEvaluator` | TODO (not implemented) | Full implementation required |
| Database | ✅ No schema changes | JSONB handles any structure |
| Validation | ✅ Can be added | New validator for MCQ answers |

---

## Edge Cases & Considerations

1. **Question Order**: Client can send answers in any order - we use `questionNumber` to map them
2. **Partial Answers**: Should entire submission be rejected if user didn't answer all questions?
   - Option A: Reject (strict) - currently implemented in validator
   - Option B: Accept partial (lenient) - treat unanswered as incorrect
3. **Score Calculation**: 
   - Currently: (totalCorrect / totalQuestions) * 100 = percentage
   - XP Reward: Should this be total task XP or sum of per-question XP?
4. **Time Tracking**: Should we track time per question or overall? Currently overall only
5. **Reattempts**: Can users retake the same MCQ task? No restriction currently
6. **Correct Answer Storage**: Must match exactly with an option string. If there's a mismatch, evaluation fails

## Example: Question 2 Evaluation

**Task Content (with correct_answer as index):**
```json
{
  "question_number": "2",
  "question_text": "Which of the following is NOT a common type of penetration test?",
  "options": [
    "Network Pentest",
    "Social Engineering Pentest", 
    "Database Pentest",
    "Software Development Pentest"
  ],
  "correct_answer": 3,
  "explanation": "Software Development Pentest is not a recognized type..."
}
```

**User Submits:**
```json
{
  "questionNumber": "2",
  "selectedOption": 2
}
```

**Evaluation (Simple):**
- User selected index: 2
- Correct answer index: 3
- Compare: 2 ≠ 3
- Result: **isCorrect = false**

**Feedback:**
```json
{
  "questionNumber": "2",
  "isCorrect": false,
  "correctOption": 3,
  "explanation": "Software Development Pentest is not a recognized type of pentesting..."
}
```

**Benefit**: Direct integer comparison, no string parsing needed!
