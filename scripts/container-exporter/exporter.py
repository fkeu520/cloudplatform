#!/usr/bin/env python3
"""轻量容器指标导出器: 通过 Docker socket 读取 stats, 输出 Prometheus 格式。

不需要 cAdvisor 镜像, 跑在 python:3-slim 容器内, 绑定 Docker socket。
用法: docker run -v /var/run/docker.sock:/var/run/docker.sock python:3-slim python /exporter.py
"""
import json, os, time, subprocess
from http.server import HTTPServer, BaseHTTPRequestHandler
from prometheus_client import Gauge, generate_latest, REGISTRY

# ── Prometheus metrics ──────────────────────────────────────────
g_mem = Gauge('container_memory_working_set_bytes', 'Container RSS', ['name'])
g_cpu = Gauge('container_cpu_usage_seconds', 'Container CPU usage rate (1m)', ['name'])
g_net_rx = Gauge('container_network_receive_bytes', 'Network RX bytes', ['name'])
g_net_tx = Gauge('container_network_transmit_bytes', 'Network TX bytes', ['name'])

DOCKER_SOCK = '/var/run/docker.sock'
PLATFORM_PREFIXES = ('platform-', 'node-exporter', 'cadvisor')

def docker_api(path):
    """通过 unix socket 调用 Docker API, 返回 JSON。"""
    import http.client
    conn = http.client.HTTPConnection('localhost', timeout=5)
    conn.sock = None
    try:
        import socket
        s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        s.connect(DOCKER_SOCK)
        conn.sock = s
        conn.request('GET', path)
        resp = conn.getresponse()
        return json.loads(resp.read())
    except Exception as e:
        print(f'[WARN] Docker API error: {e}')
        return {}
    finally:
        conn.close()

def update_metrics():
    """遍历容器, 更新 Prometheus gauge。"""
    containers = docker_api('/v1.24/containers/json?all=false')
    for c in containers:
        name = c.get('Names', [''])[0].lstrip('/')
        if not any(name.startswith(p) for p in PLATFORM_PREFIXES):
            continue

        cid = c['Id']
        stats_raw = docker_api(f'/v1.24/containers/{cid}/stats?stream=false')
        if not stats_raw or 'memory' not in stats_raw:
            continue

        # 内存
        mem_usage = stats_raw.get('memory', {}).get('usage', 0) or 0
        g_mem.labels(name=name).set(mem_usage)

        # CPU (1s 窗口)
        cpu_delta = stats_raw.get('cpu_stats', {}).get('cpu_usage', {}).get('total_usage', 0) or 0
        cpu_system = stats_raw.get('cpu_stats', {}).get('system_cpu_usage', 0) or 0
        precpu_delta = stats_raw.get('precpu_stats', {}).get('cpu_usage', {}).get('total_usage', 0) or 0
        precpu_system = stats_raw.get('precpu_stats', {}).get('system_cpu_usage', 0) or 0
        num_cpus = len(stats_raw.get('cpu_stats', {}).get('online_cpus', [])) or 1
        if cpu_system - precpu_system > 0:
            cpu_pct = (cpu_delta - precpu_delta) / (cpu_system - precpu_system) * num_cpus
        else:
            cpu_pct = 0
        g_cpu.labels(name=name).set(max(0, cpu_pct))

        # 网络
        net = stats_raw.get('networks', {})
        rx = sum(n.get('rx_bytes', 0) for n in net.values()) if net else 0
        tx = sum(n.get('tx_bytes', 0) for n in net.values()) if net else 0
        g_net_rx.labels(name=name).set(rx)
        g_net_tx.labels(name=name).set(tx)

class MetricsHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/metrics':
            update_metrics()
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain; charset=utf-8')
            self.end_headers()
            self.wfile.write(generate_latest(REGISTRY))
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    PORT = int(os.environ.get('EXPORTER_PORT', '9091'))
    print(f'Container exporter listening on :{PORT}')
    HTTPServer(('0.0.0.0', PORT), MetricsHandler).serve_forever()
