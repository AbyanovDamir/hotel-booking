#!/usr/bin/env bash
set -euo pipefail

mvn -q clean package

cleanup() {
  jobs -p | xargs -r kill
}
trap cleanup EXIT INT TERM

java -jar eureka-server/target/eureka-server-1.0.0-exec.jar &
sleep 8
java -jar hotel-service/target/hotel-service-1.0.0-exec.jar &
java -jar booking-service/target/booking-service-1.0.0-exec.jar &
sleep 8
java -jar api-gateway/target/api-gateway-1.0.0-exec.jar &

echo "Gateway: http://localhost:8080 | Eureka: http://localhost:8761"
wait
