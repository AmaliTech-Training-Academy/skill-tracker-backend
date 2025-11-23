# -------------------------------------------------------
# Skill Tracker Backend - Makefile
# -------------------------------------------------------

# Root variables
ROOT_DIR := $(shell pwd)
MVN := ./mvnw
SERVICES_DIR := skilltracker-services
COMMON_DIR := skilltracker-common
INFRA_DIR := skilltracker-infra

# Default target
.DEFAULT_GOAL := help

export COMPOSE_BAKE=true

.PHONY: h
h: help

# -------------------------------------------------------
# HELP
# -------------------------------------------------------
help:
	@echo ""
	@echo "📘 Skill Tracker Backend - Command Reference"
	@echo "==========================================================================================="
	@echo ""
	@echo "🔧 MAVEN COMMANDS"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "🛠️   make build                      - Build all Maven modules"
	@echo "🧹  make clean                      - Clean Maven build targets"
	@echo "🧪  make test                       - Run all Maven tests"
	@echo "🧪  make test-service SERVICE=user  - Run tests for a specific service"
	@echo "🧪  make test-infra COMPONENT=...   - Run tests for an infrastructure component"
	@echo "🧪  make test-common                - Run tests for common module"
	@echo "📦  make package                    - Package all modules (skip tests)"
	@echo ""
	@echo "🗄️   FLYWAY DATABASE MIGRATIONS"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "🗄️   make migrate                   - Run Flyway migrations for all services"
	@echo "🗄️   make migrate-service SERVICE=task - Run Flyway migrations for a specific service"
	@echo ""
	@echo "🚀  make run SERVICE=user           - Run a specific service (e.g. user)"
	@echo "🔁  make rebuild SERVICE=task       - Clean, build, and run a service"
	@echo ""
	@echo "🌍  make run-infra                  - Run infrastructure (Config, Discovery, Gateway)"
	@echo "🧩  make run-services               - Run all microservices"
	@echo "⚙️   make run-all                    - Run infra + microservices"
	@echo "🪄  make run-all-alias              - Alias for 'make run-all'"
	@echo ""
	@echo "🛑  make stop                       - Stop all running Spring Boot processes"
	@echo "📜  make logs SERVICE=user          - Tail logs for a specific service"
	@echo ""
	@echo ""
	@echo "🐳 DOCKER BUILD COMMANDS"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "🏗️   make build-infra               - Build all infrastructure Docker images"
	@echo "🏗️   make build-services            - Build all microservice Docker images"
	@echo "🏗️   make build-all                 - Build both infra + microservice images"
	@echo ""
	@echo "🐳  make dkr-build SERVICE=<svc>   - Build Docker image for a specific service"
	@echo ""
	@echo ""
	@echo "▶️ DOCKER RUN COMMANDS"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "▶️   make start-infra               - Start infrastructure containers"
	@echo "▶️   make start-services            - Start all microservice containers"
	@echo "▶️   make start-all                 - Start infra + all microservice containers"
	@echo ""
	@echo "🐳  make dkr-run SERVICE=<svc>     - Run Docker container for a specific service"
	@echo ""
	@echo ""
	@echo "🧭 CONTAINER LIFECYCLE MANAGEMENT"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "🛑  make stop-all                   - Stop all running containers (no removal)"
	@echo "🧹  make down-all                   - Stop and remove all containers"
	@echo "🔥  make reset-all                  - Stop, remove containers + volumes (hard reset)"
	@echo ""
	@echo "♻️   make rebuild-all               - Clean, rebuild & start everything (Docker)"
	@echo ""
	@echo ""
	@echo "🧽 MAINTENANCE & UTILITIES"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "🧽  make dkr-clean                  - Remove unused containers, images & volumes"
	@echo "📜  make dkr-logs                   - Tail logs from all running containers"
	@echo ""
	@echo ""
	@echo "💡 EXAMPLES"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "  make run SERVICE=user"
	@echo "  make rebuild SERVICE=task"
	@echo "  make test-service SERVICE=analytics"
	@echo "  make test-infra COMPONENT=config-server"
	@echo "  make migrate"
	@echo "  make migrate-service SERVICE=task"
	@echo "  make run-infra"
	@echo "  make run-services"
	@echo "  make run-all"
	@echo "  make logs SERVICE=user"
	@echo ""
	@echo "  make build-infra"
	@echo "  make build-services"
	@echo "  make dkr-build SERVICE=user"
	@echo "  make dkr-run SERVICE=user"
	@echo "  make dkr-stop SERVICE=user"
	@echo ""
	@echo "  make start-all"
	@echo "  make stop-all"
	@echo "  make down-all"
	@echo "  make reset-all"
	@echo "  make dkr-clean"
	@echo "  make dkr-logs"
	@echo "  make rebuild-all"
	@echo ""
	@echo "==========================================================================================="


# -------------------------------------------------------
# BUILD COMMANDS
# -------------------------------------------------------
build:
	@echo "🏗️  Building all modules..."
	$(MVN) clean install -DskipTests

clean:
	@echo "🧹 Cleaning project..."
	$(MVN) clean

test:
	@echo "🧪 Running all tests..."
	$(MVN) test

package:
	@echo "📦 Packaging all modules..."
	$(MVN) clean package -DskipTests

# -------------------------------------------------------
# RUN SINGLE SERVICE
# -------------------------------------------------------
run:
	@if [ -z "$(SERVICE)" ]; then \
		echo "❌ Please provide a SERVICE variable, e.g. make run SERVICE=user"; \
		exit 1; \
	fi
	@echo "🚀 Starting $(SERVICE)-service..."
	$(MVN) -f $(SERVICES_DIR)/$(SERVICE)-service spring-boot:run

# -------------------------------------------------------
# REBUILD SINGLE SERVICE
# -------------------------------------------------------
rebuild:
	@if [ -z "$(SERVICE)" ]; then \
		echo "❌ Please specify a service. Example: make rebuild SERVICE=user"; \
		exit 1; \
	fi
	@echo "🧹 Cleaning and rebuilding $(SERVICE)-service..."
	$(MVN) -f $(SERVICES_DIR)/$(SERVICE)-service clean package -DskipTests
	@echo "🚀 Starting $(SERVICE)-service..."
	$(MVN) -f $(SERVICES_DIR)/$(SERVICE)-service spring-boot:run

# -----------------------------------------------
# Run Infrastructure Only
# -----------------------------------------------
run-infra:
	@echo "🏗️  Starting Infrastructure Services..."
	@mkdir -p logs
	@echo "🧩 Starting Config Server..."
	@$(MVN) -f $(INFRA_DIR)/config-server spring-boot:run > logs/config-server.log 2>&1 &
	sleep 8
	@echo "✅ Config Server started."

	@echo "🧩 Starting Discovery Server..."
	@$(MVN) -f $(INFRA_DIR)/discovery-server spring-boot:run > logs/discovery-server.log 2>&1 &
	sleep 8
	@echo "✅ Discovery Server started."

	@echo "🧩 Starting API Gateway..."
	@$(MVN) -f $(INFRA_DIR)/api-gateway spring-boot:run > logs/api-gateway.log 2>&1 &
	sleep 8
	@echo "✅ API Gateway started."

	@echo ""
	@echo "🏁 All infrastructure services are running in the background (logs in ./logs)"

# -----------------------------------------------
# Run Microservices Only
# -----------------------------------------------
run-services:
	@echo "🧠 Starting Microservices..."
	@mkdir -p logs
	@for service in $(SERVICES_DIR)/*-service; do \
		service_name=$$(basename $$service); \
		echo "🚀 Starting $$service_name..."; \
		$(MVN) -f $$service spring-boot:run > logs/$$service_name.log 2>&1 & \
		sleep 5; \
	done
	@echo ""
	@echo "🏁 All microservices are running in the background (logs in ./logs)"

# -----------------------------------------------
# Run Everything (Infra + Services)
# -----------------------------------------------
run-all:
	@echo "🚀 Starting Infrastructure + Microservices..."
	$(MAKE) run-infra
	sleep 10
	$(MAKE) run-services



# -------------------------------------------------------
# STOP SERVICES
# -------------------------------------------------------
stop:
	@echo "🛑 Stopping all Spring Boot services..."
	@pkill -f "spring-boot:run" || true
	@echo "✅ All Spring Boot processes stopped."

# -------------------------------------------------------
# LOGS
# -------------------------------------------------------
logs:
	@if [ -z "$(SERVICE)" ]; then \
		echo "❌ Please specify a service. Example: make logs SERVICE=user"; \
		exit 1; \
	fi
	@if [ -f logs/$(SERVICE)-service.log ]; then \
		echo "📜 Tailing logs for $(SERVICE)-service..."; \
		tail -f logs/$(SERVICE)-service.log; \
	else \
		echo "⚠️  Log file not found for $(SERVICE)-service. Maybe it hasn't been started yet."; \
	fi

# Allow `make run all` as shorthand for `make run-all`
run-all-alias: run-all


# -------------------------------------------------------
# 🐳 DOCKER COMMANDS
# -------------------------------------------------------
# -------------------------------------------------------
# Build Infrastructure Images
# -------------------------------------------------------
# Builds Docker images for all infrastructure components
# such as Config Server, Discovery Server, and API Gateway.
# These services provide the foundational network and configuration
# layer for the Skill Tracker microservices ecosystem.
# Example: make build-infra
# -------------------------------------------------------
build-infra:
	@echo "🏗️  Building infrastructure Docker images..."
	@COMPOSE_BAKE=true docker-compose build


# -------------------------------------------------------
# Build Microservice Images
# -------------------------------------------------------
# Builds Docker images for all backend microservices such as
# user-service, task-service, analytics-service, etc.
# Each service has its own docker-compose.yml for independent builds.
# Example: make build-services
# -------------------------------------------------------
build-services:
	@echo "🏗️  Building all microservice Docker images with Buildx Bake..."
	@docker buildx bake user-service task-service analytics-service feedback-service \
	gamification-service notification-service payment-service practice-service bff-service \
	--set *.network=host
	@echo "✅ All microservice Docker images built successfully!"
	@echo ""


# -------------------------------------------------------
# Build All Images
# -------------------------------------------------------
# Combines both infrastructure and microservice builds.
# Runs make build-infra followed by make build-services.
# Example: make build-all
# -------------------------------------------------------
build-all: build-infra build-services
	@echo "✅ All infrastructure and microservice Docker images built successfully!"


# -------------------------------------------------------
# Start Infrastructure Containers
# -------------------------------------------------------
# Spins up all infrastructure containers defined in docker-compose.yml
# (Config Server, Discovery Server, API Gateway, databases, etc.)
# Creates the 'skilltracker-network' if it does not exist.
# Example: make start-infra
# -------------------------------------------------------
start-infra:
	@echo "▶️  Starting infrastructure containers..."
	docker network create skilltracker-network 2>/dev/null || true
	docker-compose up -d
	@echo "⏳ Waiting for infrastructure to become healthy..."
	@sleep 10


# -------------------------------------------------------
# Start Microservices Containers
# -------------------------------------------------------
start-services:
	@echo "▶️  Starting all microservice containers..."
	@cd skilltracker-services/user-service && docker-compose up -d
	@cd skilltracker-services/task-service && docker-compose up -d
	@cd skilltracker-services/analytics-service && docker-compose up -d
	@cd skilltracker-services/feedback-service && docker-compose up -d
	@cd skilltracker-services/gamification-service && docker-compose up -d
	@cd skilltracker-services/notification-service && docker-compose up -d
	@cd skilltracker-services/payment-service && docker-compose up -d
	@cd skilltracker-services/practice-service && docker-compose up -d
	@cd skilltracker-services/bff-service && docker-compose up -d

# -------------------------------------------------------
# Start All Containers (Infra + Microservices)
# -------------------------------------------------------
# Starts the infrastructure and then all microservice containers.
# Useful for bootstrapping the full Skill Tracker system locally.
# Example: make start-all
# -------------------------------------------------------
start-all: start-infra start-services
	@echo "✅ All containers started successfully!"


# -------------------------------------------------------
# Build Specific Microservice Image
# -------------------------------------------------------
# Builds a Docker image for a single microservice.
# Requires specifying the service name via SERVICE variable.
# Example: make dkr-build SERVICE=user
# -------------------------------------------------------
dkr-build:
	@if [ -z "$(SERVICE)" ]; then \
		echo "❌ Please specify a service to build. Example: make dkr-build SERVICE=user"; \
		exit 1; \
	fi
	@echo "🐳 Building Docker image for $(SERVICE)-service (with Bake)..."
	@if [ -d "$(SERVICES_DIR)/$(SERVICE)-service" ]; then \
		cd $(SERVICES_DIR)/$(SERVICE)-service && COMPOSE_BAKE=true docker compose build; \
		echo "✅ Successfully built Docker image for $(SERVICE)-service."; \
	else \
		echo "⚠️  Service directory not found: $(SERVICES_DIR)/$(SERVICE)-service"; \
		exit 1; \
	fi

# -------------------------------------------------------
# Run Specific Service
# -------------------------------------------------------
dkr-run:
	@if [ -z "$(SERVICE)" ]; then \
		echo "❌ Please specify a service to run. Example: make dkr-run SERVICE=user"; \
		exit 1; \
	fi
	@echo "🚀 Starting Docker container for $(SERVICE)-service..."
	@if [ -d "$(SERVICES_DIR)/$(SERVICE)-service" ]; then \
		cd $(SERVICES_DIR)/$(SERVICE)-service && docker-compose up -d; \
		echo "✅ $(SERVICE)-service is now running."; \
	else \
		echo "⚠️  Service directory not found: $(SERVICES_DIR)/$(SERVICE)-service"; \
		exit 1; \
	fi

# -------------------------------------------------------
# Stop All Containers
# -------------------------------------------------------
# Stops all running containers for both
# infrastructure and microservices.
# Example: make stop-all
# -------------------------------------------------------
stop-all:
	@echo "🛑 Stopping all containers (without removing them)..."
	@cd skilltracker-services/bff-service && docker-compose stop
	@cd skilltracker-services/practice-service && docker-compose stop
	@cd skilltracker-services/payment-service && docker-compose stop
	@cd skilltracker-services/notification-service && docker-compose stop
	@cd skilltracker-services/gamification-service && docker-compose stop
	@cd skilltracker-services/feedback-service && docker-compose stop
	@cd skilltracker-services/analytics-service && docker-compose stop
	@cd skilltracker-services/task-service && docker-compose stop
	@cd skilltracker-services/user-service && docker-compose stop
	docker-compose stop
	@echo "✅ All containers stopped (but not removed)!"

# -------------------------------------------------------
# Stop and Remove Containers
# -------------------------------------------------------

down-all:
	@echo "🧹 Stopping and removing all containers..."
	@cd skilltracker-services/bff-service && docker-compose down
	@cd skilltracker-services/practice-service && docker-compose down
	@cd skilltracker-services/payment-service && docker-compose down
	@cd skilltracker-services/notification-service && docker-compose down
	@cd skilltracker-services/gamification-service && docker-compose down
	@cd skilltracker-services/feedback-service && docker-compose down
	@cd skilltracker-services/analytics-service && docker-compose down
	@cd skilltracker-services/task-service && docker-compose down
	@cd skilltracker-services/user-service && docker-compose down
	docker-compose down
	@echo "✅ All containers stopped and removed!"

# -------------------------------------------------------
# Stop Specific Service
# -------------------------------------------------------
dkr-stop:
	@if [ -z "$(SERVICE)" ]; then \
		echo "❌ Please specify a service to stop. Example: make dkr-stop SERVICE=user"; \
		exit 1; \
	fi
	@echo "🛑 Stopping Docker container for $(SERVICE)-service..."
	@if [ -d "$(SERVICES_DIR)/$(SERVICE)-service" ]; then \
		cd $(SERVICES_DIR)/$(SERVICE)-service && docker-compose down; \
		echo "✅ $(SERVICE)-service has been stopped."; \
	else \
		echo "⚠️  Service directory not found: $(SERVICES_DIR)/$(SERVICE)-service"; \
		exit 1; \
	fi

# -------------------------------------------------------
# Docker Cleanup
# -------------------------------------------------------
# Cleans up unused Docker containers, images, and volumes.
# Runs after stopping all containers.
# Example: make dkr-clean
# -------------------------------------------------------
dkr-clean: stop-all
	@echo "🧹 Cleaning up Docker resources..."
	docker system prune -f
	docker volume prune -f
	@echo "✅ Docker cleanup complete!"


reset-all:
	@echo "🔥 Removing containers, networks, and volumes..."
	docker-compose down -v
	@echo "✅ Environment fully reset!"


# -------------------------------------------------------
# View Docker Logs
# -------------------------------------------------------
# Streams logs from all containers (infra + services).
# Useful for debugging and monitoring container output.
# Example: make dkr-logs
# -------------------------------------------------------
dkr-logs:
	@echo "📜 Showing logs from all running containers..."
	docker-compose logs -f


# -------------------------------------------------------
# Rebuild and Restart Everything
# -------------------------------------------------------
# Performs a full system rebuild:
# 1. Cleans Docker containers & volumes
# 2. Rebuilds all images
# 3. Starts the full stack
# Example: make rebuild-all
# -------------------------------------------------------
rebuild-all: dkr-clean build-all start-all
	@echo "♻️  Complete Docker rebuild and startup finished successfully!"


# -------------------------------------------------------
# TEST COMMANDS
# -------------------------------------------------------
test:
	@echo "🧪 Running all tests..."
	$(MVN) test

# Test a specific service
test-service:
	@if [ -z "$(SERVICE)" ]; then \
		echo "❌ Please provide a SERVICE variable, e.g. make test-service SERVICE=user"; \
		exit 1; \
	fi
	@echo "🧪 Running tests for $(SERVICE)-service..."
	$(MVN) -f $(SERVICES_DIR)/$(SERVICE)-service test

# Test infrastructure component
test-infra:
	@if [ -z "$(COMPONENT)" ]; then \
		echo "❌ Please provide a COMPONENT variable, e.g. make test-infra COMPONENT=config-server"; \
		exit 1; \
	fi
	@echo "🧪 Running tests for $(COMPONENT)..."
	$(MVN) -f $(INFRA_DIR)/$(COMPONENT) test

# Test common module
test-common:
	@echo "🧪 Running tests for common module..."
	$(MVN) -f $(COMMON_DIR) test

# -------------------------------------------------------
# DATABASE MIGRATION COMMANDS (FLYWAY)
# -------------------------------------------------------
# Migrate all services
# Example: make migrate
# -------------------------------------------------------
migrate:
	@echo "🗄️  Running Flyway migrations for all services..."
	$(MVN) flyway:migrate
	@echo "✅ All migrations completed!"

# Migrate a specific service
# Example: make migrate-service SERVICE=task
# -------------------------------------------------------
migrate-service:
	@if [ -z "$(SERVICE)" ]; then \
		echo "❌ Please provide a SERVICE variable, e.g. make migrate-service SERVICE=task"; \
		exit 1; \
	fi
	@echo "🗄️  Running Flyway migrations for $(SERVICE)-service..."
	$(MVN) flyway:migrate -pl $(SERVICES_DIR)/$(SERVICE)-service
	@echo "✅ Migration completed for $(SERVICE)-service!"