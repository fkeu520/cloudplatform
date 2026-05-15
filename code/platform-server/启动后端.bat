@echo off
chcp 65001 >nul
title 云枢中台后端服务

echo ========================================
echo 云枢中台后端服务
echo ========================================
echo.

cd /d "%~dp0"

echo 正在启动后端服务...
echo.

if exist "target\platform-admin-1.0.0.jar" (
    java -jar target\platform-admin-1.0.0.jar
) else (
    echo 错误：未找到 JAR 文件，请先编译
    echo 运行：mvn clean package -DskipTests
    pause
)
