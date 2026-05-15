@echo off
chcp 65001 >nul
title 云枢中台 PC 管理后台

echo ========================================
echo 云枢中台 PC 管理后台
echo ========================================
echo.

cd /d "%~dp0"

echo 正在安装依赖...
call npm install

echo.
echo 正在启动开发服务器...
echo.
echo 访问地址：http://localhost:5173
echo.

npm run dev
