# Skill Tracker Backend - Microservices Architecture

A comprehensive Spring Boot microservices application for skill tracking and management.

## 🔐 Security Setup (IMPORTANT - First Time Setup)

Before running the application, you must create the database initialization file:

```bash
# Copy the template file
cp init-postgres.sql.example init-postgres.sql

# Edit init-postgres.sql and replace placeholder passwords with actual values from your .env file
```

⚠️ **NEVER commit `init-postgres.sql` to version control** - it's already in `.gitignore`

## 🚀 Quick Start

### Prerequisites
- Docker Desktop installed and running
- At least 8GB RAM allocated to Docker

### Setup
```bash
# 1. Copy environment template
cp .env.example .env

# 2. Setup database initialization
cp init-postgres.sql.example init-postgres.sql
# Edit init-postgres.sql with actual passwords from .env

# 3. Start all services
docker-compose up -d
```

## 🔐 Security Best Practices

- ✅ **Never commit** `.env` or `init-postgres.sql` to version control
- ✅ **Use strong passwords** in production
- ✅ **Use secrets management** for production deployments
