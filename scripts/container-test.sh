#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT_DIR"

MODE=${1:-all}
COMPOSE="docker compose -f docker-compose.yml -f docker-compose.test.yml"

run_unit_tests() {
  echo "=== Maven tests inside container ==="
  $COMPOSE build unit-tests
  $COMPOSE run --rm --no-deps unit-tests test
}

run_smoke_tests() {
  echo "=== Starting application stack ==="
  $COMPOSE up -d --build api-gateway
  echo "=== API smoke tests inside container ==="
  $COMPOSE run --rm smoke-tests
  $COMPOSE ps
}

case "$MODE" in
  unit)
    run_unit_tests
    ;;
  smoke)
    run_smoke_tests
    ;;
  all)
    run_unit_tests
    run_smoke_tests
    ;;
  *)
    echo "Usage: $0 [unit|smoke|all]"
    exit 2
    ;;
esac
