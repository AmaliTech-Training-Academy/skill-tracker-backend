# Skill Tracker Backend

[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/technologies/javase/jdk21-archive.html)
[![Spring Boot 3.5.6](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Build-Maven-blue)](https://maven.apache.org/)
[![Docker & Compose](https://img.shields.io/badge/Docker-Compose-2496ED)](https://www.docker.com/)
[![RabbitMQ](https://img.shields.io/badge/Messaging-RabbitMQ-FF6600)](https://www.rabbitmq.com/)

---

## Project Overview

**SkillBoost** is a comprehensive microservices-based platform designed to facilitate skill tracking, task management, and user feedback in an educational environment. Built on Spring Boot 3.5.6 with Java 21, the platform employs an event-driven architecture leveraging RabbitMQ for inter-service communication and includes a Backend-for-Frontend (BFF) pattern for optimized client integration.

The system manages user authentication, task generation powered by AI (Spring AI), automated code evaluation through Judge0, feedback generation, analytics, gamification, notifications, and payment processing—all orchestrated through a central API Gateway with service discovery.

---

## Architecture

### Microservices

The platform is composed of **9 independent microservices**:

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 8080 | Central entry point; routes requests to services; validates JWT tokens |
| **User Service** | 8084 | Authentication (OAuth2), JWT token generation, user profile management |
| **Task Service** | 8085 | Task generation via Spring AI; submission management |
| **Analytics Service** | 8086 | User progress tracking, performance metrics, skill development analytics |
| **Feedback Service** | 8090 | Event-driven feedback generation; code evaluation via Judge0 |
| **BFF Service** | 8083 | Backend-for-Frontend; client request optimization |
| **Gamification Service** | 8088 | Points, badges, leaderboards, achievement tracking |
| **Practice Service** | 8089 | Practice session management, exercise recommendations |
| **Notification Service** | 8091 | Async notifications via RabbitMQ; email/push delivery |
| **Payment Service** | 8087 | Payment processing, subscription management |

### Infrastructure Components

| Component | Port | Purpose |
|-----------|------|---------|
| **Config Server** | 8081 | Centralized configuration management via Git |
| **Discovery Server (Eureka)** | 8082 | Service registry and discovery |
| **PostgreSQL** | 5432 | Primary relational database |
| **MongoDB** | 27017 | Document store for notifications & audit logs |
| **Redis** | 6379 | Distributed caching layer |
| **RabbitMQ (AMQP)** | 5672 | Message broker for event-driven communication |
| **RabbitMQ Management UI** | 15672 | RabbitMQ admin console |
| **Ngrok** | 4040 | HTTP tunneling (development only) |

### Data Flow & Event Architecture

```
Task Generation Flow:
┌─────────────────┐
│  Task Service   │ (generates tasks via Spring AI)
└────────┬────────┘
         │ publishes SubmissionCreatedEvent
         ↓
    [RabbitMQ]
         │ consumed by
         ↓
┌─────────────────────────┐
│  Feedback Service       │ (event-driven, no REST endpoints)
│  - Executes code (Judge0)
│  - Generates feedback (OpenAI via Spring AI)
└────────┬────────────────┘
         │ publishes SubmissionEvaluatedEvent
         ↓
    [RabbitMQ]
         │ consumed by
         ↓
┌─────────────────────┐
│  Task Service       │ (updates submission status)
└─────────────────────┘
```

**Key Architectural Patterns:**
- **Microservices**: Independent, loosely coupled services
- **Event-Driven**: Asynchronous communication via RabbitMQ
- **Backend-for-Frontend (BFF)**: Dedicated BFF Service for optimized client interactions
- **Service Discovery**: Eureka-based dynamic service registration
- **Centralized Configuration**: Spring Cloud Config Server with Git-backed configuration
- **API Gateway**: Single entry point with JWT validation and routing
- **Distributed Caching**: Redis for session and query caching
- **Multi-Database**: PostgreSQL for relational data, MongoDB for notifications/audit

---

## Prerequisites

- **Java 21** (or compatible JDK)
- **Docker** and **Docker Compose** (latest versions)
- **Make** (essential for command execution)
- **Git** (for cloning the repository)
- **Maven 3.8+** (bundled as `./mvnw`)

### Required Environment Variables

Create a `.env` file in the project root from `.env.example`:

```bash
cp .env.example .env
# Edit .env and fill in your configuration values
```

For a complete list of all environment variables and their descriptions, see `.env.example`.

---

## Quick Start

### 1. Clone & Environment Setup

```bash
# Clone repository
git clone ssh://git@ssh.github.com:443/AmaliTech-Training-Academy/skill-tracker-backend.git
cd skill-tracker-backend

# Create environment file from template
cp .env.example .env

# Edit .env and configure required values
# At minimum, set database credentials and API keys
```

### 2. Build the Project

**Build All Modules (Maven):**
```bash
make build
```

**Build Docker Images:**
```bash
# Build infrastructure images
make build-infra

# Build all microservice images
make build-services

# Build everything
make build-all
```

### 3. Run the Stack

#### Option A: Docker Containers (Recommended for Production/Demo)

```bash
# Start everything (infra + services)
make start-all

# Check service health
curl http://localhost:8080/actuator/health
```

**To stop containers:**
```bash
make stop-all      # Stop without removing
make down-all      # Stop and remove containers
make reset-all     # Hard reset (removes volumes)
```

#### Option B: Local Development (Spring Boot CLI)

```bash
# Start infrastructure services
make run-infra

# In separate terminals, start individual services
make run SERVICE=user
make run SERVICE=task
make run SERVICE=feedback
# ... etc for other services
```

### 4. Database Migrations

Run Flyway migrations after services are started:

```bash
# Migrate all services
make migrate

# Migrate a specific service
make migrate-service SERVICE=task
```

---

## Development Workflow

### Running a Single Service

```bash
# Start & run a service with Spring Boot Maven plugin
make run SERVICE=task

# Clean, build, and run a service
make rebuild SERVICE=feedback
```

### Viewing Logs

```bash
# Local development (Spring Boot)
make logs SERVICE=user

# Docker containers
make dkr-logs
```

### Running Tests

```bash
# Run all tests
make test

# Test a specific service
make test-service SERVICE=task

# Test infrastructure component
make test-infra COMPONENT=config-server

# Test common module
make test-common
```

### Building a Specific Service Image

```bash
make dkr-build SERVICE=user
make dkr-run SERVICE=user
```

---

## Accessing Services

### API Gateway (Main Entry Point)
```
http://localhost:8080
```

### Service Discovery (Eureka UI)
```
http://localhost:8082/eureka
```

### Config Server
```
http://localhost:8081
```

### RabbitMQ Management Console
```
http://localhost:15672
# Default: guest / guest
```

### Individual Service Health Checks
```bash
curl http://localhost:8084/actuator/health  # User Service
curl http://localhost:8085/actuator/health  # Task Service
curl http://localhost:8086/actuator/health  # Analytics Service
# ... etc
```

---

## Service Port Reference

All services are accessible through the **API Gateway on port 8080** in production. Local development exposes each service on its configured port:

| Service | Local Port | Via Gateway |
|---------|------------|------------|
| User Service | 8084 | ✓ (8080) |
| Task Service | 8085 | ✓ (8080) |
| Analytics Service | 8086 | ✓ (8080) |
| Payment Service | 8087 | ✓ (8080) |
| Gamification Service | 8088 | ✓ (8080) |
| Practice Service | 8089 | ✓ (8080) |
| Feedback Service | 8090 | Event-driven only |
| Notification Service | 8091 | Event-driven only |
| BFF Service | 8083 | ✓ (8080) |

**Note:** Feedback and Notification Services are event-driven consumers (RabbitMQ) without REST endpoints.

---

## Advanced Commands

### Docker Management

```bash
# Build and start everything with full rebuild
make rebuild-all

# Clean up Docker resources
make dkr-clean

# Stop all running containers
make stop-all
```

### Database & Migrations

```bash
# Run migrations for all services
make migrate

# Run migrations for a specific service
make migrate-service SERVICE=feedback
```

### Maintenance

```bash
# Full project clean
make clean

# Package all modules (skip tests)
make package
```

---

## Architecture Decisions

### Why Spring Cloud Config?
- Externalized configuration management
- Easy environment-specific overrides
- Git-backed versioning and audit trail

### Why Event-Driven for Feedback?
- Decouples task and feedback services
- Enables asynchronous processing for code execution
- Handles high-volume submissions gracefully

### Why Multiple Databases?
- **PostgreSQL**: Transactional consistency for core domains
- **MongoDB**: Flexible schema for audit logs and notifications

### Why BFF Service?
- Optimized API surface for frontend clients
- Aggregates data from multiple services
- Reduces client complexity and network calls

---

## Configuration Management

### Externalized Configuration (Config Server Pattern)

This backend uses **Spring Cloud Config Server** for centralized configuration management. Service properties are **not** stored in this repository; they are fetched at runtime from a separate config repository.

**Config Repository:**
```
https://github.com/thenoblet/skilltracker-config
```

**How it works:**
1. Each service connects to the Config Server on startup
2. Config Server retrieves properties from the external Git repository
3. Service-specific properties are loaded dynamically (e.g., `task-service.properties`, `user-service.properties`)
4. Environment variables in `.env` override Git-backed properties

**Local Bootstrap Configuration (This Repository):**
Each service has minimal `bootstrap.properties` defining:
- Application name
- Server port
- Eureka discovery server location

These bootstrap properties enable initial startup and service discovery before retrieving full configuration from Config Server.

**Configuration Hierarchy (Highest to Lowest Priority):**
1. Environment variables (`.env`)
2. Git-backed properties (`skilltracker-config` repo)
3. Local `bootstrap.properties` (fallback/bootstrap only)

---

## Security

- **Authentication**: OAuth2 + JWT via User Service
- **API Gateway**: Validates JWT tokens; adds `X-User-Id` and `X-User-Roles` headers
- **Microservices**: Trust headers from gateway; use `@PreAuthorize` for authorization
- **Message Broker**: RabbitMQ credentials from `.env`
- **Database**: Separate credentials per service (task-service has its own DB user)

### Available Roles
- `USER` - Regular authenticated users
- `ADMIN` - Administrative access

---

## Troubleshooting

### Services fail to start
1. Check `.env` is properly configured
2. Verify database credentials
3. Ensure Config Server is running and accessible
4. Check logs: `make logs SERVICE=<name>` or `make dkr-logs`

### RabbitMQ connection errors
1. Verify RabbitMQ credentials in `.env`
2. Check RabbitMQ health: `docker-compose ps` or management UI (port 15672)
3. Ensure STOMP plugin is enabled (enabled in docker-compose.yaml)

### Database migration failures
1. Verify `POSTGRES_*` environment variables
2. Ensure PostgreSQL is healthy: `docker-compose ps`
3. Check migration files in `src/main/resources/db/migration/`

### Port conflicts
- Change port in service's `bootstrap.properties` or `application.properties`
- Docker Compose port mappings can be modified in service-specific `docker-compose.yml`

---

## Development Tools & Tips

### Rebuild Everything (Fresh Start)
```bash
make rebuild-all
```

### Monitor All Logs
```bash
make dkr-logs
```

### Stop Everything Without Cleanup
```bash
make stop-all
```

### Package Without Tests
```bash
make package
```

---

## Deployment

For production deployment:
1. Use `make build-all` to generate Docker images
2. Push images to container registry
3. Configure environment variables for production
4. Use `docker-compose` or Kubernetes manifests to orchestrate
5. Run Flyway migrations post-deployment

---

## Further Documentation

For detailed documentation on specific services, security architecture, and API specifications, refer to the `/docs` directory:

- `docs/security-architecture.md` - Detailed security design
- `docs/authorization-enforcement.md` - Authorization patterns
- `docs/task-service-*.md` - Task Service specifics
- `docs/feedback-service-*.md` - Feedback Service specifics
- `docs/git-commit-instructions.md` - Git workflow guide

---

## Support & Contribution

For issues, questions, or contributions, refer to the project's GitHub repository and issue tracker.

---

**Last Updated:** November 2025  
**Skill Tracker Backend v0.0.1-SNAPSHOT**
