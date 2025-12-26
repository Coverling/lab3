.PHONY: help build clean run docker-up docker-down docker-logs test

help:
	@echo "Available commands:"
	@echo "  make build        - Build the Maven project"
	@echo "  make clean        - Clean build artifacts and caches"
	@echo "  make run          - Run application locally (requires PostgreSQL running)"
	@echo "  make docker-up    - Start Docker containers (PostgreSQL + App)"
	@echo "  make docker-down  - Stop Docker containers"
	@echo "  make docker-logs  - Show Docker logs"
	@echo "  make test         - Run tests"
	@echo "  make install-deps - Install Maven dependencies"

build:
	@echo "Building Maven project..."
	mvn clean package

clean:
	@echo "Cleaning build artifacts..."
	mvn clean

run: build
	@echo "Running application..."
	java -jar target/notification-service-1.0.0.jar

docker-up:
	@echo "Starting Docker containers..."
	docker-compose up -d
	@echo "Waiting for services to be ready..."
	@sleep 10
	@echo "Services started. Access at http://localhost:8080"

docker-down:
	@echo "Stopping Docker containers..."
	docker-compose down

docker-logs:
	@echo "Showing Docker logs..."
	docker-compose logs -f app

docker-logs-postgres:
	@echo "Showing PostgreSQL logs..."
	docker-compose logs -f postgres

test:
	@echo "Running tests..."
	mvn test

install-deps:
	@echo "Installing dependencies..."
	mvn dependency:resolve

rebuild-docker:
	@echo "Rebuilding Docker image..."
	docker-compose build --no-cache

ps:
	@echo "Showing running containers..."
	docker-compose ps

shell-postgres:
	@echo "Connecting to PostgreSQL..."
	docker-compose exec postgres psql -U notification_user -d notification_db

health-check:
	@echo "Checking app health..."
	curl -v http://localhost:8080/api/notifications/health
	@echo "\nChecking Service B..."
	curl -v http://localhost:8080/api/client/health
