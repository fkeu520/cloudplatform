@echo off
chcp 65001 >nul
title 云枢中台前端启动

echo ========================================
echo   云枢中台前端启动工具
echo ========================================
echo.

cd /d "%~dp0"

:: 检查 Node.js
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ Node.js 未安装，请先安装 Node.js 18+
    echo   下载地址：https://nodejs.org/
    pause
    exit /b 1
)

:: 检查 npm
npm --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ npm 未正确安装
    pause
    exit /b 1
)

echo.
echo [1/4] 检查 node_modules...
if not exist "node_modules" (
    echo   node_modules 不存在，开始安装依赖...
    echo   安装过程约需 5-10 分钟
    npm install
    if %errorlevel% neq 0 (
        echo ✗ 依赖安装失败
        pause
        exit /b 1
    )
) else (
    echo   ✓ node_modules 存在
)

echo.
echo [2/4] 检查 package.json...
if not exist "package.json" (
    echo ✗ package.json 不存在，请确保在 platform-admin 目录运行
    pause
    exit /b 1
)
echo   ✓ package.json 存在

echo.
echo [3/4] 检查 vite.config.ts...
if not exist "vite.config.ts" (
    echo   vite.config.ts 不存在，创建默认配置...
    (
echo import { defineConfig } from 'vite'
echo import vue from '@vitejs/plugin-vue'
echo.
echo export default defineConfig(^
    plugins: [vue()],
    server: ^{
        port: 5173,
        proxy: ^{
            '/api': ^{
                target: 'http://localhost:8080',
                changeOrigin: true
            ^}
        ^}
    ^}
^)
    ) > vite.config.ts
)
echo   ✓ vite.config.ts 存在

echo.
echo [4/4] 启动开发服务器...
echo   启动中，请稍候...
echo.

npm run dev

echo.
echo ========================================
echo   前端服务已停止
echo ========================================
echo.
echo   如需重启，请重新运行此脚本
echo.

pause