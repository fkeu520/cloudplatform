#!/usr/bin/env python3
"""本地测试: 验证 exporter.py 能正确解析 Docker 29.x stats 响应。

不依赖真实 Docker daemon, 用 mock 数据测试字段读取和指标更新。
跑法: python test_exporter.py
"""
import sys
import os
import json
import socket
import http.client
from unittest.mock import patch


class FakeGauge:
    """简化版 prometheus_client.Gauge.

    用法: gauge.labels(name='x').set(100) → gauge.get('x') == 100
    """
    def __init__(self, *args, **kwargs):
        self._values = {}
        self._current = None

    def labels(self, **kwargs):
        name = kwargs.get('name', '')
        self._values.setdefault(name, 0)
        self._current = name
        return self

    def set(self, v):
        self._values[self._current] = int(v)

    def get(self, name):
        return self._values.get(name, 0)


class FakePromModule:
    """简化版 prometheus_client 模块."""
    Gauge = FakeGauge

    @staticmethod
    def start_http_server(*args, **kwargs):
        pass


sys.modules['prometheus_client'] = FakePromModule()

# 把 scripts/container-exporter 加到 path
sys.path.insert(0, os.path.join(os.path.dirname(__file__)))

import exporter  # noqa: E402


# Helper: 读取一个 Gauge 当前值
def gauge_value(g, name):
    return g.get(name)


def make_mock_stats_v29():
    """模拟 Docker 29.5.3 的 stats 响应 (memory_stats 字段, 无 memory 别名)."""
    return {
        'memory_stats': {
            'usage': 524288000,  # 500 MB
            'max_usage': 1073741824,
            'stats': {
                'cache': 104857600,
                'rss': 419430400,  # 400 MB
            },
        },
        'cpu_stats': {
            'cpu_usage': {
                'total_usage': 8000000000,  # 8s in ns
                'usage_in_usermode': 7000000000,
                'usage_in_kernelmode': 1000000000,
            },
            'system_cpu_usage': 20000000000,  # 20s in ns
            'online_cpus': 6,
        },
        'precpu_stats': {
            'cpu_usage': {'total_usage': 7500000000},
            'system_cpu_usage': 19500000000,
        },
        'networks': {
            'eth0': {'rx_bytes': 1048576, 'tx_bytes': 524288},  # 1MB RX, 512KB TX
        },
    }


def make_mock_stats_legacy():
    """模拟旧 Docker API (memory 字段 + 系统容器名字不带 platform- 前缀)."""
    return {
        'memory': {
            'usage': 268435456,  # 256 MB
            'stats': {'rss': 134217728, 'cache': 134217728},
        },
        'cpu_stats': {
            'cpu_usage': {'total_usage': 4000000000},
            'system_cpu_usage': 10000000000,
        },
        'networks': {},
    }


def test_v29_memory_stats_field():
    """验证: 读 memory_stats 而非 memory (Docker 29.x)."""
    with patch.object(exporter, 'docker_api') as mock_api:
        # 第一次调用返回容器列表, 第二次返回 stats
        mock_api.side_effect = [
            [{'Names': ['/platform-test-svc'], 'Id': 'abc123'}],
            make_mock_stats_v29(),
        ]
        exporter.update_metrics()

    # 验证指标值
    mem_rss = gauge_value(exporter.g_mem_rss, 'platform-test-svc')
    mem_usage = gauge_value(exporter.g_mem_usage, 'platform-test-svc')
    cpu_total = gauge_value(exporter.g_cpu_total_ns, 'platform-test-svc')
    net_rx = gauge_value(exporter.g_net_rx, 'platform-test-svc')
    net_tx = gauge_value(exporter.g_net_tx, 'platform-test-svc')

    assert mem_rss == 419430400, f'mem_rss wrong: {mem_rss}'
    assert mem_usage == 524288000, f'mem_usage wrong: {mem_usage}'
    assert cpu_total == 8000000000, f'cpu_total wrong: {cpu_total}'
    assert net_rx == 1048576, f'net_rx wrong: {net_rx}'
    assert net_tx == 524288, f'net_tx wrong: {net_tx}'
    print('[PASS] v29 memory_stats field parsing')


def test_legacy_memory_field_fallback():
    """验证: 兼容旧 API (memory 字段) - 仅 memory_stats 不存在时使用."""
    # 注意: 不在 PLATFORM_PREFIXES 的容器会被跳过, 这里用 platform- 前缀
    with patch.object(exporter, 'docker_api') as mock_api:
        mock_api.side_effect = [
            [{'Names': ['/platform-legacy-svc'], 'Id': 'xyz789'}],
            make_mock_stats_legacy(),
        ]
        exporter.update_metrics()

    mem_rss = gauge_value(exporter.g_mem_rss, 'platform-legacy-svc')
    assert mem_rss == 134217728, f'mem_rss fallback wrong: {mem_rss}'
    print('[PASS] legacy memory field fallback')


def test_skip_non_platform_containers():
    """验证: 跳过非项目容器 (名字不以 platform-/node-exporter/cadvisor 等开头)."""
    with patch.object(exporter, 'docker_api') as mock_api:
        mock_api.side_effect = [
            [{'Names': ['/redis'], 'Id': 'redis123'}],  # 不在白名单
            # 不应该有第二次调用
        ]
        exporter.update_metrics()

    # 验证 redis 没被加指标 (只调用了一次 api, 第二次 skip)
    assert mock_api.call_count == 1, f'expected 1 call, got {mock_api.call_count}'
    print('[PASS] skip non-platform containers')


def test_empty_docker_response():
    """验证: Docker API 失败时返回空 dict, 不崩."""
    with patch.object(exporter, 'docker_api', return_value={}):
        exporter.update_metrics()  # 应该不抛异常
    print('[PASS] empty Docker response handled gracefully')


def test_stats_no_memory_stats_field():
    """验证: 当 stats 响应完全没有 memory_stats 和 memory 字段时, 跳过内存更新但不崩."""
    with patch.object(exporter, 'docker_api') as mock_api:
        mock_api.side_effect = [
            [{'Names': ['/platform-empty'], 'Id': 'empty123'}],
            {'cpu_stats': {}, 'networks': {}},  # 没有 memory_stats
        ]
        exporter.update_metrics()
    print('[PASS] stats without memory_stats field handled')


def test_multi_network_interface_aggregation():
    """验证: 多网络接口的 RX/TX 累加正确."""
    stats = {
        'memory_stats': {'usage': 0, 'stats': {}},
        'cpu_stats': {'cpu_usage': {'total_usage': 0}, 'system_cpu_usage': 0},
        'networks': {
            'eth0': {'rx_bytes': 1000, 'tx_bytes': 500},
            'eth1': {'rx_bytes': 2000, 'tx_bytes': 1500},
        },
    }
    with patch.object(exporter, 'docker_api') as mock_api:
        mock_api.side_effect = [
            [{'Names': ['/platform-multi-net'], 'Id': 'multi123'}],
            stats,
        ]
        exporter.update_metrics()

    rx = gauge_value(exporter.g_net_rx, 'platform-multi-net')
    tx = gauge_value(exporter.g_net_tx, 'platform-multi-net')
    assert rx == 3000, f'expected rx=3000, got {rx}'
    assert tx == 2000, f'expected tx=2000, got {tx}'
    print('[PASS] multi-network interface aggregation')


def test_api_path_no_version_prefix():
    """验证: API 路径不带版本前缀 (Docker 29.x 自动协商)."""
    # 这个测试通过 monkey-patch 检查 update_metrics 调用的路径
    captured_paths = []

    def mock_api(path):
        captured_paths.append(path)
        return []

    with patch.object(exporter, 'docker_api', side_effect=mock_api):
        exporter.update_metrics()

    # 验证第一次调用是 /containers/json (不带 v1.24)
    assert any('/containers/json' in p and 'v1.24' not in p for p in captured_paths), \
        f'expected no version prefix, got: {captured_paths}'
    print(f'[PASS] API path no version prefix: {captured_paths}')


def test_cpu_percent_with_precpu_delta():
    """验证: CPU 使用率 % 正确计算 (用 precpu_stats 算 delta)."""
    stats = {
        'memory_stats': {'usage': 0, 'stats': {}},
        'cpu_stats': {
            'cpu_usage': {'total_usage': 8000000000},  # 当前: 8s
            'system_cpu_usage': 20000000000,           # 系统: 20s
            'online_cpus': 6,
        },
        'precpu_stats': {
            'cpu_usage': {'total_usage': 7500000000},   # 上次: 7.5s → delta 0.5s
            'system_cpu_usage': 19500000000,           # 上次: 19.5s → delta 0.5s
        },
        'networks': {},
    }
    with patch.object(exporter, 'docker_api') as mock_api:
        mock_api.side_effect = [
            [{'Names': ['/platform-cpu-1'], 'Id': 'cpu1'}],
            stats,
        ]
        exporter.update_metrics()

    cpu_pct = gauge_value(exporter.g_cpu_percent, 'platform-cpu-1')
    # (8 - 7.5) / (20 - 19.5) * 100 = 0.5 / 0.5 * 100 = 100%
    assert abs(cpu_pct - 100.0) < 0.01, f'expected ~100%, got {cpu_pct}'
    print(f'[PASS] CPU percent with precpu delta: {cpu_pct}%')


def test_cpu_percent_zero_on_first_collect():
    """验证: 首次采集无 precpu delta 时, CPU % 不设置 (无数据而非 0)."""
    stats = {
        'memory_stats': {'usage': 0, 'stats': {}},
        'cpu_stats': {
            'cpu_usage': {'total_usage': 8000000000},
            'system_cpu_usage': 20000000000,
        },
        'precpu_stats': {
            'cpu_usage': {'total_usage': 8000000000},  # delta = 0
            'system_cpu_usage': 20000000000,           # delta = 0
        },
        'networks': {},
    }
    with patch.object(exporter, 'docker_api') as mock_api:
        mock_api.side_effect = [
            [{'Names': ['/platform-cpu-2'], 'Id': 'cpu2'}],
            stats,
        ]
        exporter.update_metrics()

    # delta = 0, 跳过设置, gauge 应保持初始 0 (FakeGauge default)
    cpu_pct = gauge_value(exporter.g_cpu_percent, 'platform-cpu-2')
    # 注意: FakeGauge 默认 0, 真实 Gauge 不会变 (因为没调 .set()).
    # 这里我们只能验证没崩 + 不会得到 100% 这种错误值
    assert cpu_pct < 100, f'should not be 100 when no delta, got {cpu_pct}'
    print(f'[PASS] CPU percent no-op on zero delta: {cpu_pct}%')


def test_cpu_percent_low_usage():
    """验证: 低 CPU 使用率 (5% 空闲, 1 核满载 → 100% 报告)."""
    stats = {
        'memory_stats': {'usage': 0, 'stats': {}},
        'cpu_stats': {
            'cpu_usage': {'total_usage': 1000000000},   # 1s CPU
            'system_cpu_usage': 20000000000,            # 20s 系统 (6 核)
        },
        'precpu_stats': {
            'cpu_usage': {'total_usage': 0},             # 首次: 0 → delta 1s
            'system_cpu_usage': 0,                       # 首次: 0 → delta 20s
        },
        'networks': {},
    }
    with patch.object(exporter, 'docker_api') as mock_api:
        mock_api.side_effect = [
            [{'Names': ['/platform-cpu-3'], 'Id': 'cpu3'}],
            stats,
        ]
        exporter.update_metrics()

    cpu_pct = gauge_value(exporter.g_cpu_percent, 'platform-cpu-3')
    # (1 - 0) / (20 - 0) * 100 = 5%
    assert abs(cpu_pct - 5.0) < 0.01, f'expected ~5%, got {cpu_pct}'
    print(f'[PASS] CPU percent low usage: {cpu_pct}%')


if __name__ == '__main__':
    print('=== exporter.py 测试套件 ===')
    print()

    test_v29_memory_stats_field()
    test_legacy_memory_field_fallback()
    test_skip_non_platform_containers()
    test_empty_docker_response()
    test_stats_no_memory_stats_field()
    test_multi_network_interface_aggregation()
    test_api_path_no_version_prefix()
    test_cpu_percent_with_precpu_delta()
    test_cpu_percent_zero_on_first_collect()
    test_cpu_percent_low_usage()

    print()
    print('=== ALL TESTS PASSED ===')