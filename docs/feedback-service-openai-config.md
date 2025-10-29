# OpenAI Configuration for Feedback Service

## Overview

This service now uses **Spring AI** framework for OpenAI integration, consistent with the task-service implementation.

## Configuration Properties

Add to your configuration server (`feedback-service.properties`):

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/${POSTGRES_DB}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}

# OpenAI Configuration (via Spring AI)
spring.ai.openai.base-url=https://ai-api.amalitech.org/api/v2
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7

# Retry Configuration
spring.ai.retry.on-http-codes=429,500,502,503,504
spring.ai.retry.max-attempts=5
spring.ai.retry.backoff.initial-interval=2s
spring.ai.retry.backoff.multiplier=2

# Judge0 API Configuration
client.judge0-api.base-url=${JUDGE0_API_URL:http://localhost:2358}
client.judge0-api.response-timeout-ms=30000
client.connect-timeout-ms=5000

# RabbitMQ Configuration
spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USER:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
```

## Environment Variables

Set the following environment variable:

```bash
export OPENAI_API_KEY=sk-your-actual-api-key-here
```

## Spring AI Benefits

✅ **Automatic Retry Logic** - Built-in retry for transient failures  
✅ **Structured Output** - Easy JSON parsing with type safety  
✅ **Consistent Configuration** - Same as task-service  
✅ **Better Error Handling** - Framework handles common error cases  
✅ **Model Flexibility** - Easy to switch between models

## Model Options

Configure via `spring.ai.openai.chat.options.model`:
- `gpt-4o-mini` (default - fast and cost-effective)
- `gpt-4o` (most capable)
- `gpt-4-turbo` (balanced performance)
- `gpt-3.5-turbo` (fastest, cheapest)

## Migration Summary

### What Changed:
1. ✅ Added `spring-ai-starter-model-openai` dependency
2. ✅ Refactored `AIFeedbackClient` to use `ChatClient` instead of raw `WebClient`
3. ✅ Removed custom `OpenAIRequest` and `OpenAIResponse` DTOs
4. ✅ Removed `openAiApiWebClient` bean from `WebClientConfig`
5. ✅ Switched from custom properties (`client.openai-api.*`) to Spring AI properties (`spring.ai.openai.*`)

### What Stayed the Same:
- Task evaluation flow (Strategy pattern)
- Judge0 integration
- Event-driven architecture
- Reactive processing with Project Reactor
