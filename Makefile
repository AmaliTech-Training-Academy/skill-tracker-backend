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

# -------------------------------------------------------
# HELP
# -------------------------------------------------------

help:
	@echo ""
	@echo "📘 Skill Tracker Backend Commands"
	@echo "-------------------------------------------------------"
	@echo "🛠️   make build                   - Build all modules"
	@echo "🧹  make clean                   - Clean all targets"
	@echo "🧪  make test                    - Run all tests"
	@echo "📦  make package                 - Package all modules (skip tests)"
	@echo ""
	@echo "🚀  make run SERVICE=user        - Run a specific service"
	@echo "🔁  make rebuild SERVICE=user    - Clean, build, and run a service"
	@echo ""
	@echo "🌍  make run-infra               - Run only infrastructure services"
	@echo "🧩  make run-services            - Run only microservices"
	@echo "⚙️   make run-all                 - Run all infra and microservices concurrently"
	@echo ""
	@echo "🛑  make stop                    - Stop all running Spring Boot services"
	@echo "📜  make logs SERVICE=user       - Tail logs for a specific service"
	@echo ""
	@echo "💡 Examples:"
	@echo "  make run SERVICE=user"
	@echo "  make rebuild SERVICE=task"
	@echo "  make run-infra"
	@echo "  make run-services"
	@echo "  make run-all"
	@echo ""

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
# Run All Services (Infra + Microservices)
# -----------------------------------------------

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
