#!/usr/bin/env python3
"""轻量容器指标导出器: 通过 Docker socket 读取 stats, 输出 Prometheus 格式。

修复版本 (2026-06-15):
- 去掉固定 API 版本前缀 (Docker 自动协商, 兼容 29.x)
- 改 memory_stats 字段 (Docker 29.x 已废弃 memory 别名)
- 用 start_http_server 多线程 (替代 BaseHTTPRequestHandler, 避免 BrokenPipe)
- 周期性采集 (15s 间隔, 避免每次 scrape 都读 socket)
- CPU/网络用 Gauge 暴露累计值, Prometheus 端用 rate() 转速率

v2 (2026-06-29): 加 container_cpu_usage_percent 直接暴露 0-100 百分比
- 用 Docker API 自带的 precpu_stats 算 delta (无需自己缓存上一次)
- 公式: (cpu_total_delta / system_cpu_delta) * 100, 类似 `docker stats` 输出
- 兼容 precpu_stats 缺失 (首次采集无 delta) 的情况

历史: KNOWN_ISSUES #21 (2026-06-11) 已记录修复要点, 但 #24 回归发现 exporter.py
实际仍用旧字段 'memory' + 固定 v1.24 路径, 导致 Grafana "容器资源排行" 空数据。
"""
import json
import os
import socket
import threading
import time
import http.client

from prometheus_client import start_http_server, Gauge

# ── Prometheus metrics (Gauge: 最新一次采集的快照值) ───────────────
# 累计值类型 (CPU 纳秒 / 网络字节) 用 Gauge, Prometheus 端用 rate() 计算速率
g_mem_rss = Gauge(
    'container_memory_rss_bytes',
    'Container RSS memory (resident set size)',
    ['name'],
)
g_mem_usage = Gauge(
    'container_memory_usage_bytes',
    'Container total memory usage (incl. cache)',
    ['name'],
)
g_cpu_total_ns = Gauge(
    'container_cpu_usage_nanoseconds_total',
    'Container CPU usage cumulative (nanoseconds)',
    ['name'],
)
g_cpu_system_ns = Gauge(
    'container_cpu_system_nanoseconds_total',
    'System CPU usage cumulative (nanoseconds, for ratio calc)',
    ['name'],
)
# v2: 直接暴露 CPU 使用率 (0-100), 避免 Prometheus/Grafana 端手动算 rate
g_cpu_percent = Gauge(
    'container_cpu_usage_percent',
    'Container CPU usage percentage (0-100, all cores summed)',
    ['name'],
)
g_net_rx = Gauge(
    'container_network_receive_bytes_total',
    'Container network RX cumulative (bytes)',
    ['name'],
)
g_net_tx = Gauge(
    'container_network_transmit_bytes_total',
    'Container network TX cumulative (bytes)',
    ['name'],
)

DOCKER_SOCK = '/var/run/docker.sock'
PLATFORM_PREFIXES = (
    'platform-',         # 项目服务: platform-user / platform-message 等
    'park-',             # 园区服务: park-space / park-property / park-contract / park-enterprise
    'node-exporter',     # 宿主机监控
    'cadvisor',          # 历史 cAdvisor 容器 (兼容)
    'container-exporter',# 当前 exporter 自身 (避免自监控)
)
COLLECT_INTERVAL = int(os.environ.get('EXPORTER_INTERVAL', '15'))


def docker_api(path):
    """通过 unix socket 调用 Docker API, 返回 JSON dict (失败返回空 dict)."""
    conn = http.client.HTTPConnection('localhost', timeout=5)
    try:
        s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        s.connect(DOCKER_SOCK)
        conn.sock = s
        conn.request('GET', path)
        resp = conn.getresponse()
        return json.loads(resp.read())
    except Exception as e:
        print(f'[WARN] Docker API error ({path}): {e}')
        return {}
    finally:
        try:
            conn.close()
        except Exception:
            pass


def update_metrics():
    """遍历运行中的容器, 更新 Prometheus gauge."""
    # 不带版本前缀, Docker 自动协商 (兼容 29.x)
    containers = docker_api('/containers/json?all=false')
    if not containers:
        return

    for c in containers:
        name = (c.get('Names') or [''])[0].lstrip('/')
        if not name:
            continue
        if not any(name.startswith(p) for p in PLATFORM_PREFIXES):
            continue

        cid = c.get('Id')
        if not cid:
            continue

        stats_raw = docker_api(f'/containers/{cid}/stats?stream=false')
        if not stats_raw:
            continue

        # ── 内存 (Docker 29.x 用 memory_stats, 旧版本 memory 是别名) ──
        mem = stats_raw.get('memory_stats') or stats_raw.get('memory') or {}
        if mem:
            # 优先 stats 子对象 (cgroup v2), 退回到 usage (cgroup v1)
            rss = (
                (mem.get('stats') or {}).get('rss')
                or mem.get('usage', 0)
                or 0
            )
            g_mem_rss.labels(name=name).set(int(rss))
            g_mem_usage.labels(name=name).set(int(mem.get('usage', 0) or 0))

        # ── CPU 累计值 (纳秒) ──
        # prometheus 端用 rate(container_cpu_usage_nanoseconds_total[5m]) 转速率
        cpu_stats = stats_raw.get('cpu_stats') or {}
        cpu_usage = cpu_stats.get('cpu_usage') or {}
        cpu_total_ns = int(cpu_usage.get('total_usage', 0) or 0)
        cpu_system_ns = int(cpu_stats.get('system_cpu_usage', 0) or 0)
        g_cpu_total_ns.labels(name=name).set(cpu_total_ns)
        g_cpu_system_ns.labels(name=name).set(cpu_system_ns)

        # ── CPU 使用率 % (0-100, all cores summed) ──
        # 利用 Docker API 自带的 precpu_stats 算两次采集间的 delta
        # 公式与 `docker stats` CLI 一致: (cpu_delta / system_delta) * 100
        # precpu_stats 在容器启动后第一次采集时为 0, 跳过避免除零
        precpu_stats = stats_raw.get('precpu_stats') or {}
        precpu_usage = precpu_stats.get('cpu_usage') or {}
        precpu_total = int(precpu_usage.get('total_usage', 0) or 0)
        precpu_system = int(precpu_stats.get('system_cpu_usage', 0) or 0)
        cpu_delta = cpu_total_ns - precpu_total
        sys_delta = cpu_system_ns - precpu_system
        if cpu_delta > 0 and sys_delta > 0:
            # online_cpus 不参与公式 (Docker stats 内部按 all_cpus 算百分比)
            # 但 * 100 让结果落到 0-100 范围 (无上限, 多核容器可超过 100)
            cpu_pct = (cpu_delta / sys_delta) * 100.0
            g_cpu_percent.labels(name=name).set(round(cpu_pct, 2))
        # else: 首次采集或容器刚启动, 无 delta 数据, 不设置 (保持 -1 默认, 或跳过)

        # ── 网络累计值 (字节) ──
        # prometheus 端用 rate(container_network_receive_bytes_total[5m]) 转速率
        networks = stats_raw.get('networks') or {}
        rx_total = sum(
            int(n.get('rx_bytes', 0) or 0) for n in networks.values()
        ) if networks else 0
        tx_total = sum(
            int(n.get('tx_bytes', 0) or 0) for n in networks.values()
        ) if networks else 0
        g_net_rx.labels(name=name).set(rx_total)
        g_net_tx.labels(name=name).set(tx_total)


def collect_loop():
    """后台周期性采集循环."""
    print(f'[INFO] Collect loop started, interval={COLLECT_INTERVAL}s')
    while True:
        try:
            update_metrics()
        except Exception as e:
            # 单次采集失败不影响下一次
            print(f'[ERROR] update_metrics failed: {e}')
        time.sleep(COLLECT_INTERVAL)


if __name__ == '__main__':
    port = int(os.environ.get('EXPORTER_PORT', '9091'))
    print(f'[INFO] Container exporter listening on :{port}')
    print(f'[INFO] Watched prefixes: {PLATFORM_PREFIXES}')

    # 多线程 HTTP 服务 (start_http_server 自带 thread pool, 不会爆 BrokenPipe)
    start_http_server(port)

    # 后台采集线程
    collector = threading.Thread(target=collect_loop, daemon=True, name='collect-loop')
    collector.start()

    # 主线程保活 (避免容器退出)
    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        print('[INFO] Shutting down...')