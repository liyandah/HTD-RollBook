#!/bin/bash
# Run this ON THE SERVER after uploading the new JAR.
# Usage: bash restart_backend.sh

set -e

JAR="/home/liyandah/htf-data-collection-0.0.1.jar"
LOG="/home/liyandah/backend.log"

if [ ! -f "$JAR" ]; then
  echo "ERROR: JAR not found at $JAR"
  exit 1
fi

echo "Stopping old backend..."
pkill -f htf-data-collection-0.0.1.jar || true
sleep 2

# Make sure nothing is still listening on 8599
if pgrep -f htf-data-collection-0.0.1.jar >/dev/null; then
  echo "ERROR: Old process still running. Try: pkill -9 -f htf-data-collection-0.0.1.jar"
  exit 1
fi

echo "Starting new backend..."
nohup java -jar "$JAR" \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/salvation_army_db \
  --spring.datasource.username=postgres \
  --spring.datasource.password='${DATABASE_PASSWORD}' \
  > "$LOG" 2>&1 &

sleep 5

if pgrep -f htf-data-collection-0.0.1.jar >/dev/null; then
  echo "Backend started."
  echo "Process:"
  pgrep -af htf-data-collection-0.0.1.jar
  echo ""
  echo "Last log lines:"
  tail -n 20 "$LOG"
else
  echo "ERROR: Backend did not start. Check log:"
  tail -n 50 "$LOG"
  exit 1
fi
