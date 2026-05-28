#!/bin/bash
# P3-8: CI 依赖分析
set -e

echo "=== Maven 依赖分析 ==="
cd "$(dirname "$0")/../code/platform-server"

mvn dependency:analyze -q 2>&1 || {
    echo "⚠️  依赖分析发现问题，请检查上述警告"
}

echo "=== 依赖分析完成 ==="
