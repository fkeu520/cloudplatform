@echo off
chcp 65001 >nul
title 云枢中台快速启动工具

:menu
cls
echo ========================================
echo    云枢中台快速启动工具
echo ========================================
echo.
echo   1. 检查环境
echo   2. 启动后端服务
echo   3. 启动 PC 管理后台
echo   4. 启动移动应用
echo   5. 全部启动
echo   6. 停止全部服务
echo   0. 退出
echo.
echo ========================================
set /p choice=请选择 (0-6): 

if "%choice%"=="1" goto check_env
if "%choice%"=="2" goto start_backend
if "%choice%"=="3" goto start_admin
if "%choice%"=="4" goto start_app
if "%choice%"=="5" goto start_all
if "%choice%"=="6" goto stop_all
if "%choice%"=="0" goto end

echo 无效选择，请重新输入
pause
goto menu

:check_env
cls
echo ========================================
echo 检查环境...
echo ========================================
echo.

:: 检查 Java
echo [1/8] 检查 Java...
java -version >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Java 已安装
) else (
    echo ✗ Java 未安装，请安装 JDK 17+
    pause
    goto menu
)

:: 检查 Node.js
echo [2/8] 检查 Node.js...
node -v >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Node.js 已安装
) else (
    echo ✗ Node.js 未安装，请安装 Node.js 18+
    pause
    goto menu
)

:: 检查 MySQL
echo [3/8] 检查 MySQL...
mysql --version >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ MySQL 已安装
) else (
    echo ✗ MySQL 未安装，请安装 MySQL 8.0+
    pause
    goto menu
)

:: 检查 Redis
echo [4/8] 检查 Redis...
redis-cli --version >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Redis 已安装
) else (
    echo ✗ Redis 未安装，请安装 Redis 7.0+
    pause
    goto menu
)

:: 检查 Docker
echo [5/8] 检查 Docker...
docker --version >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Docker 已安装
) else (
    echo ✗ Docker 未安装，请安装 Docker 24.0+
    pause
    goto menu
)

:: 检查 Git
echo [6/8] 检查 Git...
git --version >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Git 已安装
) else (
    echo ✗ Git 未安装，请安装 Git 2.40+
    pause
    goto menu
)

:: 检查 Maven
echo [7/8] 检查 Maven...
mvn --version >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Maven 已安装
) else (
    echo ✗ Maven 未安装，请安装 Maven 3.9+
    pause
    goto menu
)

:: 检查 kubectl (可选)
echo [8/8] 检查 Kubernetes...
kubectl version --client >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Kubernetes 已安装
) else (
    echo ~ Kubernetes 未安装（可选）
)

echo.
echo ========================================
echo 环境检查完成！
echo ========================================
pause
goto menu

:start_backend
cls
echo ========================================
echo 启动后端服务...
echo ========================================
echo.

cd /d "%~dp0code\platform-server"
if not exist "platform-admin" (
    echo 错误：后端代码目录不存在
    pause
    goto menu
)

echo 正在编译后端...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo 编译失败
    pause
    goto menu
)

echo 正在启动后端服务...
start "云枢中台后端" cmd /k "cd platform-admin && java -jar target/platform-admin-1.0.0.jar"

echo.
echo 后端服务启动中...
echo 访问地址：http://localhost:8080
echo Swagger: http://localhost:8080/swagger-ui.html
echo.
pause
goto menu

:start_admin
cls
echo ========================================
echo 启动 PC 管理后台...
echo ========================================
echo.

cd /d "%~dp0code\platform-admin"
if not exist "package.json" (
    echo 错误：PC 后台代码目录不存在
    pause
    goto menu
)

echo 正在安装依赖...
call npm install
if %errorlevel% neq 0 (
    echo 依赖安装失败
    pause
    goto menu
)

echo 正在启动 PC 管理后台...
start "云枢中台 PC 管理后台" cmd /k "npm run dev"

echo.
echo PC 管理后台启动中...
echo 访问地址：http://localhost:5173
echo.
pause
goto menu

:start_app
cls
echo ========================================
echo 启动移动应用...
echo ========================================
echo.

cd /d "%~dp0code\platform-app"
if not exist "package.json" (
    echo 错误：移动应用代码目录不存在
    pause
    goto menu
)

echo 正在安装依赖...
call npm install
if %errorlevel% neq 0 (
    echo 依赖安装失败
    pause
    goto menu
)

echo 正在启动小程序开发服务器...
start "云枢中台小程序" cmd /k "npm run dev:mp-weixin"

echo.
echo 小程序启动中...
echo 请使用 HBuilderX 或微信开发者工具运行
echo.
pause
goto menu

:start_all
cls
echo ========================================
echo 全部启动...
echo ========================================
echo.

:: 启动后端
echo [1/3] 启动后端服务...
cd /d "%~dp0code\platform-server\platform-admin"
start "云枢中台后端" cmd /k "java -jar target/platform-admin-1.0.0.jar"
timeout /t 5 /nobreak >nul

:: 启动 PC 后台
echo [2/3] 启动 PC 管理后台...
cd /d "%~dp0code\platform-admin"
start "云枢中台 PC 管理后台" cmd /k "npm run dev"
timeout /t 5 /nobreak >nul

:: 启动小程序
echo [3/3] 启动移动应用...
cd /d "%~dp0code\platform-app"
start "云枢中台小程序" cmd /k "npm run dev:mp-weixin"

echo.
echo ========================================
echo 全部服务启动完成！
echo ========================================
echo.
echo 访问地址:
echo   PC 管理后台：http://localhost:5173
echo   后端 API: http://localhost:8080
echo   Swagger 文档：http://localhost:8080/swagger-ui.html
echo.
echo 默认账号：admin / admin123
echo.
pause
goto menu

:stop_all
cls
echo ========================================
echo 停止全部服务...
echo ========================================
echo.

:: 停止后端
echo [1/4] 停止后端服务...
taskkill /F /FI "WindowTitle eq 云枢中台后端*" >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ 后端服务已停止
) else (
    echo ~ 后端服务未运行
)

:: 停止 PC 后台
echo [2/4] 停止 PC 管理后台...
taskkill /F /FI "WindowTitle eq 云枢中台 PC 管理后台*" >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ PC 管理后台已停止
) else (
    echo ~ PC 管理后台未运行
)

:: 停止小程序
echo [3/4] 停止移动应用...
taskkill /F /FI "WindowTitle eq 云枢中台小程序*" >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ 移动应用已停止
) else (
    echo ~ 移动应用未运行
)

:: 停止 Docker 容器
echo [4/4] 停止 Docker 容器...
docker stop emqx elasticsearch influxdb kafka 2>nul
if %errorlevel% == 0 (
    echo ✓ Docker 容器已停止
) else (
    echo ~ 无 Docker 容器运行
)

echo.
echo ========================================
echo 全部服务已停止
echo ========================================
pause
goto menu

:end
cls
echo 感谢使用云枢中台快速启动工具
echo.
timeout /t 2 /nobreak >nul
exit
