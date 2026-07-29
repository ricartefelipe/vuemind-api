#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PORT="${PORT:-8080}"
HEALTH_URL="${HEALTH_URL:-http://localhost:${PORT}/actuator/health}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-90}"
IMAGE_NAME="${IMAGE_NAME:-vuemind-api}"
PID_FILE="${PID_FILE:-/tmp/vuemind-api-portfolio.pid}"
LOG_FILE="${LOG_FILE:-/tmp/vuemind-api-portfolio.log}"

wait_health() {
  local deadline=$((SECONDS + MAX_WAIT_SECONDS))
  until curl -fsS "$HEALTH_URL" >/dev/null 2>&1; do
    if (( SECONDS >= deadline )); then
      echo "Timeout aguardando health: $HEALTH_URL" >&2
      return 1
    fi
    sleep 2
  done
}

if [[ "${USE_DOCKER:-0}" == "1" ]]; then
  docker build -t "$IMAGE_NAME" .
  docker rm -f "$IMAGE_NAME" >/dev/null 2>&1 || true
  docker run -d --name "$IMAGE_NAME" -p "${PORT}:8080" -e SERVER_PORT=8080 "$IMAGE_NAME"
  wait_health
  echo "OK vuemind-api (docker) $HEALTH_URL"
  exit 0
fi

./mvnw -q -DskipTests package
JAR="$(ls -1 target/vuemind-api-*.jar | grep -v '\.original$' | head -n1)"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Já rodando PID $(cat "$PID_FILE")"
else
  nohup java ${JAVA_OPTS:-} -jar "$JAR" --server.port="$PORT" >"$LOG_FILE" 2>&1 &
  echo $! >"$PID_FILE"
fi

wait_health
echo "OK vuemind-api $HEALTH_URL"
echo "PID $(cat "$PID_FILE")  log $LOG_FILE"
echo "Parar: kill \$(cat $PID_FILE)"
