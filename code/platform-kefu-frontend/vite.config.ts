/// <reference types="vitest" />
import { defineConfig } from 'vite'
import type { UserConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 注意: 这里 test 字段是 vitest 扩展的配置。项目用 vite 8 (rolldown), 而
// vitest 3.2.7 自带嵌套 vite 7 (rollup), `/// <reference types="vitest" />`
// 引入的模块增强与本地 vite 8 的 UserConfig 类型不匹配, 直接内联 object literal
// 会被 vue-tsc -b 报 TS2769。因此把配置先赋给带 test 字段的类型化变量,
// 绕过 object literal 的多余属性检查 (excess property check), 运行时 vitest 正常读取。
const config: UserConfig & { test?: unknown } = {
  base: './',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api/kefu': {
        target: 'http://localhost:8000',
        changeOrigin: true
      }
    }
  },
  test: {
    globals: true,
    environment: 'happy-dom',
    include: ['src/**/*.{test,spec}.{ts,js}']
  }
}

export default defineConfig(config)
