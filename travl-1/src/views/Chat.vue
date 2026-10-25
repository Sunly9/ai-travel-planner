<template>
    <div class="page-container chat-page">
      <div class="chat-header" style="height: 46px;">
        <van-nav-bar title="AI 对话" 
        left-arrow
        left-text="返回"
        fixed
        @click-left="onBack"
        />
      </div>
      <div class="chat-container" ref="chatContainer">
        <div v-if="messages.length === 0" class="chat-empty">
          <EmptyState
            icon="💬"
            title="开始和旅游助手对话"
            description="不知道该问什么？点下面任意一条试试"
          />
          <div class="quick-questions">
            <div class="quick-title">
              常见问题
            </div>
            <van-tag
              class="quick-tag"
              v-for="question in quickQuestions"
              :key="question"
              mark
              size="large"
              @click="sendQuick(question)"
            >{{question}}</van-tag>
          </div>
        </div>
        <!-- 有对话内容 -->
         <div class="message-list" v-else>
          <!-- 按日期分组，避免历史消息混成一坨 -->
          <template v-for="item in messageGroups" :key="item.key">
            <div v-if="item.type === 'divider'" class="date-divider">{{ item.label }}</div>
            <ChatBubble v-else :message="item.msg" />
          </template>
          <div v-if="isStreaming" class="streaming-indicator">
            <van-loading type="spinner" size="20px" />
            <span>AI 正在思考中…</span>
          </div>

        </div>
      </div>
        <div class="chat-input-area">
          <van-field
              v-model="inputMessage"
              :disabled="isStreaming"
              placeholder="请输入您的问题..."
              @keydown.enter="onEnter"
            >
              <template #button>
                <van-button :disabled="!inputMessage.trim()" @click="sendMessage" type="primary">发送</van-button>
              </template>
            </van-field>
        </div>
       
    </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { fetchStream } from '../utils/request'
import request from '../utils/request'
import { showToast, showConfirmDialog } from 'vant'
import ChatBubble from '../components/ChatBubble.vue'
import EmptyState from '../components/EmptyState.vue'

const chatContainer = ref(null)

const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const router = useRouter()
const onBack = () => { router.back() }

const inputMessage = ref('')
const isStreaming = ref(false)
const messages = ref([])

// 消息按日期分组，插入「今天 / 昨天 / M月D日」分隔条
const dateLabel = (ts) => {
  const d = ts ? new Date(ts) : new Date()
  if (Number.isNaN(d.getTime())) return '更早'
  const startOf = (x) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const diffDays = Math.round((startOf(new Date()) - startOf(d)) / 86400000)
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const messageGroups = computed(() => {
  const out = []
  let last = ''
  for (const msg of messages.value) {
    const label = dateLabel(msg.timestamp)
    if (label !== last) {
      out.push({ type: 'divider', label, key: `divider-${label}-${out.length}` })
      last = label
    }
    out.push({ type: 'msg', msg, key: `msg-${msg.id}` })
  }
  return out
})

const quickQuestions = ref([
  '北京有哪些必去的景点？',
  '上海美食推荐',
  '成都三日游攻略',
  '如何选择旅行保险？'
])

const saveMsg = (role, content) => {
  request.post('/api/chat-messages', { role, content }).catch(() => {})
}

const loadHistory = async () => {
  try {
    const res = await request.get('/api/chat-messages')
    if (res.success && res.data) {
      messages.value = res.data.map((m) => ({
        id: m.id,
        role: m.role,
        content: m.content,
        timestamp: m.createTime,
      }))
      scrollToBottom()
    }
  } catch {
    // 首次可能没记录，忽略
  }
}

onMounted(() => {
  loadHistory()
})

const sendMessage = () => {
  const msg = inputMessage.value.trim()
  if (!msg || isStreaming.value) return
  addUserMessage(msg)
  saveMsg('user', msg)
  inputMessage.value = ''
  fetchAIRespone(msg)
}

const onEnter = (e) => {
  e.preventDefault()
  sendMessage()
}

// 点击「常见问题」标签：直接当作一条消息发出去
const sendQuick = (question) => {
  if (isStreaming.value) return
  inputMessage.value = question
  sendMessage()
}

const addUserMessage = (msg) => {
  messages.value.push({
    id: Date.now() + 1,
    role: 'user',
    content: msg,
    timestamp: new Date().toISOString(),
  })
}

const fetchAIRespone = (userMsg) => {
  isStreaming.value = true
  messages.value.push({
    id: Date.now() + 2,
    role: 'ai',
    content: '',
    timestamp: new Date().toISOString(),
  })

  let fullResponse = ''
  fetchStream('chat', {
    message: userMsg
  }, (chunk) => {
    fullResponse += chunk
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.role === 'ai') {
      lastMsg.content = fullResponse
    }
    scrollToBottom()
  }, () => {
    isStreaming.value = false
    saveMsg('assistant', fullResponse)
    scrollToBottom()
  }, (errMsg) => {
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.role === 'ai') {
      lastMsg.content = `抱歉，AI回复错误：${errMsg}`
    }
    isStreaming.value = false
    showToast('AI回复错误，请稍后重试')
    scrollToBottom()
  })
}

const clearHistory = async () => {
  if (messages.value.length === 0) {
    showToast('暂无聊天记录')
    return
  }
  try {
    await showConfirmDialog({ title: '清空聊天', message: '确定要清空所有聊天记录吗？' })
    const res = await request.delete('/api/chat-messages')
    if (res.success) {
      messages.value = []
      showToast('已清空')
    }
  } catch (e) {
    // 用户取消
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding-bottom: 50px;
}

.chat-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 60px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.quick-questions {
  margin-top: 32px;
  text-align: center;
}

.quick-title {
  font-size: 14px;
  color: #999;
  margin-bottom: 16px;
}

.quick-tag {
  margin: 8px;
  cursor: pointer;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.date-divider {
  align-self: center;
  font-size: 12px;
  color: #969799;
  background: #f2f3f5;
  padding: 2px 10px;
  border-radius: 10px;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  color: #999;
  font-size: 14px;
}

.chat-input-area {
  position: fixed;
  bottom: 50px;
  left: 0;
  right: 0;
  background: #fff;
  padding: 8px 16px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
  max-width: 750px;
  margin: 0 auto;
}

.chat-input-area :deep(.van-field) {
  background: #f7f8fa;
  border-radius: 20px;
  padding: 8px 16px;
}
</style>


