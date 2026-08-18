<template>
  <div class="kefu-iframe-wrapper">
    <!--
      kefu (智能问答) 嵌入式入口
      - admin nginx 已配 /kefu/ → platform-kefu-frontend:80 (同域代理, 避免跨域)
      - kefu-frontend 是 hash router, 切换 chat/knowledge/dashboard 用 hash 即可
    -->
    <iframe
      ref="iframeRef"
      :src="iframeSrc"
      class="kefu-iframe"
      frameborder="0"
      allow="clipboard-write"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * Kefu 智能问答 - 通用 iframe 包装
 * 后端: platform-kefu (8050/8000) - FastAPI, gateway 路由 /api/kefu/**
 * 前端: platform-kefu-frontend (8060/80) - Vue 3 SPA (hash router)
 * 部署:
 *   - platform-admin 容器 nginx 已不再代理 /kefu/, 让 vue-router 处理 /kefu/chat
 *   - iframe src 改为 /kefu-frontend/#/chat, 云端 nginx (cloudhub) 代理
 *     /kefu-frontend/ → kefu-frontend:8060 (避免路径冲突)
 *   - 这样 kefu SPA 在 iframe 内运行, 外层是 platform-admin 框架
 *     (顶部 tab / 左侧菜单 / 工作台框架完整保留)
 *
 * 菜单: sys_menu id 510 (智能问答) / 511 (对话) / 512 (知识库) / 513 (仪表盘)
 *       517 (会话管理) / 518 (FAQ管理) / 519 (知识导入) / 520 (数据源) / 521 (评估)
 * 父级 app_id=7 (knowledge)
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const iframeRef = ref<HTMLIFrameElement | null>(null)

/**
 * 映射 admin 路由 -> kefu-frontend hash 路径
 *  /kefu           -> /kefu-frontend/#/chat
 *  /kefu/chat      -> /kefu-frontend/#/chat
 *  /kefu/knowledge -> /kefu-frontend/#/knowledge
 *  /kefu/dashboard -> /kefu-frontend/#/dashboard
 */
const iframeSrc = computed(() => {
  const sub = route.path.replace(/^\/kefu/, '') || '/chat'
  const hash = sub === '/' || sub === '' ? '/chat' : sub
  return `/kefu-frontend/#${hash}`
})

/** 路由变化时, 主动更新 iframe hash (无重载, 避免闪烁) */
watch(() => route.path, () => {
  if (!iframeRef.value?.contentWindow) return
  const sub = route.path.replace(/^\/kefu/, '') || '/chat'
  const target = sub === '/' || sub === '' ? '/chat' : sub
  try {
    iframeRef.value.contentWindow.location.hash = target
  } catch {
    /* 跨域等极端情况, 忽略 — src 兜底 */
  }
})

onMounted(() => {
  // iframe load 完成后, 同步当前 hash 避免初始偏差
  setTimeout(() => {
    if (!iframeRef.value?.contentWindow) return
    const sub = route.path.replace(/^\/kefu/, '') || '/chat'
    const target = sub === '/' || sub === '' ? '/chat' : sub
    try {
      iframeRef.value.contentWindow.location.hash = target
    } catch { /* ignore */ }
  }, 200)
})
</script>

<style scoped>
.kefu-iframe-wrapper {
  width: 100%;
  height: calc(100vh - 110px); /* 减去 Layout 头部 + padding */
  min-height: 600px;
  background: #fff;
}
.kefu-iframe {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}
</style>
