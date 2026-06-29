<template>
  <div class="chat-page">
    <header>
      <h1>智能问答</h1>
      <div class="user">
        <nav>
          <router-link to="/">智能问答</router-link>
          <router-link to="/knowledge">知识库</router-link>
          <router-link to="/dashboard">数据看板</router-link>
        </nav>
        <span>智能问答</span>
      </div>
    </header>
    <div class="chat-container">
      <div class="messages" ref="messagesRef">
        <div v-for="msg in messages" :key="msg.id" :class="['msg', msg.role]">
          <div class="bubble" v-html="renderMarkdown(msg.content)"></div>
          <div v-if="msg.chunks?.length" class="sources">
            <span v-for="(_chunk, i) in msg.chunks" :key="i" class="source-tag">引用 {{ i + 1 }}</span>
          </div>
        </div>
        <div v-if="streaming" class="msg assistant">
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
          :disabled="loading"
        />
        <button @click="sendQuestion" :disabled="loading || !question.trim()">
          {{ loading ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { chatApi } from '@/api'
import { marked } from 'marked'

const question = ref('')
const loading = ref(false)
const streaming = ref(false)
const streamContent = ref('')
const messages = ref<{ id: string; role: string; content: string; chunks?: string[] }[]>([
  { id: 'welcome', role: 'assistant', content: '你好！我是智能客服助手，请问有什么问题？' }
])
const messagesRef = ref<HTMLDivElement>()

const renderMarkdown = (text: string) => marked.parse(text, { async: false })

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const sendQuestion = async () => {
  const q = question.value.trim()
  if (!q || loading.value) return

  const userMsg = { id: Date.now().toString(), role: 'user', content: q }
  messages.value.push(userMsg)
  question.value = ''
  loading.value = true
  streaming.value = true
  streamContent.value = ''
  await scrollToBottom()

  try {
    const response = await chatApi.askStream(q)
    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let fullAnswer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const text = decoder.decode(value)
      const lines = text.split('\n\n')
      for (const line of lines) {
        if (line.startsWith('data: ')) {
          try {
            const data = JSON.parse(line.slice(6))
            if (data.content) {
              fullAnswer += data.content
              streamContent.value = fullAnswer
              await scrollToBottom()
            }
          } catch {}
        }
      }
    }

    messages.value.push({
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: fullAnswer || '暂无答案',
      chunks: []
    })
  } catch (e: any) {
    messages.value.push({
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: '请求失败：' + (e.message || '未知错误')
    })
  } finally {
    streaming.value = false
    loading.value = false
    streamContent.value = ''
    await scrollToBottom()
  }
}
</script>

<style scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f2f2f7;
}
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 32px;
  background: #ffffff;
  border-bottom: 1px solid #d2d2d7;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
header h1 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
}
.user {
  display: flex;
  align-items: center;
  gap: 12px;
}
nav {
  display: flex;
  gap: 4px;
  margin-right: 12px;
  background: #f2f2f7;
  padding: 3px;
  border-radius: 10px;
}
nav a {
  color: #6e6e73;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  padding: 5px 14px;
  border-radius: 7px;
  transition: background 0.2s, color 0.2s;
}
nav a:hover {
  color: #1d1d1f;
}
nav a.router-link-active {
  background: #ffffff;
  color: #007AFF;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.user span {
  font-size: 13px;
  color: #6e6e73;
}
.user button {
  padding: 6px 16px;
  background: #f2f2f7;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  transition: background 0.2s;
}
.user button:hover { background: #e5e5ea; }
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
}
.msg {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}
.msg.user { align-items: flex-end; }
.msg.assistant { align-items: flex-start; }
.bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.6;
  word-break: break-word;
  font-size: 15px;
}
.msg.user .bubble {
  background: #007AFF;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}
.msg.assistant .bubble {
  background: #ffffff;
  color: #1d1d1f;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.bubble.typing {
  padding: 14px 18px;
  display: flex;
  gap: 5px;
  align-items: center;
}
.bubble.typing span {
  width: 8px;
  height: 8px;
  background: #c7c7cc;
  border-radius: 50%;
  animation: typing 1.2s ease-in-out infinite;
}
.bubble.typing span:nth-child(2) { animation-delay: 0.2s; }
.bubble.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}
.sources {
  margin-top: 6px;
  display: flex;
  gap: 6px;
}
.source-tag {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3px;
  padding: 3px 10px;
  background: #e5f1ff;
  color: #007AFF;
  border-radius: 9999px;
}
.input-area {
  display: flex;
  gap: 10px;
  padding: 14px 0;
  background: transparent;
}
.input-area input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #d1d1d6;
  border-radius: 12px;
  font-size: 15px;
  color: #1d1d1f;
  background: #ffffff;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input-area input:focus {
  border-color: #007AFF;
  box-shadow: 0 0 0 3px rgba(0,122,255,0.15);
}
.input-area input::placeholder { color: #86868b; }
.input-area button {
  padding: 12px 22px;
  background: #007AFF;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: background 0.2s;
}
.input-area button:hover { background: #0056b3; }
.input-area button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
