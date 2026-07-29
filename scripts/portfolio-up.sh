#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PORT="${PORT:-8080}"
IMAGE_NAME="${IMAGE_NAME:-vuemind-api}"

if [[ "${USE_DOCKER:-0}" == "1" ]]; then
  docker build -t "$IMAGE_NAME" .
  exec docker run --rm -p "${PORT}:8080" \
    -e SERVER_PORT=8080 \
    "$IMAGE_NAME"
fi

./mvnw -q -DskipTests package
JAR="$(ls -1 target/vuemind-api-*.jar | grep -v '\.original$' | head -n1)"
exec java ${JAVA_OPTS:-} -jar "$JAR" --server.port="$PORT"
