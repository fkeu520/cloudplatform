@echo off
chcp 65001 >nul
title 云枢中台环境检查工具 v1.0

echo ========================================
echo   云枢中台 Phase 1 环境检查工具
echo ========================================
echo.

set ERROR_COUNT=0
set WARNING_COUNT=0

:: ============ Git ============
echo [1/11] 检查 Git...
git --version >nul 2>&1
if %errorlevel% == 0 (
    for /f "tokens=3" %%i in ('git --version') do set GIT_VER=%%i
    echo   ✓ Git %GIT_VER% 已安装
) else (
    echo   ✗ Git 未安装
    echo     下载地址：https://git-scm.com/download/win
    set /a ERROR_COUNT+=1
)

:: ============ Java ============
echo.
echo [2/11] 检查 Java JDK 17...
java --version >nul 2>&1
if %errorlevel% == 0 (
    java --version 2>&1 | findstr "17"
    if %errorlevel% == 0 (
        echo   ✓ JDK 17 已安装
    ) else (
        echo   ⚠ Java 已安装但版本不是 17
        java --version 2>&1 | findstr "version"
        set /a WARNING_COUNT+=1
    )
) else (
    echo   ✗ Java 未安装
    echo     下载地址：https://adoptium.net/temurin/releases/?version=17
    set /a ERROR_COUNT+=1
)

:: ============ Maven ============
echo.
echo [3/11] 检查 Maven...
mvn --version >nul 2>&1
if %errorlevel% == 0 (
    for /f "tokens=4" %%i in ('mvn --version 2^>nul') do (
        if "%%i"=="Apache" set MAVEN_VER=%%j
    )
    echo   ✓ Maven 已安装
    mvn --version 2>&1 | findstr "Apache"
) else (
    echo   ✗ Maven 未安装
    echo     下载地址：https://maven.apache.org/download.cgi
    set /a ERROR_COUNT+=1
)

:: ============ Node.js ============
echo.
echo [4/11] 检查 Node.js 18+...
node --version >nul 2>&1
if %errorlevel% == 0 (
    for /f "tokens=2" %%i in ('node --version') do set NODE_VER=%%i
    echo   ✓ Node.js %NODE_VER% 已安装
) else (
    echo   ✗ Node.js 未安装
    echo     下载地址：https://nodejs.org/
    set /a ERROR_COUNT+=1
)

:: ============ npm ============
echo.
echo [5/11] 检查 npm...
npm --version >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ npm 已安装
    npm --version
) else (
    echo   ⚠ npm 未正确安装（Node.js 可能未正确安装）
    set /a WARNING_COUNT+=1
)

:: ============ Docker ============
echo.
echo [6/11] 检查 Docker...
docker --version >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Docker 已安装
    docker --version
) else (
    echo   ✗ Docker 未安装
    echo     下载地址：https://www.docker.com/products/docker-desktop/
    set /a ERROR_COUNT+=1
)

:: ============ Docker Service ============
echo.
echo [7/11] 检查 Docker 服务...
docker ps >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Docker 服务正在运行
) else (
    echo   ⚠ Docker 已安装但服务未运行
    echo     请打开 Docker Desktop 应用程序
    set /a WARNING_COUNT+=1
)

:: ============ MySQL ============
echo.
echo [8/11] 检查 MySQL 端口 3306...
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ MySQL 正在监听端口 3306
) else (
    echo   ⚠ MySQL 端口 3306 未监听（如果使用 Docker，可能还未启动）
    set /a WARNING_COUNT+=1
)

:: ============ Redis ============
echo.
echo [9/11] 检查 Redis 端口 6379...
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Redis 正在监听端口 6379
) else (
    echo   ⚠ Redis 端口 6379 未监听
    set /a WARNING_COUNT+=1
)

:: ============ Nacos ============
echo.
echo [10/11] 检查 Nacos 端口 8848...
netstat -ano | findstr ":8848" | findstr "LISTENING" >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Nacos 正在监听端口 8848
    echo     控制台地址：http://localhost:8848/nacos
) else (
    echo   ⚠ Nacos 端口 8848 未监听
    set /a WARNING_COUNT+=1
)

:: ============ Kafka ============
echo.
echo [11/11] 检查 Kafka 端口 9092...
netstat -ano | findstr ":9092" | findstr "LISTENING" >nul 2>&1
if %errorlevel% == 0 (
    echo   ✓ Kafka 正在监听端口 9092
) else (
    echo   ⚠ Kafka 端口 9092 未监听
    set /a WARNING_COUNT+=1
)

:: ============ 总结 ============
echo.
echo ========================================
echo   检查完成
echo ========================================
echo.

if %ERROR_COUNT% gtr 0 (
    echo   ✗ 发现 %ERROR_COUNT% 个错误，需要先安装相关软件
) else (
    echo   ✓ 无阻塞性错误
)

if %WARNING_COUNT% gtr 0 (
    echo   ⚠ 发现 %WARNING_COUNT% 个警告（不影响开发，可稍后处理）
) else (
    echo   ✓ 无警告
)

echo.
echo ========================================
echo   快速链接
echo ========================================
echo.
echo   Nacos 控制台：http://localhost:8848/nacos
echo   MinIO 控制台：http://localhost:9001
echo   Kibana：http://localhost:5601
echo   ElasticSearch：http://localhost:9200
echo.
echo   后端 API：http://localhost:8080
echo   Swagger 文档：http://localhost:8080/swagger-ui.html
echo   前端后台：http://localhost:5173
echo.

echo ========================================
echo   下一步操作
echo ========================================
echo.
echo   1. 启动所有中间件：双击 platform\docker-compose-up.bat
echo   2. 编译后端代码：双击 platform\code\platform-server\compile.bat
echo   3. 启动前端：双击 platform\code\platform-admin\start.bat
echo.

pause