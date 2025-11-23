# MCQ Task Architecture Analysis

## Current Design: 1 Task = 1 Question

The current architecture is **intentionally designed for single questions per task**:

### Data Model Structure

**McqTaskContent** (single question):
```java
- question_number: String
- question_title: String
- question_description: String
- question_text: String          // Single question
- question_duration: int
- question_difficulty: String
- options: List<String>          // 4 options
- hint: String
- correct_answer: String
- explanation: String
- xpReward: int
```

**TaskSubmission** (1-to-1 mapping with Task):
```java
- id: UUID (submission record)
- userId: UUID
- taskId: UUID               // Points to ONE task
- answer: McqSubmissionAnswer
  - selectedOption: int      // Single answer
- feedback: McqSubmissionFeedback
- isCorrect: Boolean
- scoreEarned: Integer
```

### Current Flow

1. **Generation** (quantity=10):
   - OpenAI returns: `{ "expected_output": [ {q1}, {q2}, ..., {q10} ] }`
   - Loop creates **10 separate Task records**, each with 1 question
   - Each Task has unique ID

2. **User Submission**:
   - For each of 10 questions, user submits 1 TaskSubmission
   - Each submission references 1 Task by ID
   - Each submission stores 1 selected answer

3. **Evaluation**:
   - Feedback-service receives submission event
   - Evaluates against stored question in Task.content
   - Returns pass/fail + XP for that one question

### Why This Design?

**Advantages:**
- ✅ Simple 1-to-1 mapping between Task and Submission
- ✅ Granular tracking: Each question is independently trackable
- ✅ Flexible XP rewards: Can assign different XP per question
- ✅ Progress tracking: Can mark individual questions as complete
- ✅ Reusability: Same question (Task) can be assigned to multiple users
- ✅ Consistent with Coding/Essay tasks (1 task per submission)

**Disadvantages:**
- ❌ Creates many Task records (10 questions = 10 tasks)
- ❌ May feel fragmented in UI (10 separate entries instead of 1 quiz)

## Alternative Design: 1 Task = N Questions

If we wanted all 10 questions in 1 Task:

**Required Changes:**

1. **McqTaskContent** redesign:
```java
public class McqTaskContent implements TaskContent {
    private List<Question> questions;  // NEW: Array of questions
    
    @Data
    @Builder
    public static class Question {
        private String question_number;
        private String question_text;
        private List<String> options;
        private String correct_answer;
        // ... etc
    }
}
```

2. **McqSubmissionAnswer** redesign:
```java
public class McqSubmissionAnswer implements SubmissionAnswer {
    private List<Integer> selectedOptions;  // Multiple answers
}
```

3. **TaskSubmission** becomes 1-to-many:
   - 1 Task (10 questions)
   - 1 TaskSubmission with array of 10 answers
   - Total score = aggregate of all 10 questions

4. **Evaluation logic** changes:
   - Evaluate all 10 answers at once
   - Generate cumulative feedback
   - Single pass/fail for entire quiz

5. **Generation logic** changes:
   - Create 1 Task per call (not 10)
   - Store all questions in single Task.content

## Recommendation

**Current design (1 Task = 1 Question) is correct for this platform** because:

1. **Alignment with platform philosophy**: Other task types (Coding, Essay) follow 1 submission = 1 evaluation
2. **Granular progress tracking**: Users see 10 completed questions, not "1 quiz"
3. **Flexible difficulty**: Can mix EASY/MEDIUM/HARD questions in one "quiz set"
4. **User experience**: Progressive feedback after each question
5. **XP system**: Award XP per question answered

## Current Implementation Issue

The problem with current code is **not the architecture**—it's the **generation loop**:

**Current (Correct):**
```java
List<JsonNode> challengeNodes = parseMcqResponseToNodes(response);  // 10 questions
for (JsonNode challengeNode : challengeNodes) {
    Task task = createAndSaveMCQTask(...);  // Create 10 separate Task records
}
```

This is **working as designed**: 10 questions → 10 tasks.

## Summary

- ✅ **1 Task per MCQ question is the correct design**
- ✅ Consistent with coding/essay task models
- ✅ Matches the submission/evaluation pipeline
- ✅ Aligns with XP reward system
- ✅ Supports granular progress tracking
- The fix should focus on ensuring **all 10 tasks are properly persisted**, not restructuring the model
