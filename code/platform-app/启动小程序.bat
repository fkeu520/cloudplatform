@echo off
chcp 65001 >nul
title 云枢中台移动应用

echo ========================================
echo 云枢中台移动应用
echo ========================================
echo.

cd /d "%~dp0"

echo 正在安装依赖...
call npm install

echo.
echo 正在启动小程序开发服务器...
echo.
echo 请使用 HBuilderX 或微信开发者工具运行
echo.

npm run dev:mp-weixin
