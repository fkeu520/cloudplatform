@echo off
chcp 65001 >nul
title 云枢中台后端编译

echo ========================================
echo   云枢中台后端项目编译
echo ========================================
echo.

cd /d "%~dp0"

:: 检查 Java
java --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ Java 未安装，请先安装 JDK 17+
    pause
    exit /b 1
)

:: 检查 Maven
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ Maven 未安装，请先安装 Maven 3.9+
    pause
    exit /b 1
)

echo.
echo [1/3] 检查 pom.xml...
if not exist "pom.xml" (
    echo ✗ pom.xml 不存在，请确保在 platform-server 目录运行
    pause
    exit /b 1
)
echo   ✓ pom.xml 存在

echo.
echo [2/3] 清理并编译项目...
echo   编译过程约需 3-5 分钟（首次需下载依赖）
echo.

mvn clean compile -DskipTests

if %errorlevel% neq 0 (
    echo.
    echo ✗ 编译失败，请检查错误信息
    echo.
    echo   常见问题：
    echo   1. 网络问题导致依赖下载失败 - 尝试多次执行
    echo   2. Java 版本不对 - 确保 JDK 17+
    echo   3. 内存不足 - 关闭其他程序后重试
    echo.
    pause
    exit /b 1
)

echo.
echo [3/3] 编译完成！
echo.

:: 检查 target 目录
if exist "platform-user\target\platform-user-1.0.0.jar" (
    echo   ✓ platform-user 构建成功
) else (
    echo   ⚠ platform-user JAR 未生成，可能编译选项不同
)

echo.
echo ========================================
echo   编译完成
echo ========================================
echo.
echo   下一步：
echo   1. 启动服务：mvn spring-boot:run
echo   2. 或打包后运行：java -jar platform-user\target\platform-user-1.0.0.jar
echo.
echo   API 文档地址：http://localhost:8080/swagger-ui.html
echo.

pause