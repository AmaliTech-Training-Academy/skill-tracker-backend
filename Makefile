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
	@echo "📦  make package                    - Package all modules (skip tests)"
	@echo ""
	@echo "🚀  make run SERVICE=user           - Run a specific service (e.g. user)"
	@echo "🔁  make rebuild SERVICE=task       - Clean, build, and run a service"
	@echo ""
	@echo "🌍  make run-infra                  - Run infrastructure (Config, Discovery, Gateway)"
	@echo "🧩  make run-services               - Run all microservices"
	@echo "⚙️   make run-all                    - Run infra + microservices"
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
	@echo "📦  make build-discovery            - Build Discovery Server image"
	@echo "📦  make build-config               - Build Config Server image"
	@echo "📦  make build-gateway              - Build API Gateway image"
	@echo "📦  make build-shared               - Build Shared Services images"
	@echo ""
	@echo "🐳  make dkr-build SERVICE=<svc>   - Build Docker image for a specific service"
	@echo ""
	@echo ""
	@echo "▶️ DOCKER START COMMANDS"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "▶️   make start-infra               - Start all infrastructure containers"
	@echo "▶️   make start-services            - Start all microservice containers"
	@echo "▶️   make start-all                 - Start infra + all microservice containers"
	@echo ""
	@echo "🔍  make start-discovery            - Start Discovery Server"
	@echo "⚙️   make start-config              - Start Config Server"
	@echo "🚪  make start-gateway              - Start API Gateway"
	@echo "🗄️   make start-shared              - Start Shared Services (MongoDB, Redis, RabbitMQ)"
	@echo ""
	@echo "🐳  make dkr-run SERVICE=<svc>     - Run Docker container for a specific service"
	@echo ""
	@echo ""
	@echo "🛑 DOCKER STOP COMMANDS"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "🛑  make stop-all                   - Stop all running containers (no removal)"
	@echo "🧹  make down-all                   - Stop and remove all containers"
	@echo "🔥  make reset-all                  - Stop, remove containers + volumes (hard reset)"
	@echo ""
	@echo "🛑  make stop-discovery             - Stop Discovery Server"
	@echo "🛑  make stop-config                - Stop Config Server"
	@echo "🛑  make stop-gateway               - Stop API Gateway"
	@echo "🛑  make stop-shared                - Stop Shared Services"
	@echo ""
	@echo "🐳  make dkr-stop SERVICE=<svc>    - Stop specific service container"
	@echo ""
	@echo ""
	@echo "🔍 STATUS & MONITORING"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "📊  make status                     - Show all running containers"
	@echo "🔍  make check-infra                - Show infrastructure container status"
	@echo "🔍  make check-services             - Show microservices container status"
	@echo ""
	@echo ""
	@echo "🧽 MAINTENANCE & UTILITIES"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "🌐  make create-network             - Create skilltracker-network"
	@echo "🧽  make dkr-clean                  - Remove unused containers, images & volumes"
	@echo "📜  make dkr-logs                   - Tail logs from all running containers"
	@echo "♻️   make rebuild-all               - Clean, rebuild & start everything (Docker)"
	@echo ""
	@echo ""
	@echo "💡 EXAMPLES"
	@echo "-------------------------------------------------------------------------------------------"
	@echo "  # Maven workflows"
	@echo "  make run SERVICE=user"
	@echo "  make run-all"
	@echo "  make stop"
	@echo ""
	@echo "  # Docker workflows"
	@echo "  make build-infra"
	@echo "  make start-infra"
	@echo "  make start-service SERVICE=user"
	@echo "  make status"
	@echo "  make stop-all"
	@echo "  make down-all"
	@echo ""
	@echo "  # Individual components"
	@echo "  make build-discovery"
	@echo "  make start-discovery"
	@echo "  make stop-gateway"
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
# STOP MAVEN SERVICES
# -------------------------------------------------------
stop:
	@echo "🛑 Stopping all Spring Boot services..."
	@ps aux | grep "[s]pring-boot:run" | awk '{print $$2}' | xargs -r kill || true
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
# CREATE DOCKER NETWORK
# -------------------------------------------------------
create-network:
	@echo "🌐 Creating skilltracker-network..."
	@docker network create skilltracker-network 2>/dev/null || echo "✅ Network already exists"

# -------------------------------------------------------
# BUILD INDIVIDUAL INFRASTRUCTURE COMPONENTS
# -------------------------------------------------------
build-discovery:
	@echo "📦 Building Discovery Server..."
	@cd $(INFRA_DIR)/discovery-server && COMPOSE_BAKE=true docker-compose build
	@echo "✅ Discovery Server image built!"

build-config:
	@echo "📦 Building Config Server..."
	@cd $(INFRA_DIR)/config-server && COMPOSE_BAKE=true docker-compose build
	@echo "✅ Config Server image built!"

build-gateway:
	@echo "📦 Building API Gateway..."
	@cd $(INFRA_DIR)/api-gateway && COMPOSE_BAKE=true docker-compose build
	@echo "✅ API Gateway image built!"

build-shared:
	@echo "📦 Building Shared Services..."
	@if [ -d "$(INFRA_DIR)/shared-services" ]; then \
		cd $(INFRA_DIR)/shared-services && COMPOSE_BAKE=true docker-compose build; \
		echo "✅ Shared Services images built!"; \
	else \
		echo "⚠️  Shared services directory not found"; \
	fi

# -------------------------------------------------------
# Build Infrastructure Images
# -------------------------------------------------------
build-infra:
	@echo "🏗️  Building infrastructure Docker images..."
	@echo "📦 Building Discovery Server..."
	@cd $(INFRA_DIR)/discovery-server && COMPOSE_BAKE=true docker-compose build
	@echo "📦 Building Config Server..."
	@cd $(INFRA_DIR)/config-server && COMPOSE_BAKE=true docker-compose build
	@echo "📦 Building API Gateway..."
	@cd $(INFRA_DIR)/api-gateway && COMPOSE_BAKE=true docker-compose build
	@if [ -d "$(INFRA_DIR)/shared-services" ]; then \
		echo "📦 Building Shared Services..."; \
		cd $(INFRA_DIR)/shared-services && COMPOSE_BAKE=true docker-compose build; \
	fi
	@echo "✅ All infrastructure Docker images built successfully!"
	@echo ""


# -------------------------------------------------------
# Build Microservice Images
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
build-all: build-infra build-services
	@echo "✅ All infrastructure and microservice Docker images built successfully!"


# -------------------------------------------------------
# START INDIVIDUAL INFRASTRUCTURE COMPONENTS
# -------------------------------------------------------
start-discovery:
	@echo "🔍 Starting Discovery Server..."
	$(MAKE) create-network
	@cd $(INFRA_DIR)/discovery-server && docker-compose up -d
	@echo "✅ Discovery Server started!"

start-config:
	@echo "⚙️  Starting Config Server..."
	$(MAKE) create-network
	@cd $(INFRA_DIR)/config-server && docker-compose up -d
	@echo "✅ Config Server started!"

start-gateway:
	@echo "🚪 Starting API Gateway..."
	$(MAKE) create-network
	@cd $(INFRA_DIR)/api-gateway && docker-compose up -d
	@echo "✅ API Gateway started!"

start-shared:
	@echo "🗄️  Starting Shared Services..."
	$(MAKE) create-network
	@if [ -d "$(INFRA_DIR)/shared-services" ]; then \
		cd $(INFRA_DIR)/shared-services && docker-compose up -d; \
		echo "✅ Shared Services started!"; \
	else \
		echo "⚠️  Shared services directory not found"; \
	fi

# -------------------------------------------------------
# Start Infrastructure Containers
# -------------------------------------------------------
start-infra:
	@echo "▶️  Starting infrastructure containers..."
	@docker network create skilltracker-network 2>/dev/null || true

	@echo "🗄️  Starting Shared Services (MongoDB, Redis, RabbitMQ)..."
	@if [ -d "$(INFRA_DIR)/shared-services" ]; then \
		cd $(INFRA_DIR)/shared-services && docker-compose up -d; \
	fi
	@sleep 5

	@echo "🔍 Starting Discovery Server..."
	@cd $(INFRA_DIR)/discovery-server && docker-compose up -d
	@sleep 10

	@echo "⚙️  Starting Config Server..."
	@cd $(INFRA_DIR)/config-server && docker-compose up -d
	@sleep 10

	@echo "🚪 Starting API Gateway..."
	@cd $(INFRA_DIR)/api-gateway && docker-compose up -d

	@echo "⏳ Waiting for infrastructure to become healthy..."
	@sleep 10
	@echo "✅ All infrastructure containers started!"


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
start-all: start-infra start-services
	@echo "✅ All containers started successfully!"


# -------------------------------------------------------
# Build Specific Microservice Image
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
# STOP INDIVIDUAL INFRASTRUCTURE COMPONENTS
# -------------------------------------------------------
stop-discovery:
	@echo "🛑 Stopping Discovery Server..."
	@cd $(INFRA_DIR)/discovery-server && docker-compose down
	@echo "✅ Discovery Server stopped!"

stop-config:
	@echo "🛑 Stopping Config Server..."
	@cd $(INFRA_DIR)/config-server && docker-compose down
	@echo "✅ Config Server stopped!"

stop-gateway:
	@echo "🛑 Stopping API Gateway..."
	@cd $(INFRA_DIR)/api-gateway && docker-compose down
	@echo "✅ API Gateway stopped!"

stop-shared:
	@echo "🛑 Stopping Shared Services..."
	@if [ -d "$(INFRA_DIR)/shared-services" ]; then \
		cd $(INFRA_DIR)/shared-services && docker-compose down; \
		echo "✅ Shared Services stopped!"; \
	fi

# -------------------------------------------------------
# Stop All Containers
# -------------------------------------------------------
stop-all:
	@echo "🛑 Stopping all containers (without removing them)..."
	@echo "🛑 Stopping microservices..."
	@cd skilltracker-services/bff-service && docker-compose stop 2>/dev/null || true
	@cd skilltracker-services/practice-service && docker-compose stop 2>/dev/null || true
	@cd skilltracker-services/payment-service && docker-compose stop 2>/dev/null || true
	@cd skilltracker-services/notification-service && docker-compose stop 2>/dev/null || true
	@cd skilltracker-services/gamification-service && docker-compose stop 2>/dev/null || true
	@cd skilltracker-services/feedback-service && docker-compose stop 2>/dev/null || true
	@cd skilltracker-services/analytics-service && docker-compose stop 2>/dev/null || true
	@cd skilltracker-services/task-service && docker-compose stop 2>/dev/null || true
	@cd skilltracker-services/user-service && docker-compose stop 2>/dev/null || true
	@echo "🛑 Stopping infrastructure..."
	@cd $(INFRA_DIR)/api-gateway && docker-compose stop 2>/dev/null || true
	@cd $(INFRA_DIR)/config-server && docker-compose stop 2>/dev/null || true
	@cd $(INFRA_DIR)/discovery-server && docker-compose stop 2>/dev/null || true
	@if [ -d "$(INFRA_DIR)/shared-services" ]; then \
		cd $(INFRA_DIR)/shared-services && docker-compose stop 2>/dev/null || true; \
	fi
	@echo "✅ All containers stopped (but not removed)!"

# -------------------------------------------------------
# Stop and Remove Containers
# -------------------------------------------------------
down-all:
	@echo "🧹 Stopping and removing all containers..."
	@echo "🧹 Removing microservices..."
	@cd skilltracker-services/bff-service && docker-compose down 2>/dev/null || true
	@cd skilltracker-services/practice-service && docker-compose down 2>/dev/null || true
	@cd skilltracker-services/payment-service && docker-compose down 2>/dev/null || true
	@cd skilltracker-services/notification-service && docker-compose down 2>/dev/null || true
	@cd skilltracker-services/gamification-service && docker-compose down 2>/dev/null || true
	@cd skilltracker-services/feedback-service && docker-compose down 2>/dev/null || true
	@cd skilltracker-services/analytics-service && docker-compose down 2>/dev/null || true
	@cd skilltracker-services/task-service && docker-compose down 2>/dev/null || true
	@cd skilltracker-services/user-service && docker-compose down 2>/dev/null || true
	@echo "🧹 Removing infrastructure..."
	@cd $(INFRA_DIR)/api-gateway && docker-compose down 2>/dev/null || true
	@cd $(INFRA_DIR)/config-server && docker-compose down 2>/dev/null || true
	@cd $(INFRA_DIR)/discovery-server && docker-compose down 2>/dev/null || true
	@if [ -d "$(INFRA_DIR)/shared-services" ]; then \
		cd $(INFRA_DIR)/shared-services && docker-compose down 2>/dev/null || true; \
	fi
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
dkr-clean: stop-all
	@echo "🧹 Cleaning up Docker resources..."
	docker system prune -f
	docker volume prune -f
	@echo "✅ Docker cleanup complete!"


# -------------------------------------------------------
# Reset All (with volumes)
# -------------------------------------------------------
reset-all:
	@echo "🔥 Removing containers, networks, and volumes..."
	@echo "🔥 Removing microservices..."
	@cd skilltracker-services/bff-service && docker-compose down -v 2>/dev/null || true
	@cd skilltracker-services/practice-service && docker-compose down -v 2>/dev/null || true
	@cd skilltracker-services/payment-service && docker-compose down -v 2>/dev/null || true
	@cd skilltracker-services/notification-service && docker-compose down -v 2>/dev/null || true
	@cd skilltracker-services/gamification-service && docker-compose down -v 2>/dev/null || true
	@cd skilltracker-services/feedback-service && docker-compose down -v 2>/dev/null || true
	@cd skilltracker-services/analytics-service && docker-compose down -v 2>/dev/null || true
	@cd skilltracker-services/task-service && docker-compose down -v 2>/dev/null || true
	@cd skilltracker-services/user-service && docker-compose down -v 2>/dev/null || true
	@echo "🔥 Removing infrastructure..."
	@cd $(INFRA_DIR)/api-gateway && docker-compose down -v 2>/dev/null || true
	@cd $(INFRA_DIR)/config-server && docker-compose down -v 2>/dev/null || true
	@cd $(INFRA_DIR)/discovery-server && docker-compose down -v 2>/dev/null || true
	@if [ -d "$(INFRA_DIR)/shared-services" ]; then \
		cd $(INFRA_DIR)/shared-services && docker-compose down -v 2>/dev/null || true; \
	fi
	@echo "✅ Environment fully reset!"


# -------------------------------------------------------
# View Docker Logs
# -------------------------------------------------------
dkr-logs:
	@echo "📜 Showing logs from all running containers..."
	@docker ps --format "{{.Names}}" | xargs -I {} docker logs -f {} --tail=50 2>/dev/null || true


# -------------------------------------------------------
# STATUS & MONITORING
# -------------------------------------------------------
status:
	@echo "📊 Container Status:"
	@echo ""
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || echo "No containers running"

check-infra:
	@echo "🔍 Infrastructure Status:"
	@docker ps --filter "name=discovery" --filter "name=config" --filter "name=gateway" --filter "name=mongo" --filter "name=redis" --filter "name=rabbitmq" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || echo "No infrastructure running"

check-services:
	@echo "🔍 Microservices Status:"
	@docker ps --filter "name=user-service" --filter "name=task-service" --filter "name=analytics" --filter "name=feedback" --filter "name=gamification" --filter "name=notification" --filter "name=payment" --filter "name=practice" --filter "name=bff" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || echo "No microservices running"


# -------------------------------------------------------
# Rebuild and Restart Everything
# -------------------------------------------------------
rebuild-all: dkr-clean build-all start-all
	@echo "♻️  Complete Docker rebuild and startup finished successfully!"