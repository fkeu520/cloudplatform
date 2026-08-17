#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
kefu 8 HTML 交互完整性静态验证器

扫描项:
1. 所有 onclick 引用是否对应已定义的函数
2. showToast / closeDialog 等共享工具函数是否就位
4. 关键 M-XX 函数是否存在 (chat/generateAIReply, knowledge/saveKnowledge 等)

"""

import re
import sys
from pathlib import Path

KEFU_DIR = Path(__file__).resolve().parent.parent / 'prototype' / 'kefu'

REQUIRED_UTILS = ['showToast']

# 每个文件的关键函数清单
REQUIRED_FUNCS = {
    'chat.html':       ['generateAIReply', 'switchSession', 'sendMessage', 'escalateToHuman', 'closeSession', 'showToast'],
    'knowledge.html':  ['saveKnowledge', 'editKnowledge', 'deleteKnowledge', 'filterTable', 'sortTable', 'showToast'],
    'dashboard.html':  ['setRange', 'exportReport', 'showDetail', 'showToast'],
    'faq.html':        ['saveFaq', 'editFaq', 'deleteFaq', 'filterTable', 'showToast'],
    'logs.html':       ['viewLog', 'deleteLog', 'exportLogs', 'filter', 'showToast'],
    'import.html':     ['handleFile', 'handleDrop', 'processFile', 'showToast'],
    'settings.html':   ['toggleDS', 'registerDataSource', 'confirmRegister', 'newScenario', 'showToast'],
    'evaluation.html': ['exportReport', 'showToast'],
}


def check_file(filepath):
    text = filepath.read_text(encoding='utf-8')
    script_blocks = re.findall(r'<script[^>]*>(.*?)</script>', text, re.DOTALL)
    full_script = '\n'.join(script_blocks)
    funcs = set(re.findall(r'function\s+([A-Za-z_]\w*)\s*\(', full_script))
    funcs |= set(re.findall(r'(?:var|let|const)\s+([A-Za-z_]\w*)\s*=\s*function', full_script))

    # onclick 引用
    onclick_funcs = set()
    for m in re.finditer(r'onclick\s*=\s*["\']([^"\']+)["\']', text):
        for fm in re.finditer(r'\b([a-zA-Z_]\w*)\s*\(', m.group(1)):
            fn = fm.group(1)
            if fn not in ('parent','event','this','document','window','open','for','while','if'):
                onclick_funcs.add(fn)

    parent_calls = set(re.findall(r'parent\.(\w+)', text))
    unresolved = onclick_funcs - funcs - parent_calls

    required = REQUIRED_FUNCS.get(filepath.name, [])
    missing = [f for f in required if f not in funcs]
    util_missing = [u for u in REQUIRED_UTILS if u not in funcs]

    score = 100
    score -= len(missing) * 10
    score -= len(util_missing) * 15
    score -= len(unresolved) * 5
    score = max(0, score)
    return {
        'file': filepath.name,
        'lines': len(text.splitlines()),
        'total_funcs': len(funcs),
        'required': required,
        'missing': missing,
        'util_missing': util_missing,
        'unresolved': sorted(unresolved),
        'score': score,
    }


def main():
    results = [check_file(f) for f in sorted(KEFU_DIR.glob('*.html'))]
    print('=' * 80)
    print('kefu 8 HTML 交互完整性验证')
    print(f'扫描目录: {KEFU_DIR}')
    print('=' * 80)
    total = 0
    for r in results:
        score = r['score']
        total += score
        bar = '\u2588' * (score // 5) + '\u2591' * ((100 - score) // 5)
        status = '\u2705' if score >= 90 else ('\u26a0\ufe0f' if score >= 70 else '\u274c')
        print(f"\n{status} {r['file']:24} [{bar}] {score:3d}/100  ({r['lines']} 行, {r['total_funcs']} funcs)")
        if r['missing']:
            print(f"   \u274c 缺失函数: {r['missing']}")
        if r['util_missing']:
            print(f"   \u274c 工具函数: {r['util_missing']}")
        if r['unresolved']:
            print(f"   \u26a0\ufe0f 未定义引用: {r['unresolved']}")
    avg = total // len(results) if results else 0
    print('\n' + '=' * 80)
    print(f'平均: {avg}/100  | {("PASS" if avg >= 90 else "FAIL")}')
    print('=' * 80)
    return 0 if avg >= 90 else 1


if __name__ == '__main__':
    sys.exit(main())