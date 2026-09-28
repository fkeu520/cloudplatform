echo "=== Prometheus: container_cpu_usage rate (cores) ==="
curl -s 'http://127.0.0.1:9090/api/v1/query?query=rate(container_cpu_usage_nanoseconds_total%5B5m%5D)' \
  | python3 -m json.tool \
  | head -50