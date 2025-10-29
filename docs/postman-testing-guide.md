# Postman Testing Guide - AI Task Generation & Evaluation

## Overview

This guide walks you through testing the complete AI-powered task generation, submission, and feedback flow using Postman.

---

## Prerequisites

### 1. **Services Running**

Start all required services:

```bash
# Terminal 1 - Config Server
cd skilltracker-infra/config-server
./mvnw spring-boot:run

# Terminal 2 - Discovery Server
cd skilltracker-infra/discovery-server
./mvnw spring-boot:run

# Terminal 3 - API Gateway
cd skilltracker-infra/api-gateway
./mvnw spring-boot:run

# Terminal 4 - User Service
cd skilltracker-services/user-service
./mvnw spring-boot:run

# Terminal 5 - Task Service
cd skilltracker-services/task-service
./mvnw spring-boot:run

# Terminal 6 - Feedback Service
cd skilltracker-services/feedback-service
./mvnw spring-boot:run

# Terminal 7 - RabbitMQ
docker-compose up rabbitmq

# Terminal 8 - Judge0
docker-compose up judge0
```

### 2. **Environment Variables Set**

Ensure you have:
```bash
export OPENAI_API_KEY=sk-your-actual-key
export JUDGE0_API_URL=http://localhost:2358
```

### 3. **Database Running**

```bash
docker-compose up postgres
```

---

## Testing Flow

The complete flow has **3 main steps**:

```
1. Generate AI Task → 2. Submit Solution → 3. Get Feedback
```

---

## Step 1: Authentication

### **Option A: OAuth2 Login (Recommended)**

1. **Open Browser:**
   ```
   http://localhost:8080/oauth2/authorization/google
   ```

2. **Complete Google/GitHub OAuth flow**

3. **Extract Cookie:**
   - Open Browser Dev Tools (F12)
   - Go to Application → Cookies → http://localhost:8080
   - Copy the `accessToken` value

4. **Use in Postman:**
   - Go to request → Cookies
   - Add cookie: `accessToken=<your-token-here>`

### **Option B: Direct Headers (Dev Only)**

If testing microservices directly (bypassing gateway):

```
Headers:
X-User-Id: 123e4567-e89b-12d3-a456-426614174000
X-User-Roles: USER,ADMIN
```

⚠️ **This only works if services are directly accessible (dev environment)**

---

## Step 2: Generate AI Task (Event-Based)

Task generation is **event-driven** via RabbitMQ, not exposed as REST endpoint.

### **Option A: Trigger via RabbitMQ Message (Recommended)**

Use RabbitMQ Management UI to publish a message:

1. **Access RabbitMQ UI:**
   ```
   http://localhost:15672
   Username: guest
   Password: guest
   ```

2. **Go to:** Queues → `task.generation.admin.q` → Publish Message

3. **Payload:**
   ```json
   {
     "taskType": "CODING",
     "skillName": "Java Programming",
     "difficulty": "MEDIUM",
     "topic": "implement a function to reverse a string",
     "languageName": "Java"
   }
   ```

4. **Properties:**
   ```
   content_type: application/json
   ```

5. **Click "Publish message"**

6. **Check Logs:**
   ```bash
   # In task-service terminal, you should see:
   [INFO] Received admin generation request for: Java Programming
   [INFO] Generating Coding tasks via OpenAI for skill: Java Programming
   [INFO] Successfully created coding task: <title> (ID: <uuid>)
   ```

### **Option B: Use Existing Tasks**

If tasks already exist in the database, skip generation and get a task ID:

**GET** `http://localhost:8080/api/v1/tasks/{id}`

```
Method: GET
URL: http://localhost:8080/api/v1/tasks/123e4567-e89b-12d3-a456-426614174000
Headers:
  Cookie: accessToken=<your-token>
```

**Response:**
```json
{
  "status": "success",
  "message": "Task retrieved successfully.",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "title": "Reverse a String",
    "description": "Implement a function that reverses a string...",
    "type": "CODING",
    "difficulty": "MEDIUM",
    "content": {
      "prompt": "Write a function...",
      "testCases": [
        {
          "input": "hello",
          "expectedOutput": "olleh",
          "isHidden": false
        }
      ]
    }
  }
}
```

---

## Step 3: Submit Your Solution

Once you have a task ID, submit your solution.

### **Endpoint:** POST `/api/v1/submissions`

**Request:**
```
Method: POST
URL: http://localhost:8080/api/v1/submissions
Headers:
  Cookie: accessToken=<your-token>
  Content-Type: application/json
```

**Body (for CODING task):**
```json
{
  "taskId": "123e4567-e89b-12d3-a456-426614174000",
  "answer": {
    "type": "CODING",
    "code": "public class Solution {\n    public static String reverse(String str) {\n        return new StringBuilder(str).reverse().toString();\n    }\n}",
    "languageId": 62
  }
}
```

**Language IDs (Judge0):**
- Java: 62
- Python: 71
- JavaScript: 63
- C++: 54
- C: 50

**Response:**
```json
{
  "status": "success",
  "message": "Submission accepted for evaluation.",
  "data": {
    "id": "submission-uuid",
    "status": "PENDING"
  }
}
```

---

## Step 4: Monitor Evaluation (Automatic)

The evaluation happens **automatically in the background**:

### **Event Flow:**

```
1. Submission Created
   ↓
2. Task-Service publishes SubmissionCreatedEvent to RabbitMQ
   ↓
3. Feedback-Service consumes event
   ↓
4. Feedback-Service executes code via Judge0
   ↓
5. Feedback-Service generates AI feedback via OpenAI
   ↓
6. Feedback-Service publishes SubmissionEvaluatedEvent
   ↓
7. Task-Service consumes event and updates submission
```

### **Monitor Logs:**

**Task-Service:**
```bash
[INFO] Submission received from authenticated user: <user-id>
[INFO] Publishing submission.created event for ID: <submission-id>
[INFO] Received submission.evaluated event for ID: <submission-id>
[INFO] Updated submission <submission-id> with evaluation results
```

**Feedback-Service:**
```bash
[INFO] Received submission from RabbitMQ: <submission-id>
[INFO] Handling submission for evaluation: <submission-id>
[INFO] Evaluating CODING task submission: <submission-id>
[INFO] Generating AI feedback for task: <task-id>
[INFO] Publishing submission.evaluated event for ID: <submission-id>
```

### **Monitor RabbitMQ:**

1. **Go to:** http://localhost:15672 → Queues

2. **Watch:**
   - `submission.created.q` - Should process message
   - `submission.evaluated.q` - Should receive result

---

## Step 5: Get Evaluation Results

### **Endpoint:** GET `/api/v1/submissions/{id}` (if endpoint exists)

Or check database directly:

```sql
SELECT * FROM task_submissions 
WHERE id = '<submission-id>' 
ORDER BY created_at DESC;
```

**Expected Fields:**
- `status`: "COMPLETED" or "ERROR"
- `score`: 0-100
- `is_correct`: true/false
- `feedback`: AI-generated feedback
- `evaluated_at`: timestamp

---

## Complete Postman Collection

### **Collection Structure:**

```
Skill Tracker API
├── 1. Authentication
│   └── Login (OAuth2 - use browser)
│
├── 2. Tasks
│   ├── GET Get Task by ID
│   └── (Generation via RabbitMQ)
│
└── 3. Submissions
    ├── POST Submit Solution
    └── GET Get Submission Result
```

### **1. Get Task by ID**

```
GET http://localhost:8080/api/v1/tasks/{{taskId}}

Headers:
Cookie: accessToken={{authToken}}
```

### **2. Submit Coding Solution**

```
POST http://localhost:8080/api/v1/submissions

Headers:
Cookie: accessToken={{authToken}}
Content-Type: application/json

Body:
{
  "taskId": "{{taskId}}",
  "answer": {
    "type": "CODING",
    "code": "public class Solution {\n    public static String reverse(String str) {\n        return new StringBuilder(str).reverse().toString();\n    }\n}",
    "languageId": 62
  }
}
```

### **Postman Variables:**

Create environment variables:
- `baseUrl`: `http://localhost:8080`
- `authToken`: `<your-access-token>`
- `taskId`: `<generated-task-id>`
- `submissionId`: `<submission-id>`

---

## Sample Test Cases

### **Test 1: Simple String Reversal (Java)**

**Code to Submit:**
```java
public class Solution {
    public static String reverse(String str) {
        return new StringBuilder(str).reverse().toString();
    }
}
```

**Expected Result:**
- Score: ~90-100
- Correctness: All tests pass
- Efficiency: O(n) - optimal
- Style: Clean and readable

### **Test 2: Failing Solution (Wrong Logic)**

**Code to Submit:**
```java
public class Solution {
    public static String reverse(String str) {
        return str; // Wrong - doesn't reverse
    }
}
```

**Expected Result:**
- Score: ~20-40
- Correctness: Tests fail
- Feedback: "Code doesn't reverse the string"

### **Test 3: Inefficient Solution**

**Code to Submit:**
```java
public class Solution {
    public static String reverse(String str) {
        String result = "";
        for (int i = str.length() - 1; i >= 0; i--) {
            result += str.charAt(i); // Inefficient string concatenation
        }
        return result;
    }
}
```

**Expected Result:**
- Score: ~70-80
- Correctness: Tests pass
- Efficiency: O(n²) - suboptimal
- Feedback: "Consider using StringBuilder for better performance"

---

## Troubleshooting

### **Issue: 401 Unauthorized**

**Cause:** No valid JWT token

**Solution:**
1. Complete OAuth2 login in browser
2. Extract `accessToken` cookie
3. Add to Postman request

### **Issue: 403 Forbidden**

**Cause:** Token valid but no SecurityContext

**Solution:**
- Ensure API Gateway is running
- Check that request goes through gateway (port 8080)
- Verify gateway adds `X-User-Id` and `X-User-Roles` headers

### **Issue: Submission Stuck in PENDING**

**Cause:** Feedback service not consuming events

**Solution:**
1. Check RabbitMQ is running
2. Check feedback-service logs
3. Verify queue `submission.created.q` has consumer
4. Check for errors in feedback-service

### **Issue: No AI Feedback Generated**

**Cause:** OpenAI API call failing

**Solution:**
1. Verify `OPENAI_API_KEY` is set
2. Check feedback-service logs for OpenAI errors
3. Verify `spring.ai.openai.base-url` is correct
4. Check if falling back to simple feedback

### **Issue: Judge0 Execution Failed**

**Cause:** Judge0 not running or wrong URL

**Solution:**
1. Start Judge0: `docker-compose up judge0`
2. Verify URL: `http://localhost:2358`
3. Test Judge0 directly:
   ```bash
   curl http://localhost:2358/about
   ```

---

## Expected Timing

- **Task Generation:** 5-15 seconds (AI generates task)
- **Submission Processing:** Immediate (returns PENDING)
- **Code Execution:** 2-5 seconds (Judge0)
- **AI Feedback:** 3-10 seconds (OpenAI)
- **Total Evaluation:** 5-15 seconds

---

## Monitoring Tools

### **RabbitMQ Management UI**
```
http://localhost:15672
Username: guest
Password: guest
```

**What to Monitor:**
- Queues → `submission.created.q` → Messages ready/delivered
- Queues → `submission.evaluated.q` → Messages ready/delivered
- Exchanges → `submission.exchange` → Message flow

### **Service Logs**

Watch logs in real-time:
```bash
# Task-Service
tail -f logs/task-service.log

# Feedback-Service
tail -f logs/feedback-service.log
```

### **Database**

Check submission status:
```sql
-- View all submissions
SELECT id, user_id, task_id, status, score, is_correct, created_at, evaluated_at
FROM task_submissions
ORDER BY created_at DESC
LIMIT 10;

-- View specific submission with feedback
SELECT id, status, score, is_correct, feedback
FROM task_submissions
WHERE id = '<submission-id>';
```

---

## Sample Postman Tests

Add to your Postman request tests:

```javascript
// Test 1: Submission accepted
pm.test("Submission accepted", function () {
    pm.response.to.have.status(202);
    pm.expect(pm.response.json().status).to.eql("success");
});

// Test 2: Submission ID returned
pm.test("Submission ID returned", function () {
    const response = pm.response.json();
    pm.expect(response.data.id).to.exist;
    pm.environment.set("submissionId", response.data.id);
});

// Test 3: Status is PENDING
pm.test("Status is PENDING", function () {
    const response = pm.response.json();
    pm.expect(response.data.status).to.eql("PENDING");
});
```

---

## Quick Start Commands

### **1. Login and Get Token**

```bash
# Open in browser
open http://localhost:8080/oauth2/authorization/google

# Extract cookie from dev tools
```

### **2. Test Task Retrieval**

```bash
curl -X GET "http://localhost:8080/api/v1/tasks/{taskId}" \
  --cookie "accessToken=<your-token>"
```

### **3. Submit Solution**

```bash
curl -X POST "http://localhost:8080/api/v1/submissions" \
  --cookie "accessToken=<your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "task-uuid",
    "answer": {
      "type": "CODING",
      "code": "public class Solution { ... }",
      "languageId": 62
    }
  }'
```

---

## What Success Looks Like

### **1. Task Retrieved:**
```json
{
  "status": "success",
  "data": {
    "id": "...",
    "title": "Reverse a String",
    "type": "CODING",
    "content": { "testCases": [...] }
  }
}
```

### **2. Submission Accepted:**
```json
{
  "status": "success",
  "message": "Submission accepted for evaluation.",
  "data": {
    "id": "submission-uuid",
    "status": "PENDING"
  }
}
```

### **3. Task-Service Logs:**
```
[INFO] Submission received from authenticated user: user-uuid
[INFO] Publishing submission.created event for ID: submission-uuid
```

### **4. Feedback-Service Logs:**
```
[INFO] Received submission from RabbitMQ: submission-uuid
[INFO] Registered task evaluators: [CODING, MCQ, ESSAY]
[INFO] Evaluating CODING task submission: submission-uuid
[INFO] Generating detailed AI feedback for task: task-uuid
[INFO] Publishing submission.evaluated event for ID: submission-uuid
```

### **5. Task-Service Logs (After Evaluation):**
```
[INFO] Received submission.evaluated event for ID: submission-uuid
[INFO] Updated submission with evaluation results
```

### **6. Database Updated:**
```sql
task_submissions:
- status: COMPLETED
- score: 85
- is_correct: true
- feedback: "Correctness: ...\nEfficiency: ...\nStyle: ..."
- evaluated_at: 2025-10-29 17:05:00
```

---

## Debugging Tips

### **Check Event Flow:**

```bash
# Verify event was published
# In task-service logs, look for:
"Publishing submission.created event"

# Verify event was consumed
# In feedback-service logs, look for:
"Received submission from RabbitMQ"

# Verify result was published
# In feedback-service logs, look for:
"Publishing submission.evaluated event"

# Verify result was consumed
# In task-service logs, look for:
"Received submission.evaluated event"
```

### **Check RabbitMQ Queues:**

```
submission.created.q:
- Ready: 0 (all processed)
- Total: X (messages processed)

submission.evaluated.q:
- Ready: 0 (all processed)
- Total: X (messages processed)
```

### **Check Judge0:**

```bash
curl http://localhost:2358/about

# Should return Judge0 version info
```

### **Check OpenAI:**

Look for in feedback-service logs:
```
[INFO] Generating detailed AI feedback for task: <task-id>
```

If you see errors:
```
[ERROR] Failed to generate AI feedback: 401 Unauthorized
```
→ Check `OPENAI_API_KEY` environment variable

---

## Next Steps

After successful test:

1. ✅ **Test with different code solutions** (correct, incorrect, inefficient)
2. ✅ **Test with different programming languages** (change `languageId`)
3. ✅ **Test error scenarios** (invalid code, syntax errors)
4. ✅ **Test authorization** (remove cookie, expect 401)
5. ✅ **Monitor performance** (check evaluation time)

---

## Summary

**Testing Checklist:**
- [ ] All services running
- [ ] OAuth2 login completed
- [ ] Access token extracted
- [ ] Task retrieved successfully
- [ ] Solution submitted
- [ ] Event flow working (check logs)
- [ ] Evaluation completed
- [ ] Feedback generated
- [ ] Database updated

**Happy Testing!** 🚀
