# ================================================
# 云枢中台 Phase 1 启动脚本
# 启动顺序：Nacos → Gateway → User → Auth
# ================================================

@echo off
chcp 65001 >nul
title 云枢中台 - Phase 1 启动

setlocal enabledelayedexpansion

echo ========================================
echo   云枢中台 Phase 1 启动
echo ========================================
echo.

echo [1/4] 检查 Nacos (8848)...
curl -s http://localhost:8848/nacos/v1/console/health/readiness >nul 2>&1
if errorlevel 1 (
    echo   [WARN] Nacos 未启动，请先执行 docker-compose up -d
    echo   或手动启动 Nacos 后继续。
    echo.
    pause
    exit /b 1
)
echo   OK

echo [2/4] 启动 Gateway (8080)...
start "platform-gateway" cmd /c "cd /d %~dp0 && java -jar platform-gateway\target\platform-gateway-1.0.0-SNAPSHOT.jar"
echo   已在新窗口启动 Gateway

echo.
echo [3/4] 启动 User (8081)...
start "platform-user" cmd /c "cd /d %~dp0 && java -jar platform-user\target\platform-user-1.0.0-SNAPSHOT.jar"
echo   已在新窗口启动 User

echo.
echo [4/4] 启动 Auth (8082)...
start "platform-auth" cmd /c "cd /d %~dp0 && java -jar platform-auth\target\platform-auth-1.0.0-SNAPSHOT.jar"
echo   已在新窗口启动 Auth

echo.
echo ========================================
echo   全部启动完成！
echo ========================================
echo.
echo 服务地址：
echo   网关    http://localhost:8080
echo   用户中心 http://localhost:8081
echo   认证中心 http://localhost:8082
echo   Nacos   http://localhost:8848/nacos
echo.
echo 测试账号：admin / 123456
echo.
pause