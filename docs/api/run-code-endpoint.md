# Execute Code and Run Tests

## Run Code Synchronously

**Summary:**

| Method | POST |
|--------|------|
| Path | /api/v1/submissions/run-code |
| Description | Executes user-submitted code against all test cases for a coding task and returns immediate results with test outcomes. |

**Detailed Explanation:**

This endpoint provides synchronous code execution for immediate feedback. The user's source code is submitted along with a language identifier, then executed against all test cases defined for the task. The endpoint compares actual output against expected output with normalized whitespace handling. All test cases are executed in parallel (up to 5 concurrent executions) to minimize latency. Results include individual test outcomes, execution metrics (time, memory), and aggregated statistics. Requires active authentication via Authorization header or JWT cookie.

| Path Parameters: | None. |
| Query Parameters: | None. |

**Sample Request Body:**

Example for a Python CODING task.

```json
{
  "taskId": "d38b4e34-acbf-4dc2-9000-eb62b76ca12e",
  "code": "def count_vowels(s):\n    vowels = 'aeiouAEIOU'\n    return sum(1 for char in s if char in vowels)",
  "languageId": 71
}
```

**Parameter Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| taskId | UUID | Yes | The unique identifier of the coding task. |
| code | String | Yes | The user-submitted source code as a string. Line breaks represented as `\n`. |
| languageId | Integer | Yes | Judge0 language identifier (e.g., 71 for Python 3, 62 for Java, 63 for JavaScript). |

**Example JSON Responses:**

**Success (200 OK):**

Code executed successfully with test results.

```json
{
  "message": "Code executed successfully",
  "data": {
    "testResults": [
      {
        "passed": true,
        "input": "\"hello\"",
        "expectedOutput": "2",
        "actualOutput": "2",
        "executionTimeMs": 17,
        "memoryUsedKb": 8192,
        "statusDescription": "Accepted"
      },
      {
        "passed": true,
        "input": "\"HELLO\"",
        "expectedOutput": "2",
        "actualOutput": "2",
        "executionTimeMs": 18,
        "memoryUsedKb": 8192,
        "statusDescription": "Accepted"
      },
      {
        "passed": false,
        "input": "\"bcdfg\"",
        "expectedOutput": "0",
        "actualOutput": "5",
        "executionTimeMs": 19,
        "memoryUsedKb": 8192,
        "statusDescription": "Wrong Answer"
      }
    ],
    "allTestsPassed": false,
    "testsPassed": 2,
    "testsTotal": 3,
    "stdout": "2",
    "stderr": null,
    "avgExecutionTimeMs": 18.0,
    "avgMemoryUsedKb": 8192
  },
  "metadata": {
    "traceId": "...",
    "timestamp": "2025-11-18T04:12:21Z"
  }
}
```

**Response Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| testResults | Array | List of individual test case execution results. |
| testResults[].passed | Boolean | Whether the test case passed (output matched expected). |
| testResults[].input | String | The input provided to the test case. |
| testResults[].expectedOutput | String | The expected output (normalized). |
| testResults[].actualOutput | String | The actual output from code execution (normalized). |
| testResults[].executionTimeMs | Long | Execution time in milliseconds. |
| testResults[].memoryUsedKb | Integer | Memory used in kilobytes. |
| testResults[].statusDescription | String | Judge0 status description (e.g., "Accepted", "Wrong Answer", "Runtime Error"). |
| allTestsPassed | Boolean | Whether all test cases passed. |
| testsPassed | Integer | Number of passing test cases. |
| testsTotal | Integer | Total number of test cases executed. |
| stdout | String | Standard output from the first test case execution. |
| stderr | String | Standard error from the first test case execution (if any). |
| avgExecutionTimeMs | Double | Average execution time across all test cases. |
| avgMemoryUsedKb | Integer | Average memory usage across all test cases. |

**Error (401 Unauthorized - Missing Authentication):**

No valid authentication token provided.

```json
{
  "status": 401,
  "message": "Unauthorized",
  "detail": "Authentication required. Please provide a valid JWT token in the Authorization header or as an accessToken cookie.",
  "instance": "/api/v1/submissions/run-code",
  "errors": null,
  "metadata": {
    "traceId": "...",
    "timestamp": "2025-11-18T04:12:21Z"
  }
}
```

**Error (400 Bad Request - Invalid Request):**

Missing or invalid request parameters.

```json
{
  "status": 400,
  "message": "Validation failed.",
  "detail": "The request body is missing or invalid.",
  "instance": "/api/v1/submissions/run-code",
  "errors": [
    {
      "field": "taskId",
      "message": "Task ID is required"
    },
    {
      "field": "code",
      "message": "Code cannot be empty"
    },
    {
      "field": "languageId",
      "message": "Language ID is required"
    }
  ],
  "metadata": {
    "traceId": "...",
    "timestamp": "2025-11-18T04:12:21Z"
  }
}
```

**Error (404 Not Found - Task Not Found):**

The specified task does not exist.

```json
{
  "status": 404,
  "message": "Not Found",
  "detail": "Task not found with id: d38b4e34-acbf-4dc2-9000-eb62b76ca12e",
  "instance": "/api/v1/submissions/run-code",
  "errors": null,
  "metadata": {
    "traceId": "...",
    "timestamp": "2025-11-18T04:12:21Z"
  }
}
```

**Error (400 Bad Request - Not a Coding Task):**

The task is not a coding task (e.g., MCQ or Essay).

```json
{
  "status": 400,
  "message": "Bad Request",
  "detail": "Task is not a coding task",
  "instance": "/api/v1/submissions/run-code",
  "errors": null,
  "metadata": {
    "traceId": "...",
    "timestamp": "2025-11-18T04:12:21Z"
  }
}
```

**Error (502 Bad Gateway - Judge0 Service Unavailable):**

Judge0 service is unreachable or returned an error.

```json
{
  "status": 502,
  "message": "Service Unavailable",
  "detail": "Code execution service unavailable. Please try again later.",
  "instance": "/api/v1/submissions/run-code",
  "errors": null,
  "metadata": {
    "traceId": "...",
    "timestamp": "2025-11-18T04:12:21Z"
  }
}
```

**Behavior Notes:**

- **Synchronous Execution:** Unlike submission evaluation, this endpoint executes code synchronously and returns results immediately.
- **Parallel Test Execution:** All test cases are executed in parallel with a concurrency limit of 5 to minimize total execution time.
- **Output Normalization:** Both expected and actual outputs are normalized (whitespace trimmed, line endings standardized) before comparison to handle platform differences.
- **Thread Context:** Response processing occurs on a separate thread pool; authentication context is preserved via headers.
- **Performance:** Typical response time is 1-3 seconds for 5 test cases on Judge0's infrastructure.

**Judge0 Language IDs Reference:**

| Language | ID |
|----------|-----|
| Python 3.8+ | 71 |
| Java | 62 |
| JavaScript (Node.js) | 63 |
| C++ | 54 |
| Go | 60 |
| Rust | 73 |
| TypeScript | 74 |

For a complete list, refer to the Judge0 API documentation.
