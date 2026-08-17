<template>
  <div class="chat-page">
    <header>
      <h1>智能问答</h1>
      <div class="user">
        <nav>
          <router-link to="/">智能问答</router-link>
          <router-link to="/knowledge">知识库</router-link>
          <router-link to="/dashboard">数据看板</router-link>
          <router-link to="/sessions">会话管理</router-link>
          <router-link to="/faqs">FAQ管理</router-link>
          <router-link to="/import">知识导入</router-link>
          <router-link to="/settings">数据源</router-link>
          <router-link to="/evaluation">评估</router-link>
        </nav>
        <span :class="['session-badge', sessionStatus]">{{ statusText }}</span>
      </div>
    </header>
    <div class="chat-container">
      <div class="messages" ref="messagesRef">
        <div v-for="msg in messages" :key="msg.msg_id || msg.id" :class="['msg', msg.role === 'customer' ? 'user' : 'assistant']">
          <div class="bubble" v-html="renderMarkdown(msg.content)"></div>
          <div v-if="msg.data_source_refs?.length" class="sources">
            <span v-for="(ref, i) in msg.data_source_refs" :key="i" class="source-tag" :title="ref.label">
              {{ ref.source }}: {{ ref.entity_type }}#{{ (ref.entity_id || '').substring(0, 8) }}
            </span>
          </div>
        </div>
        <div v-if="loading" class="msg assistant">
          <div class="bubble typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
      <div class="input-area">
        <input
          v-model="question"
          placeholder="输入问题..."
          @keyup.enter="sendQuestion"
          :disabled="loading || sessionStatus === 'CLOSED'"
        />
        <button @click="sendQuestion" :disabled="loading || !question.trim() || sessionStatus === 'CLOSED'">
          {{ loading ? '发送中...' : '发送' }}
        </button>
        <button v-if="sessionStatus === 'AI'" class="btn-secondary" @click="transferToHuman">转人工</button>
        <button v-if="sessionStatus !== 'CLOSED'" class="btn-secondary" @click="closeSession">结束会话</button>
      </div>
    </div>
    <div v-if="showRateModal" class="modal-overlay" @click="showRateModal = false">
      <div class="modal-card" @click.stop>
        <h3>会话评价</h3>
        <div class="stars">
          <span v-for="n in 5" :key="n" :class="['star', { active: n <= rateScore }]" @click="rateScore = n">★</span>
        </div>
        <textarea v-model="rateComment" placeholder="请输入评价（可选）" rows="3"></textarea>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showRateModal = false">取消</button>
          <button @click="submitRating" :disabled="rateScore === 0">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { sessionApi } from '@/api'
import { marked } from 'marked'

const question = ref('')
const loading = ref(false)
const messages = ref<any[]>([
  { id: 'welcome', role: 'assistant', content: '你好！我是云枢园区智能客服助手，请问有什么可以帮您？' }
])
const messagesRef = ref<HTMLDivElement>()
const sessionId = ref('')
const sessionStatus = ref('AI')
const showRateModal = ref(false)
const rateScore = ref(0)
const rateComment = ref('')

const statusText = computed(() => {
  const map: Record<string, string> = { AI: 'AI 接待中', HUMAN: '人工接待中', CLOSED: '已结束' }
  return map[sessionStatus.value] || sessionStatus.value
})

const renderMarkdown = (text: string) => marked.parse(text, { async: false })

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const initSession = async () => {
  try {
    const res = await sessionApi.create({ channel: 'web' })
    sessionId.value = res.data.id
    sessionStatus.value = res.data.status
  } catch (e) {
    console.error('创建会话失败', e)
  }
}

const sendQuestion = async () => {
  const q = question.value.trim()
  if (!q || loading.value || !sessionId.value) return
  messages.value.push({ msg_id: 'u_' + Date.now(), role: 'customer', content: q })
  question.value = ''
  loading.value = true
  await scrollToBottom()
  try {
    const res = await sessionApi.sendMessage(sessionId.value, q)
    const data = res.data
    messages.value.push(data.ai_message)
    sessionStatus.value = data.session_status || sessionStatus.value
  } catch (e: any) {
    messages.value.push({
      msg_id: 'err_' + Date.now(),
      role: 'assistant',
      content: '抱歉，请求失败：' + (e.response?.data?.detail || e.message || '未知错误')
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const transferToHuman = async () => {
  if (!sessionId.value) return
  try {
    const res = await sessionApi.transfer(sessionId.value)
    sessionStatus.value = res.data.status
  } catch (e) { console.error('转人工失败', e) }
}

const closeSession = async () => {
  if (!sessionId.value) return
  try {
    await sessionApi.close(sessionId.value)
    sessionStatus.value = 'CLOSED'
    showRateModal.value = true
  } catch (e) { console.error('关闭会话失败', e) }
}

const submitRating = async () => {
  if (!sessionId.value || rateScore.value === 0) return
  try {
    await sessionApi.rate(sessionId.value, rateScore.value, rateComment.value)
    showRateModal.value = false
    rateScore.value = 0
    rateComment.value = ''
  } catch (e) { console.error('提交评价失败', e) }
}

onMounted(initSession)
</script>

<style scoped>
.chat-page { height: 100vh; display: flex; flex-direction: column; background: #f2f2f7; }
header { display: flex; justify-content: space-between; align-items: center; padding: 14px 32px; background: #ffffff; border-bottom: 1px solid #d2d2d7; box-shadow: 0 1px 2px rgba(0,0,0,0.04); }
header h1 { font-size: 17px; font-weight: 600; color: #1d1d1f; }
.user { display: flex; align-items: center; gap: 12px; }
nav { display: flex; gap: 2px; margin-right: 12px; background: #f2f2f7; padding: 3px; border-radius: 10px; }
nav a { color: #6e6e73; text-decoration: none; font-size: 13px; font-weight: 500; padding: 5px 12px; border-radius: 7px; transition: background 0.2s, color 0.2s; }
nav a:hover { color: #1d1d1f; }
nav a.router-link-active { background: #ffffff; color: #007AFF; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.session-badge { font-size: 12px; font-weight: 600; padding: 4px 12px; border-radius: 9999px; }
.session-badge.AI { background: #d1f7d1; color: #248a3d; }
.session-badge.HUMAN { background: #fff3e0; color: #cc7a00; }
.session-badge.CLOSED { background: #f2f2f7; color: #86868b; }
.chat-container { flex: 1; display: flex; flex-direction: column; max-width: 800px; width: 100%; margin: 0 auto; padding: 20px; }
.messages { flex: 1; overflow-y: auto; padding: 8px 4px; }
.msg { margin-bottom: 16px; display: flex; flex-direction: column; }
.msg.user { align-items: flex-end; }
.msg.assistant { align-items: flex-start; }
.bubble { max-width: 75%; padding: 12px 16px; border-radius: 18px; line-height: 1.6; word-break: break-word; font-size: 15px; }
.msg.user .bubble { background: #007AFF; color: #ffffff; border-bottom-right-radius: 4px; }
.msg.assistant .bubble { background: #ffffff; color: #1d1d1f; border-bottom-left-radius: 4px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.bubble.typing { padding: 14px 18px; display: flex; gap: 5px; align-items: center; }
.bubble.typing span { width: 8px; height: 8px; background: #c7c7cc; border-radius: 50%; animation: typing 1.2s ease-in-out infinite; }
.bubble.typing span:nth-child(2) { animation-delay: 0.2s; }
.bubble.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing { 0%, 60%, 100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-6px); opacity: 1; } }
.sources { margin-top: 6px; display: flex; gap: 6px; flex-wrap: wrap; }
.source-tag { font-size: 11px; font-weight: 500; padding: 3px 10px; background: #e5f1ff; color: #007AFF; border-radius: 9999px; }
.input-area { display: flex; gap: 10px; padding: 14px 0; }
.input-area input { flex: 1; padding: 12px 16px; border: 1px solid #d1d1d6; border-radius: 12px; font-size: 15px; color: #1d1d1f; background: #ffffff; outline: none; transition: border-color 0.2s, box-shadow 0.2s; }
.input-area input:focus { border-color: #007AFF; box-shadow: 0 0 0 3px rgba(0,122,255,0.15); }
.input-area input::placeholder { color: #86868b; }
.input-area button { padding: 12px 22px; background: #007AFF; color: #ffffff; border: none; border-radius: 12px; cursor: pointer; font-size: 15px; font-weight: 600; transition: background 0.2s; }
.input-area button:hover { background: #0056b3; }
.input-area button:disabled { opacity: 0.5; cursor: not-allowed; }
.input-area .btn-secondary { background: #f2f2f7; color: #1d1d1f; border: 1px solid #d2d2d7; }
.input-area .btn-secondary:hover { background: #e5e5ea; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; backdrop-filter: blur(4px); }
.modal-card { background: #ffffff; border-radius: 18px; padding: 28px; max-width: 420px; width: 90%; box-shadow: 0 12px 40px rgba(0,0,0,0.2); }
.modal-card h3 { font-size: 18px; font-weight: 600; color: #1d1d1f; margin: 0 0 18px; }
.stars { display: flex; gap: 8px; margin-bottom: 18px; }
.stars .star { font-size: 32px; color: #d1d1d6; cursor: pointer; transition: color 0.15s; }
.stars .star.active { color: #ff9500; }
.stars .star:hover { color: #ffb340; }
.modal-card textarea { width: 100%; padding: 12px; border: 1px solid #d1d1d6; border-radius: 10px; font-size: 14px; resize: none; outline: none; font-family: inherit; }
.modal-card textarea:focus { border-color: #007AFF; }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 18px; }
.modal-actions button { padding: 10px 20px; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; }
.modal-actions button:not(.btn-secondary) { background: #007AFF; color: #ffffff; }
.modal-actions .btn-secondary { background: #f2f2f7; color: #1d1d1f; border: 1px solid #d2d2d7; }
.modal-actions button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>