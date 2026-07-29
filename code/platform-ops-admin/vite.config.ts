import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  // 2026-07-29: 适配 cloud.hugh.sryze.cc/ops/ 子路径访问
  // 当 nginx/tunnel 用 location /ops/ 路由时,HTML 静态资源必须带 /ops/ 前缀
  base: '/ops/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5174,
    allowedHosts: ['.monkeycode-ai.online'], // O6: 允许反向代理 host
    proxy: {
      // 统一走 API 网关（需先启动 Docker 中的 platform-gateway）
      '/api': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
