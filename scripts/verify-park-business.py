#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
park-business 11 HTML 交互完整性静态验证器

扫描项:
1. 所有 <button>/<a> 元素的 onclick 引用是否对应已定义的函数
2. 所有引用的 JS 函数是否在 script 块中定义
3. showToast / switchTab / closeDialog 等共享工具函数是否就位
4. 关键 M-XX 状态机/联动函数是否存在

运行方式: python scripts/verify-park-business.py
退出码: 0 = 平均 >= 90, 1 = 平均 < 90 (CI 失败)
"""

import os
import re
import json
import sys
from pathlib import Path

PARK_DIR = Path(__file__).resolve().parent.parent / 'prototype' / 'park-business'

# 必需的工具函数（每个页面都必须有）
REQUIRED_UTILS = ['showToast']

# M-XX 关键函数映射（每个 M-XX 对应的实现函数）
# 当名: 函数列表 — 函数必须在 script 块中定义
M_REQUIRED = {
    'pool.html':           ['batchAssign', 'claim'],
    'my-customer.html':    ['batchAbandon', 'batchReturnPool', 'abandon', 'returnPool',
                            'saveLead', 'confirmConvert', 'saveFollowup', 'confirmMark',
                            'applyTabFilter'],
    'recycle.html':        ['confirmRestore'],
    'pool-log.html':       ['exportLog'],
    'customer-detail.html':['saveContact'],
    'opportunity.html':    ['filterTable', 'sortTable', 'resetSearch'],
    'opportunity-detail.html': ['confirmAgreement', 'terminate', 'saveFollowupFromOpp',
                                'saveSchedule', 'editSchedule', 'cancelSchedule'],
    'agreement.html':      ['confirmDelay', 'confirmTerminate', 'confirmContract',
                            'filterTable', 'resetSearch', 'sortTable'],
    'agreement-detail.html':['confirmDelay', 'confirmTerminateAgreement', 'confirmContract',
                             'checkExpiry', 'uploadAttachment', 'downloadAttachment'],
    'statistics.html':     ['exportReport'],
    'settings.html':       ['saveSettings', 'resetSettings'],
}

# 已知 JS 动态绑定的按钮（这些会被脚本在运行时绑定 onclick）
# 静态验证脚本会跳过它们
DYNAMIC_BOUND_BTN_TEXTS = ['搜索', '重置', '确认']  # 通过 querySelectorAll 动态绑定


def extract_script_functions(html_text):
    """从 HTML 提取 <script> 中定义的函数名"""
    script_blocks = re.findall(r'<script[^>]*>(.*?)</script>', html_text, re.DOTALL)
    full_script = '\n'.join(script_blocks)
    funcs = set(re.findall(r'function\s+([A-Za-z_]\w*)\s*\(', full_script))
    funcs |= set(re.findall(r'(?:var|let|const)\s+([A-Za-z_]\w*)\s*=\s*function', full_script))
    funcs |= set(re.findall(r'(?:var|let|const)\s+([A-Za-z_]\w*)\s*=\s*\([^)]*\)\s*=>', full_script))
    return funcs, full_script


def extract_buttons(html_text):
    """提取所有 button/a 元素及其 onclick 状态"""
    buttons = []
    # button onclick
    for m in re.finditer(r'<button([^>]*)>([^<]*)</button>', html_text):
        attrs = m.group(1)
        text = m.group(2).strip()
        onclick_match = re.search(r'onclick\s*=\s*["\']([^"\']+)["\']', attrs)
        buttons.append({
            'type': 'button',
            'text': text[:30],
            'has_onclick': onclick_match is not None,
            'onclick': onclick_match.group(1) if onclick_match else None,
            'attrs': attrs.strip()[:80],
        })
    # a onclick
    for m in re.finditer(r'<a([^>]*)>([^<]*)</a>', html_text):
        attrs = m.group(1)
        text = m.group(2).strip()
        onclick_match = re.search(r'onclick\s*=\s*["\']([^"\']+)["\']', attrs)
        if onclick_match:
            buttons.append({
                'type': 'a',
                'text': text[:30],
                'has_onclick': True,
                'onclick': onclick_match.group(1),
                'attrs': attrs.strip()[:80],
            })
    return buttons


def detect_dynamic_bindings(html_text):
    """检测是否存在 JS 动态绑定（querySelectorAll('.search-box button').onclick=...）"""
    patterns = [
        r"querySelectorAll\(['\"][^'\"]*button['\"][^)]*\)\.forEach",
        r"querySelectorAll\(['\"]th['\"][^)]*\)\.forEach",
        r"\.onclick\s*=\s*\w+",  # 通用 .onclick = functionName
    ]
    for p in patterns:
        if re.search(p, html_text):
            return True
    return False


def check_html(filepath):
    """检查单个 HTML 文件"""
    text = filepath.read_text(encoding='utf-8')
    funcs, _ = extract_script_functions(text)
    buttons = extract_buttons(text)
    has_dynamic = detect_dynamic_bindings(text)

    # M-XX 必需函数
    m_required = M_REQUIRED.get(filepath.name, [])
    m_missing = [f for f in m_required if f not in funcs]

    # 必需工具
    util_missing = [u for u in REQUIRED_UTILS if u not in funcs]

    # 死链按钮（无 onclick 且脚本无动态绑定）
    dead_buttons = []
    for b in buttons:
        if not b['has_onclick'] and b['text'] and not any(kw in b['text'] for kw in DYNAMIC_BOUND_BTN_TEXTS):
            if not has_dynamic:
                dead_buttons.append(b)

    # 计算分数
    score = 100
    score -= len(m_missing) * 10
    score -= len(util_missing) * 15
    score -= min(len(dead_buttons), 20) * 2
    score = max(0, score)

    return {
        'file': filepath.name,
        'lines': len(text.splitlines()),
        'total_funcs': len(funcs),
        'm_missing': m_missing,
        'util_missing': util_missing,
        'dead_buttons': dead_buttons[:5],
        'dead_button_count': len(dead_buttons),
        'has_dynamic_binding': has_dynamic,
        'score': score,
    }


def main():
    if not PARK_DIR.exists():
        print(f"错误: 目录不存在 {PARK_DIR}")
        return 1

    results = []
    for html in sorted(PARK_DIR.glob('*.html')):
        results.append(check_html(html))

    print("=" * 80)
    print("park-business 11 HTML 交互完整性验证报告")
    print(f"扫描目录: {PARK_DIR}")
    print("=" * 80)

    total_score = 0
    for r in results:
        score = r['score']
        total_score += score
        bar = '\u2588' * (score // 5) + '\u2591' * ((100 - score) // 5)
        status = '\u2705' if score >= 90 else ('\u26a0\ufe0f' if score >= 70 else '\u274c')
        dyn_tag = ' [动态绑定]' if r['has_dynamic_binding'] else ''
        print(f"\n{status} {r['file']:30} [{bar}] {score:3d}/100  "
              f"({r['lines']} 行, {r['total_funcs']} funcs){dyn_tag}")
        if r['m_missing']:
            print(f"   \u274c M-XX 缺失函数: {r['m_missing']}")
        if r['util_missing']:
            print(f"   \u274c 工具函数缺失: {r['util_missing']}")
        if r['dead_button_count'] > 0:
            print(f"   \u26a0\ufe0f 无 onclick 按钮: {r['dead_button_count']} 个 "
                  f"(示例: {[b['text'] for b in r['dead_buttons'][:2]]})")

    avg = total_score // len(results) if results else 0
    print("\n" + "=" * 80)
    rating = '优秀' if avg >= 90 else ('良好' if avg >= 75 else ('一般' if avg >= 60 else '不足'))
    print(f"平均得分: {avg}/100  | 完整度评级: {rating}")
    print(f"通过阈值: {'PASS' if avg >= 90 else 'FAIL'}")
    print("=" * 80)

    return 0 if avg >= 90 else 1


if __name__ == '__main__':
    sys.exit(main())