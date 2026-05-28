#!/bin/bash
# P3-9: 导出 OpenAPI 3.0 YAML 文档
set -e

OUTPUT_DIR="$(dirname "$0")/../docs/api"
mkdir -p "$OUTPUT_DIR"

SERVICES=("platform-user:8081" "platform-auth:8082" "platform-workflow:8084" "platform-message:8085" "platform-ops:8087")

for svc in "${SERVICES[@]}"; do
    name="${svc%%:*}"
    port="${svc##*:}"
    echo "导出 $name API 文档..."
    curl -s "http://localhost:$port/v3/api-docs" -o "$OUTPUT_DIR/${name}-api.yaml" 2>/dev/null && \
        echo "  ✅ $name" || echo "  ❌ $name (服务未运行)"
done

echo "=== OpenAPI 文档导出完成 ==="
ls -la "$OUTPUT_DIR/"
