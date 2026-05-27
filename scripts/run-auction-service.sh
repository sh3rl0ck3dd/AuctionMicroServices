#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../auction-service"

if [[ -x "./mvnw" ]]; then
  ./mvnw spring-boot:run
else
  ./gradlew bootRun
fi
