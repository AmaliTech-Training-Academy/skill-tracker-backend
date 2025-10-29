# AGENTS.md - Guidelines for AI Agents

This file contains instructions and conventions for AI agents working on the skill-tracker-backend project.

---

## Git Workflow & Strategy

### Branch Strategy

**Main Branches:**
- `dev` - Main development branch (always deployable)
- `feature/*` - Feature branches for new work

### Workflow: Rebase-Based Development

#### 1. Start a New Feature Branch

Always start from the latest `dev` branch:

```bash
git checkout dev
git pull origin dev
git checkout -b feature/amazing-feature
```

#### 2. Work and Commit Regularly

Make small, focused commits using conventional commit style:

```bash
git add .
git commit -m "feat(service): add new feature"

git add .
git commit -m "fix(controller): resolve validation bug"
```

**Conventional Commit Format:**
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `style`: Code style changes (formatting)

**Scopes:**
- Service names: `task-service`, `feedback-service`, `user-service`, etc.
- Layers: `controller`, `service`, `repository`, `config`, `security`
- Infrastructure: `gateway`, `build`, `docker`

#### 3. Periodically Rebase onto Dev

Integrate latest changes from `dev` into your branch:

```bash
# While on your feature branch
git fetch origin dev
git rebase origin/dev
```

**What this does:**
1. Saves your commits temporarily
2. Fast-forwards your branch to latest `dev`
3. Replays your commits on top
4. Results in linear history (no merge commits)

#### 4. Resolve Rebase Conflicts

When conflicts occur:

```bash
# 1. Resolve conflicts in your files
# 2. Stage resolved files
git add .

# 3. Continue rebase
git rebase --continue

# If it gets messy, abort and try again
git rebase --abort
```

#### 5. Push and Create Pull Request

When feature is ready:

```bash
# Final rebase
git fetch origin dev
git rebase origin/dev

# Push (use --force-with-lease after rebase)
git push origin feature/amazing-feature --force-with-lease

# Create Pull Request on GitHub for code review
```

#### 6. After PR Approval

```bash
# Switch to dev and fast-forward merge
git checkout dev
git merge feature/amazing-feature
git push origin dev

# Clean up
git branch -d feature/amazing-feature
```

---

## Build Commands

### Compile Specific Service

```bash
./mvnw clean compile -pl skilltracker-services/task-service -am
./mvnw clean compile -pl skilltracker-services/feedback-service -am
```

### Build All Services

```bash
./mvnw clean install
```

### Run Tests

```bash
./mvnw test -pl skilltracker-services/task-service
```

---

## Code Conventions

### Java Code Style

- Use Lombok annotations (`@Data`, `@Builder`, `@RequiredArgsConstructor`)
- Follow existing patterns in the codebase
- No comments unless code is complex or user requests them
- Use consistent naming conventions with existing code

### Spring Boot Patterns

- Use constructor injection (via `@RequiredArgsConstructor`)
- Externalize prompts to template files (no prompts in code)
- Use Spring AI `ChatClient` for AI integration
- Follow reactive patterns with `Mono`/`Flux` where appropriate

### Security Patterns

- Use `@PreAuthorize` for method-level authorization
- Trust headers from API Gateway (`X-User-Id`, `X-User-Roles`)
- Never expose microservices directly (always via gateway)
- Use `SecurityContextHolder` to get current user

---

## Documentation

### Location

All documentation goes in `/docs` directory:

```
docs/
├── security-architecture.md
├── authorization-enforcement.md
├── feedback-service-*.md
├── task-service-*.md
└── git-commit-instructions.md
```

### Style

- Use Markdown
- Include code examples
- Add diagrams where helpful (Mermaid)
- Link to relevant files with file:// URLs
- Keep documentation up-to-date with code changes

---

## AI Prompts

### Externalization Rule

**NEVER hardcode prompts in Java code.**

All AI prompts must be:
1. Stored in `.txt` files under `src/main/resources/prompts/`
2. Loaded via Spring AI `PromptTemplate`
3. Configured in `PromptTemplateConfig.java`

**Example:**

```java
@Configuration
public class PromptTemplateConfig {
    @Bean
    public PromptTemplate myPromptTemplate(
        @Value("classpath:prompts/my_prompt.txt") Resource resource
    ) {
        return new PromptTemplate(resource);
    }
}
```

**Template Syntax:**
Use `{variable}` placeholders (not `%s` or other formats)

---

## Service-Specific Notes

### Task-Service

- Generates tasks via AI using Spring AI
- Uses `@PreAuthorize("isAuthenticated()")` on endpoints
- Publishes `SubmissionCreatedEvent` to RabbitMQ
- Consumes `SubmissionEvaluatedEvent` from RabbitMQ

### Feedback-Service

- Event-driven (no REST controllers)
- Consumes `SubmissionCreatedEvent` from RabbitMQ
- Executes code via Judge0
- Generates AI feedback via OpenAI (Spring AI)
- Publishes `SubmissionEvaluatedEvent` back to task-service
- Uses Strategy Pattern for different task types (CODING, MCQ, ESSAY)

### User-Service

**⚠️ CRITICAL: DO NOT MODIFY USER-SERVICE CODE**
- Handles OAuth2 authentication
- Generates JWT tokens
- Should not be modified without explicit permission

---

## Security Notes

### Architecture

- **API Gateway**: Validates JWT, adds headers (`X-User-Id`, `X-User-Roles`)
- **Microservices**: Trust headers, use `@PreAuthorize` for authorization
- **Event Consumers**: No authorization needed (internal only)

### Available Roles

```java
public enum Role {
    USER,   // Regular authenticated users
    ADMIN   // Administrative users
}
```

### Authorization Annotations

```java
@PreAuthorize("isAuthenticated()")           // Any authenticated user
@PreAuthorize("hasRole('ADMIN')")           // Admin only
@PreAuthorize("hasRole('USER')")            // Users only
@PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Either role
```

---

## Important Reminders for AI Agents

1. ✅ **Always use conventional commits**
2. ✅ **Follow the rebase workflow** (no merge commits)
3. ✅ **Run build verification** before committing
4. ✅ **Externalize AI prompts** to template files
5. ✅ **Put documentation in /docs** directory
6. ✅ **Never modify user-service** without permission
7. ✅ **Use `@PreAuthorize`** on all REST endpoints
8. ✅ **Use Spring AI** for AI integration (not raw WebClient)
9. ✅ **Follow strategy pattern** for task evaluators in feedback-service
10. ✅ **Test compilation** after changes

---

## Configuration Management

Services use **Spring Cloud Config Server**:

- Configuration stored in separate git repository
- Properties files: `task-service.properties`, `feedback-service.properties`
- Environment-specific overrides possible

---

## Quick Reference

### Current Branch
```bash
feature/global-filters
```

### Push Changes
```bash
git push origin feature/amazing-feature --force-with-lease
```

### Verify Build
```bash
./mvnw clean compile -pl skilltracker-services/feedback-service -am
```

---

For detailed git workflow, see [docs/git-commit-instructions.md](docs/git-commit-instructions.md)
