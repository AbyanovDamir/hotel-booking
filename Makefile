COMPOSE_DEV = docker compose -f docker-compose.yml -f docker-compose.dev.yml

.PHONY: help test test-container test-e2e build up down restart logs status smoke clean

help:
	@echo "test     - run all Maven tests"
	@echo "test-container - run Maven tests in Docker"
	@echo "test-e2e - start the stack and run all tests in Docker"
	@echo "build    - build executable JAR files"
	@echo "up       - build and start the development stack"
	@echo "down     - stop containers without deleting database volumes"
	@echo "restart  - restart the development stack"
	@echo "logs     - follow service logs"
	@echo "status   - show container and health status"
	@echo "smoke    - run end-to-end API checks"
	@echo "clean    - remove Maven build output"

test:
	mvn test

test-container:
	./scripts/container-test.sh unit

test-e2e:
	./scripts/container-test.sh all

build:
	mvn package

up:
	./scripts/docker-start.sh

down:
	./scripts/docker-stop.sh

restart:
	./scripts/docker-restart.sh

logs:
	./scripts/docker-logs.sh

status:
	$(COMPOSE_DEV) ps

smoke:
	./scripts/smoke-test.sh

clean:
	mvn clean
