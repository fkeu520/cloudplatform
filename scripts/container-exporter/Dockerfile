# platform-container-exporter 镜像
# 用于容器资源监控 (替代 cAdvisor, 因 gcr.io 在国内不可达)
# 镜像地址: ghcr.io/fkeu520/cloudplatform/platform-container-exporter:latest

FROM python:3.12-slim

# 减小镜像体积 (--no-cache-dir + 清理 apt cache)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 装 prometheus-client (无缓存, 单层)
RUN pip install --no-cache-dir prometheus-client

WORKDIR /app

# 拷贝 exporter.py (修复版本: Docker 29.x memory_stats + 无版本 API + start_http_server)
COPY exporter.py /app/exporter.py

# 健康检查 (命中 /metrics 即认为健康)
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -fsS http://localhost:9091/metrics > /dev/null || exit 1

# 暴露 Prometheus scrape 端口
EXPOSE 9091

# 默认配置 (可被环境变量覆盖)
ENV EXPORTER_PORT=9091 \
    EXPORTER_INTERVAL=15

CMD ["python", "/app/exporter.py"]