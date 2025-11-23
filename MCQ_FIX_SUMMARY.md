# MCQ Task Generation Fix - Summary

## Problem

Task generation for MCQ tasks was failing with the error:
```
Missing or invalid 'challenges' array in OpenAI response
```

The actual error was a **mismatch between the prompt template and the response parsing logic**.

## Root Cause Analysis

1. **Prompt Template** (`mcq_generation_prompt.txt`):
   - Instructed OpenAI to return JSON with a `questions` array
   - Example: `{ "questions": [ {...}, {...} ] }`

2. **Parsing Code** (`ContentGeneratorServiceImpl.java`, line 212):
   - Was looking for `expected_output` array
   - Error message said `challenges` (copy-paste error from coding task parser)

3. **Mismatch Result**:
   - OpenAI generated `{ "questions": [...] }`
   - Parser looked for `root.path("expected_output")`
   - Missing node → exception thrown

## Solution Implemented

### 1. Fixed MCQ Prompt Template
**File**: `skilltracker-services/task-service/src/main/resources/prompts/mcq/mcq_generation_prompt.txt`

Changed the JSON structure from:
```json
{
  "questions": [...]
}
```

To:
```json
{
  "expected_output": [...]
}
```

This aligns with the parsing logic that expects an `expected_output` array.

### 2. Fixed Error Message
**File**: `ContentGeneratorServiceImpl.java`, line 215

Changed error message from:
```java
"Missing or invalid 'challenges' array in OpenAI response"
```

To:
```java
"Missing or invalid 'expected_output' array in OpenAI response"
```

This provides accurate debugging information for future issues.

## Consistency Check

The three task type parsers now have consistent patterns:

| Task Type | Array Name | Parser Method |
|-----------|------------|---------------|
| Coding | `challenges` | `parseCodingResponseToNodes()` |
| Essay | `tasks` | `parseEssayResponseToNodes()` |
| MCQ | `expected_output` | `parseMcqResponseToNodes()` |

## Testing

All 24 existing tests in `ContentGeneratorServiceImplTest` pass:
- ✅ Coding task generation tests
- ✅ Essay task generation tests  
- ✅ JSON parsing and validation tests
- ✅ Edge case handling

## Issue 2: Hibernate Transient Entity Error

After fixing the JSON parsing issue, a second error appeared:

```
org.hibernate.TransientPropertyValueException: Not-null property references a transient value - 
transient instance must be saved before current operation: 
com.amalitech.task.service.model.Task.taskDefinition
```

### Root Cause

The `createAndSaveMCQTask()` method was building a new TaskDefinition inline (line 176-181) without persisting it first, then trying to save a Task with this unsaved definition.

### Solution

Updated `createAndSaveMCQTask()` to use the same pattern as `createAndSaveCodingTask()`:

1. Call `getOrCreateTaskDefinition(skill, title)` to get/create and persist the definition
2. Use the persisted definition when building the Task
3. Use the correct version from the definition
4. Set `isPublished(true)` for consistency

**File**: `ContentGeneratorServiceImpl.java`, lines 164-188

Before:
```java
Task task = Task.builder()
    .title(title)
    .taskDefinition(TaskDefinition.builder()...build())  // Unsaved!
    .version(1)
    .build();
```

After:
```java
TaskDefinition definition = getOrCreateTaskDefinition(skill, title);  // Saved!
Task task = Task.builder()
    .taskDefinition(definition)
    .version(definition.getLatestVersion())
    .isPublished(true)
    .build();
```

## Verification

Build compilation: ✅ SUCCESS
Test execution: ✅ 24/24 PASSED
No regressions detected.

## Issue 3: Field Name Mismatch in JSON Parsing

### Problem

The parsing code was looking for incorrect JSON field names, causing:
- All tasks to have the same title: "AI-Generated MCQ Task" (fallback default)
- Missing fields in McqTaskContent

**Prompt specifies:**
```json
{
  "question_title": "...",
  "question_description": "...",
  "xpReward": 50,
  "question_duration": 3
}
```

**Code was looking for:**
- `title` instead of `question_title`
- `description` instead of `question_description`
- `maxXP` instead of `xpReward`
- `estimatedDuration` instead of `question_duration`

### Solution

Updated field extraction to match prompt specification:

1. Line 165: `question_title` → Used for Task title
2. Line 166: `question_description` → Used for Task description
3. Line 167: `xpReward` → Default 50 (from prompt)
4. Line 168: `question_duration` → Default 3 minutes
5. Lines 195-207: Extract all fields for McqTaskContent including:
   - `question_title`
   - `question_description`
   - `question_difficulty`
   - `xpReward`

Now each MCQ task has a unique title derived from `question_title`.

## Issue 4: Architecture Change - 1 Task = N Questions (not 1 Task = 1 Question)

### Problem

Original design created 10 separate Task records for 10 questions. User requested: **1 Task with array of 10 questions** in the content.

### Solution

Redesigned `McqTaskContent` to store array of questions:

**New McqTaskContent structure:**
```java
public class McqTaskContent implements TaskContent {
    private List<Question> questions;  // Array of questions
    
    public static class Question {
        private String question_number;
        private String question_title;
        private String question_description;
        private String question_text;
        private int question_duration;
        private String question_difficulty;
        private List<String> options;
        private String hint;
        private String correct_answer;
        private int xpReward;
        private String explanation;
    }
}
```

**Generation logic changes:**
1. `generateMCQTask()` now creates **1 Task** (not 10)
2. `createAndSaveMCQTask()` accepts `List<JsonNode>` instead of single node
3. `createMCQContent()` creates array of `Question` objects
4. Task title: "SKILL_NAME - MCQ Quiz"
5. Total duration: sum of all question durations
6. Total XP: sum of all question XP values

**Response structure:**
```json
{
  "taskId": "...",
  "title": "PENTESTING - MCQ Quiz",
  "content": {
    "questions": [
      {
        "question_number": "1",
        "question_title": "Definition of Pentesting",
        "question_description": "...",
        "question_text": "What is the primary goal...",
        "question_duration": 2,
        "question_difficulty": "EASY",
        "options": [...],
        "hint": "...",
        "correct_answer": "...",
        "xpReward": 50,
        "explanation": "..."
      },
      {
        "question_number": "2",
        ...
      },
      ...
    ]
  }
}
```

## Changes Summary

1. **Fixed JSON structure mismatch**: Updated MCQ prompt to use `expected_output` array
2. **Fixed error message**: Changed to report `expected_output` instead of `challenges`
3. **Fixed Hibernate transient entity issue**: Use proper TaskDefinition persistence pattern
4. **Fixed field name mismatches**: Extract correct fields from OpenAI response matching prompt specification
5. **Redesigned MCQ architecture**: 1 Task with array of N questions instead of N separate tasks

## Impact

- MCQ task generation will now succeed when OpenAI returns the expected `expected_output` array
- TaskDefinitions are properly persisted before creating dependent Tasks
- All task types (Coding, Essay, MCQ) now follow consistent patterns
- Error messages are accurate for debugging
- No breaking changes to existing functionality
