@echo off
chcp 65001 >nul
title 云枢中台 - 启动所有中间件

echo ========================================
echo   云枢中台 Phase 1 中间件启动工具
echo ========================================
echo.

cd /d "%~dp0"

:: 检查 Docker 是否安装
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ Docker 未安装，请先安装 Docker Desktop
    echo   下载地址：https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)

:: 检查 Docker 服务是否运行
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠ Docker 服务未运行
    echo   请打开 Docker Desktop 应用程序，等待小鲸鱼图标稳定
    echo.
    echo   等待中...
    timeout /t 10 /nobreak >nul
    
    :: 再次检查
    docker ps >nul 2>&1
    if %errorlevel% neq 0 (
        echo ✗ Docker 服务启动失败
        echo   请手动启动 Docker Desktop 后重试
        pause
        exit /b 1
    )
)

echo.
echo [1/4] 正在检查 docker-compose.yml...
if not exist "docker-compose.yml" (
    echo ✗ docker-compose.yml 文件不存在
    echo   请确保在项目根目录运行此脚本
    pause
    exit /b 1
)
echo   ✓ docker-compose.yml 存在

echo.
echo [2/4] 正在停止旧容器（如果有）...
docker-compose down >nul 2>&1
echo   ✓ 已停止旧容器

echo.
echo [3/4] 正在启动所有中间件...
docker-compose up -d
if %errorlevel% neq 0 (
    echo ✗ 启动失败，请检查 docker-compose.yml 配置
    docker-compose logs --tail=50
    pause
    exit /b 1
)
echo   ✓ 所有容器已启动

echo.
echo [4/4] 等待服务就绪（约 60 秒）...
echo   正在启动：
echo   - MySQL 8.0 (端口 3306)
echo   - Redis 7 (端口 6379)
echo   - Kafka 3.6 (端口 9092)
echo   - Nacos 2.2 (端口 8848)
echo   - MinIO (端口 9000/9001)
echo   - Elasticsearch (端口 9200)
echo   - Kibana (端口 5601)
echo.

:: 等待服务启动
echo   等待中...
timeout /t 30 /nobreak >nul

:: 检查各服务状态
echo.
echo ========================================
echo   服务启动状态
echo ========================================

:: 检查 MySQL
docker exec platform-mysql mysqladmin ping -h localhost -u root -proot123456 >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ MySQL 8.0 已就绪 (localhost:3306)
) else (
    echo   ⚠ MySQL 正在启动中...
)

:: 检查 Redis
docker exec platform-redis redis-cli ping >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Redis 7 已就绪 (localhost:6379)
) else (
    echo   ⚠ Redis 正在启动中...
)

:: 检查 Kafka
docker exec platform-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Kafka 已就绪 (localhost:9092)
) else (
    echo   ⚠ Kafka 正在启动中...
)

:: 检查 Nacos
curl -s http://localhost:8848/nacos/v1/console/health/readiness >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Nacos 已就绪 (localhost:8848)
) else (
    echo   ⚠ Nacos 正在启动中...
)

:: 检查 MinIO
curl -s http://localhost:9000/minio/health/live >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ MinIO 已就绪 (localhost:9000/9001)
) else (
    echo   ⚠ MinIO 正在启动中...
)

:: 检查 Elasticsearch
curl -s http://localhost:9200 >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Elasticsearch 已就绪 (localhost:9200)
) else (
    echo   ⚠ Elasticsearch 正在启动中...
)

echo.
echo ========================================
echo   所有中间件启动命令执行完成
echo ========================================
echo.
echo   如需查看详细日志，请运行：
echo   docker-compose logs -f
echo.
echo   如需停止所有服务，请运行：
echo   docker-compose down
echo.
echo   访问地址：
echo   - Nacos：http://localhost:8848/nacos
echo   - MinIO：http://localhost:9001
echo   - Kibana：http://localhost:5601
echo   - ES：http://localhost:9200
echo.

pause