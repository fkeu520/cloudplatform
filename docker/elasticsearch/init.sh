#!/bin/sh
# ==============================================================================
# Elasticsearch Init Script
# ==============================================================================
# 绛夊緟 ES 灏辩华鍚庡垱寤?ILM 绛栫暐鍜岀储寮曟ā鏉?
set -e

ES_URL="http://elasticsearch:9200"
MAX_RETRIES=30
RETRY_INTERVAL=5

echo "[ES-INIT] Waiting for Elasticsearch at $ES_URL..."
for i in $(seq 1 $MAX_RETRIES); do
  if curl -sf "$ES_URL/_cluster/health" >/dev/null 2>&1; then
    echo "[ES-INIT] Elasticsearch is ready"
    break
  fi
  if [ "$i" = "$MAX_RETRIES" ]; then
    echo "[ES-INIT] ERROR: Elasticsearch not ready after $MAX_RETRIES retries"
    exit 1
  fi
  sleep $RETRY_INTERVAL
done

echo "[ES-INIT] Creating ILM policy: platform-logs-1d-retention"
curl -sf -X PUT "$ES_URL/_ilm/policy/platform-logs-1d-retention" \
  -H 'Content-Type: application/json' \
  -d @/init/ilm-policy.json && echo " OK" || echo " FAILED"

echo "[ES-INIT] Creating index template: platform-logs-template"
curl -sf -X PUT "$ES_URL/_index_template/platform-logs-template" \
  -H 'Content-Type: application/json' \
  -d @/init/index-template.json && echo " OK" || echo " FAILED"

echo "[ES-INIT] Done"
