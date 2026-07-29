<template>
  <div class="ai-chat">
    <div class="chat-header-bar">
      <div class="header-left">
        <button class="btn-menu" @click="sidebarOpen = !sidebarOpen" title="会话列表">☰</button>
        <h1 class="chat-title">AI 对话助手</h1>
        <div class="header-status">
          <span class="status-dot online"></span>
          <span class="status-text">在线</span>
          <span class="status-divider">|</span>
          <span class="status-agent">{{ currentAgentLabel }}</span>
        </div>
      </div>
      <div class="header-right">
        <div class="token-display">
          <span class="token-icon">⚡</span>
          <span class="token-text">{{ estimatedTokens }} / 50k</span>
          <div class="token-bar"><div class="token-fill" :style="{ width: Math.min(estimatedTokens / 50000 * 100, 100) + '%' }"></div></div>
        </div>
        <button class="btn-header btn-context" @click="showContext = !showContext" :class="{ active: showContext }">
          <span class="btn-context-icon">📚</span>
          <span>上下文</span>
        </button>
        <button class="btn-header" @click="createNewSession">新对话</button>
      </div>
    </div>

    <!-- 侧边栏遮罩层 -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <!-- 侧边栏抽屉 -->
    <aside class="chat-sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <span class="sidebar-title">会话历史</span>
        <div class="sidebar-actions">
          <button class="btn-add-session" @click="createNewSession" title="新建会话">+</button>
          <button class="btn-close-sidebar" @click="sidebarOpen = false" title="关闭">×</button>
        </div>
      </div>
      <div class="session-list">
        <div v-for="session in sessions" :key="session.id" class="session-item" :class="{ active: currentSessionId === session.id }" @click="switchSession(session.id); sidebarOpen = false">
          <div class="session-info">
            <span class="session-title">{{ session.title || '新对话' }}</span>
            <span class="session-preview">{{ session.preview || '' }}</span>
          </div>
          <div class="session-meta">
            <span class="session-time">{{ formatSessionTime(session.updateTime || session.createTime) }}</span>
            <button class="btn-delete-session" @click.stop="deleteSession(session.id)" title="删除会话">x</button>
          </div>
        </div>
        <div v-if="loadingSessions" class="session-empty">
          <span class="empty-icon">⏳</span>
          <span>加载中...</span>
        </div>
        <div v-else-if="sessions.length === 0" class="session-empty">
          <span class="empty-icon">💬</span>
          <span>暂无历史对话，开始新对话吧 ✨</span>
        </div>
      </div>
    </aside>

    <div class="chat-body">
      <main class="chat-main">
        <div class="chat-toolbar">
          <div class="agent-tabs">
            <button v-for="agent in agents" :key="agent.key" class="agent-tab" :class="{ active: currentAgent === agent.key }" @click="switchAgent(agent.key)">
              <span class="agent-icon">{{ agent.icon }}</span>
              <span class="agent-label">{{ agent.label }}</span>
              <span class="agent-badge" v-if="agent.badge">{{ agent.badge }}</span>
            </button>
          </div>
          <button class="orchestrator-btn" @click="triggerOrchestrator" :disabled="isOrchestrating" :class="{ loading: isOrchestrating }">
            <span v-if="isOrchestrating" class="orchestrator-spinner"></span>
            <span v-else>🤖</span>
            <span>{{ isOrchestrating ? '分析中...' : '综合分析' }}</span>
          </button>
          <button class="history-btn" @click="toggleAnalysisHistory" :class="{ active: showAnalysisHistory }">
            <span>📋</span>
            <span>历史</span>
          </button>
        </div>

        <div class="messages-container" ref="messagesContainer">
          <div v-if="loadingMessages" class="messages-empty">
            <div class="empty-illustration">⏳</div>
            <h3>加载消息中...</h3>
          </div>
          <div v-else-if="messages.length === 0" class="messages-empty">
            <div class="empty-illustration">🤖</div>
            <h3>开始与 AI 对话</h3>
            <p>选择上方 Agent 角色，提问关于写作的任何问题</p>
            <div class="quick-prompts">
              <button
                v-for="prompt in quickPrompts"
                :key="prompt.key"
                class="quick-prompt"
                :class="{ active: currentAgent === prompt.key }"
                @click="quickPrompt(prompt)"
              >
                {{ prompt.text }}
              </button>
            </div>
          </div>

          <div v-for="(msg, index) in messages" :key="msg.id || index" class="message" :class="msg.role">
            <!-- 系统消息：居中提示 -->
            <div v-if="msg.role === 'system'" class="system-message">
              <span class="system-msg-text">{{ msg.content }}</span>
            </div>
            <!-- 用户/AI 消息 -->
            <template v-else>
            <div class="message-avatar">
              <span v-if="msg.role === 'user'">👤</span>
              <span v-else>{{ getAgentIcon(msg.agent) }}</span>
            </div>
            <div class="message-body">
              <div class="message-meta">
                <span class="message-sender">{{ msg.role === 'user' ? '你' : getAgentLabel(msg.agent) }}</span>
                <span class="message-time">{{ formatMsgTime(msg.createTime) }}</span>
              </div>
              <div class="message-content">{{ msg.content }}</div>
              <div v-if="msg.role === 'ai' && msg.isStreaming" class="typing-indicator"><span></span><span></span><span></span></div>
              <div v-if="msg.role === 'ai' && !msg.isStreaming && msg.content" class="message-actions">
                <button @click="copyMessage(msg.content)" title="复制">📋</button>
              </div>
            </div>
            </template>
          </div>
        </div>

        <!-- 多Agent协作结果展示 -->
        <div v-if="orchestratorResult" class="orchestrator-result">
          <div class="orchestrator-header">
            <h3>🤖 多Agent综合分析报告</h3>
            <button class="orchestrator-close" @click="closeOrchestrator">×</button>
          </div>
          <div class="orchestrator-cost">
            <span>总耗时: {{ orchestratorResult.totalCostMs }}ms</span>
          </div>

          <div class="agent-cards">
            <div class="agent-card editor">
              <div class="agent-card-header">✍️ 编辑视角</div>
              <div class="agent-card-body">{{ orchestratorResult.editorResult?.content || '（无结果）' }}</div>
            </div>
            <div class="agent-card character">
              <div class="agent-card-header">👤 人物视角</div>
              <div class="agent-card-body">{{ orchestratorResult.characterResult?.content || '（无结果）' }}</div>
            </div>
            <div class="agent-card style">
              <div class="agent-card-header">🎨 风格视角</div>
              <div class="agent-card-body">{{ orchestratorResult.styleResult?.content || '（无结果）' }}</div>
            </div>
            <div class="agent-card reader">
              <div class="agent-card-header">📖 读者视角</div>
              <div class="agent-card-body">{{ orchestratorResult.readerResult?.content || '（无结果）' }}</div>
            </div>
          </div>

          <div v-if="orchestratorResult.summary" class="orchestrator-summary">
            <div class="summary-header">📌 综合建议</div>
            <div class="summary-content">{{ orchestratorResult.summary }}</div>
          </div>
        </div>

        <!-- 分析历史记录 -->
        <div v-if="showAnalysisHistory" class="analysis-history-panel">
          <div class="history-header">
            <h3>📋 分析历史</h3>
            <button class="close-btn" @click="showAnalysisHistory = false">×</button>
          </div>
          <div class="history-content">
            <div v-if="analysisHistoryLoading" class="history-loading">加载中...</div>
            <div v-else-if="analysisHistory.length === 0" class="history-empty">
              暂无分析记录
            </div>
            <div v-else class="history-list">
              <div v-for="item in analysisHistory" :key="item.id" class="history-item" @click="viewAnalysis(item)">
                <div class="history-item-info">
                  <span class="history-chapter">{{ item.chapterDisplay || '第' + item.chapterIndex + '章' }}</span>
                  <span class="history-time">{{ formatAnalysisTime(item.createTime) }}</span>
                </div>
                <div class="history-item-summary">{{ item.summary || '（无综合建议）' }}</div>
                <button class="history-delete" @click.stop="deleteAnalysis(item.id)" title="删除">🗑️</button>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="input-wrapper">
            <textarea v-model="inputText" class="input-textarea" placeholder="输入消息，与 AI 讨论写作..." rows="2" @keydown.ctrl.enter.prevent="sendMessage" @keydown.meta.enter.prevent="sendMessage" ref="inputTextarea"></textarea>
            <div class="input-toolbar">
              <span class="input-char-count">{{ inputText.length }} / 2000</span>
              <div class="input-actions">
                <button v-if="isStreaming" class="btn-stop" @click="stopGeneration" title="停止生成">
                  <span class="stop-icon">⏹</span> 停止
                </button>
                <button class="btn-send" :class="{ active: inputText.trim() }" @click="sendMessage" :disabled="!inputText.trim() || isStreaming">
                  <span v-if="isStreaming" class="send-spinner"></span>
                  <span v-else>发送</span>
                </button>
              </div>
            </div>
          </div>
          <div class="input-hint">
            <span>Ctrl+Enter 发送</span>
            <span v-if="isRetrying" class="retry-hint">⏳ 重试中...</span>
            <span class="ai-disclaimer">本回答由 AI 生成，内容仅供参考，请仔细甄别</span>
          </div>
        </div>
      </main>

      <!-- 点击弹出的浮窗 -->
      <div v-if="showContext" class="context-popup" @click.self="showContext = false">
        <div class="context-popup-inner">
          <div class="context-popup-header">
            <span class="context-popup-title">当前上下文</span>
            <button class="context-popup-close" @click="showContext = false">×</button>
          </div>
          <div class="context-popup-body">
            <div class="context-item"><span class="context-label">作品</span><span class="context-value">{{ project?.title || '未选择' }}</span></div>
            <div class="context-item"><span class="context-label">总字数</span><span class="context-value">{{ totalWords.toLocaleString() }} 字</span></div>
            <div class="context-item"><span class="context-label">章节数</span><span class="context-value">{{ chapters.length }} 章</span></div>
            <div class="context-divider"></div>
            <div class="context-modules">
              <span class="context-module">大纲</span>
              <span class="context-module">人物</span>
              <span class="context-module">世界观</span>
              <span class="context-module">情节</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { chatApi } from '@/api'
import { useNovelStore } from '@/stores/novel'

const MAX_CONTEXT_TURNS = 10

const store = useNovelStore()
const router = useRouter()
const route = useRoute()
const messagesContainer = ref(null)
const inputTextarea = ref(null)

const currentAgent = ref('editor')
const inputText = ref('')
const isStreaming = ref(false)
const isRetrying = ref(false)
const messages = ref([])
const sessions = ref([])
const currentSessionId = ref(null)
const activeAbortController = ref(null)

// 侧边栏抽屉状态（默认收起）
const sidebarOpen = ref(false)
// 上下文弹窗显示状态
const showContext = ref(false)
// 数据加载状态
const loadingSessions = ref(false)
const loadingMessages = ref(false)

// 降级回应知识库
const fallbackKnowledgeBase = {
  rhythm: '【写作建议】关于节奏控制：\n\n1. **张弛结合**：每 1500-2000 字安排一个高潮点，中间穿插缓冲场景\n2. **悬念钩子**：在章节结尾留下悬念，吸引读者继续阅读\n3. **情绪递进**：重要场景逐步升级紧张感，不要一开始就达到顶点\n\n💡 建议：可以在草稿完成后，用「大声朗读」来感受节奏是否流畅。',
  character: '【写作建议】关于人物对话：\n\n1. **差异化声音**：每个角色应有独特的说话方式、用词习惯\n2. **潜台词**：好的对话不只是字面意思，还应包含角色的真实想法\n3. **推进情节**：对话应推动故事发展，而非仅仅传递信息\n\n💡 建议：尝试只读对话部分，看能否猜出是谁在说话。',
  style: '【写作建议】关于文风匹配：\n\n1. **保持一致性**：注意用词、句式、语气的一致性\n2. **意象系统**：建立属于自己的意象符号（如月光代表思念）\n3. **节奏感**：长短句交错，让文章有呼吸感\n\n💡 建议：可以选取一段最喜欢的文字，反复模仿练习。',
  reader: '【写作建议】关于读者视角：\n\n1. **代入感**：检查读者能否轻易代入主角视角\n2. **期待感**：每个章节应让读者想知道「然后呢」\n3. **满足感**：适时给读者「奖赏」（如揭晓悬念、情感爆发）\n\n💡 建议：写完后放置一天，用陌生读者的眼光重读。',
  default: '【写作帮助】\n\n我暂时无法连接到 AI 服务，但这里有一些写作建议：\n\n1. **遇到瓶颈时**：先跳过卡住的部分，从其他场景继续\n2. **角色不立体**：给角色增加一个小习惯或口头禅\n3. **情节太平**：添加一个意外事件或干扰因素\n4. **对话不自然**：大声朗读出来，听听是否像真人说话\n\n💡 如果问题紧急，可以尝试：\n- 简化问题描述\n- 检查网络连接\n- 稍后再试'
}

const project = computed(() => store.currentProject)
const chapters = computed(() => store.chapters || [])
const totalWords = computed(() => chapters.value.reduce((s, c) => s + (c.wordCount || 0), 0))

const agents = [
  { key: 'editor', icon: '✍️', label: '编辑', badge: '节奏/逻辑' },
  { key: 'character', icon: '👤', label: '人物', badge: '行为/对话' },
  { key: 'style', icon: '🎨', label: '风格', badge: '文风匹配' },
  { key: 'reader', icon: '📖', label: '读者', badge: '视角反馈' }
]

const quickPrompts = [
  { key: 'editor', text: '帮我分析本章的节奏，指出张弛点' },
  { key: 'character', text: '这个人物的行为是否合理？有什么建议？' },
  { key: 'style', text: '如何提升这段描写的文风张力？' },
  { key: 'reader', text: '从读者视角看，这段内容吸引人吗？' }
]

const greetings = {
  editor: '你好！我是编辑 Agent，可以帮你分析章节节奏、检测逻辑矛盾、评估伏笔回收。',
  character: '你好！我是人物 Agent，可以帮你设计角色行为、生成对话、评估人物弧光。',
  style: '你好！我是风格 Agent，可以帮你匹配文风、优化修辞、保持语言一致性。',
  reader: '你好！我是读者 Agent，可以从读者视角分析吸引力、预测期待值。'
}

const currentAgentLabel = computed(() => {
  const a = agents.find(x => x.key === currentAgent.value)
  return a ? a.icon + ' ' + a.label : '编辑'
})

const estimatedTokens = computed(() => Math.round(messages.value.reduce((s, m) => s + (m.content?.length || 0), 0) * 0.7))

// ═══════════════════════════════════════════
//  会话管理（全部基于后端 API）
// ═══════════════════════════════════════════

/** 从后端刷新会话列表 */
async function refreshSessionList() {
  const pid = store.currentProjectId
  if (!pid) return
  try {
    const data = await chatApi.listSessions(pid)
    if (Array.isArray(data)) {
      sessions.value = data.map(s => ({
        id: s.id,
        sessionId: s.sessionId || s.id,
        title: s.title || '新对话',
        preview: s.preview || '',
        createTime: s.createTime || new Date().toISOString(),
        updateTime: s.updateTime || s.createTime || new Date().toISOString()
      }))
    }
  } catch (e) {
    console.warn('刷新会话列表失败:', e)
  }
}

async function createNewSession() {
  if (isStreaming.value) return

  const pid = store.currentProjectId
  if (!pid) {
    messages.value = [{ role: 'ai', content: '请先选择一个作品。', agent: currentAgent.value, createTime: new Date().toISOString(), isStreaming: false }]
    return
  }

  try {
    loadingSessions.value = true
    const data = await chatApi.createSession(pid, { title: '新对话' })
    const newSession = {
      id: data.sessionId || data.id,
      title: data.title || '新对话',
      preview: '',
      createTime: data.createTime || new Date().toISOString(),
      updateTime: data.updateTime || data.createTime || new Date().toISOString()
    }
    sessions.value.unshift(newSession)
    currentSessionId.value = newSession.id
    messages.value = []
    showWelcome()
  } catch (e) {
    console.error('创建会话失败:', e)
    // 降级：创建本地临时会话以便能继续发送消息
    const fallbackId = Date.now()
    const fallback = {
      id: fallbackId,
      title: '新对话',
      preview: '',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
    sessions.value.unshift(fallback)
    currentSessionId.value = fallbackId
    messages.value = []
    showWelcome()
  } finally {
    loadingSessions.value = false
  }
}

async function loadSessionMessages(sid) {
  const pid = store.currentProjectId
  if (!pid) return

  loadingMessages.value = true
  try {
    const msgs = await chatApi.getSessionMessages(pid, sid)
    if (msgs?.length) {
      messages.value = msgs
        .filter(m => m.role !== 'system')
        .map(m => ({
          id: m.id,
          role: m.role === 'assistant' ? 'ai' : m.role,
          content: m.content || '',
          agent: m.agent || 'editor',
          createTime: m.createTime,
          isStreaming: false
        }))
    } else {
      messages.value = []
      showWelcome()
    }
  } catch (e) {
    console.warn('加载消息失败:', e)
    messages.value = []
    showWelcome()
  } finally {
    loadingMessages.value = false
    scrollToBottom()
  }
}

async function switchSession(sid) {
  if (isStreaming.value) return

  currentSessionId.value = sid
  messages.value = []
  await loadSessionMessages(sid)
}

async function deleteSession(sid) {
  if (!confirm('确定要删除这个会话吗？')) return

  const idx = sessions.value.findIndex(s => s.id === sid)
  if (idx === -1) return

  // 先从列表移除（乐观更新）
  sessions.value.splice(idx, 1)

  // 调用后端 API 删除
  const pid = store.currentProjectId
  if (pid) {
    try {
      await chatApi.deleteSession(pid, sid)
    } catch (e) {
      console.warn('删除会话失败:', e)
    }
  }

  if (currentSessionId.value === sid) {
    if (sessions.value.length > 0) {
      switchSession(sessions.value[0].id)
    } else {
      createNewSession()
    }
  }
}

// ═══════════════════════════════════════════
//  消息管理
// ═══════════════════════════════════════════

function showWelcome() {
  messages.value = [{
    role: 'ai',
    content: greetings[currentAgent.value] || greetings.editor,
    agent: currentAgent.value,
    createTime: new Date().toISOString(),
    isStreaming: false
  }]
}

function switchAgent(key) {
  if (isStreaming.value) return
  const agentLabel = agents.find(a => a.key === key)?.label || key
  currentAgent.value = key

  if (messages.value.length === 0) {
    showWelcome()
  } else if (messages.value.length === 1 && messages.value[0].role === 'ai' && !messages.value[0].content.includes('已切换到')) {
    showWelcome()
  } else {
    messages.value.push({
      role: 'system',
      content: `🔄 已切换到「${agentLabel} Agent」`,
      agent: key,
      createTime: new Date().toISOString(),
      isStreaming: false
    })
    scrollToBottom()
  }

}

// ═══════════════════════════════════════════
//  发送消息（增强版：停止、降级回应）
// ═══════════════════════════════════════════

function getFallbackResponse(agentKey) {
  return fallbackKnowledgeBase[agentKey] || fallbackKnowledgeBase.default
}

function stopGeneration() {
  if (activeAbortController.value) {
    activeAbortController.value.abort()
    activeAbortController.value = null
  }
  isStreaming.value = false
  isRetrying.value = false

  // 将正在流式输出的消息标记为完成
  const streamingIdx = messages.value.findIndex(m => m.isStreaming)
  if (streamingIdx !== -1) {
    messages.value[streamingIdx].isStreaming = false
  }
  scrollToBottom()
}

async function sendMessage() {
  if (!inputText.value.trim() || isStreaming.value) return

  const pid = store.currentProjectId
  if (!pid) {
    messages.value.push({ role: 'ai', content: '未选择项目，请先选择一个作品。', agent: currentAgent.value, createTime: new Date().toISOString(), isStreaming: false })
    scrollToBottom()
    return
  }

  // 检查输入长度
  if (inputText.value.length > 2000) {
    messages.value.push({ role: 'ai', content: '输入内容过长（超过 2000 字），请精简或分段发送。', agent: currentAgent.value, createTime: new Date().toISOString(), isStreaming: false })
    scrollToBottom()
    return
  }

  // 确保有当前会话（createNewSession 现在是异步的）
  if (!currentSessionId.value) {
    await createNewSession()
    // 创建失败仍无会话 ID 时终止
    if (!currentSessionId.value) return
  }

  const userMsg = inputText.value.trim()
  inputText.value = ''

  messages.value.push({ role: 'user', content: userMsg, agent: currentAgent.value, createTime: new Date().toISOString(), isStreaming: false })

  const aiIdx = messages.value.length
  messages.value.push({ role: 'ai', content: '', agent: currentAgent.value, createTime: new Date().toISOString(), isStreaming: true })
  isStreaming.value = true
  scrollToBottom()

  let sp = false
  const throttledScroll = () => { if (!sp) { sp = true; requestAnimationFrame(() => { scrollToBottom(); sp = false }) } }

  const abortController = new AbortController()
  activeAbortController.value = abortController

  try {
    const proj = store.currentProject

    await chatApi.streamChat(pid, {
        userMessage: userMsg,
        agent: currentAgent.value,
        sessionId: currentSessionId.value,
        novelTitle: proj?.title || '',
        genre: proj?.genre || '',
        currentChapter: chapters.value.length ? '第' + chapters.value.length + '章' : ''
      },
      (token) => {
        if (abortController.signal.aborted) return
        const t = messages.value[aiIdx]
        if (t && token != null && token !== 'null' && token !== 'undefined') { t.content += token; throttledScroll() }
      },
      () => {
        if (abortController.signal.aborted) return
        const t = messages.value[aiIdx]
        if (t) t.isStreaming = false
        isStreaming.value = false
        isRetrying.value = false
        activeAbortController.value = null
        scrollToBottom()
        refreshSessionList()
      },
      (err) => {
        if (abortController.signal.aborted) return

        const t = messages.value[aiIdx]
        isStreaming.value = false
        isRetrying.value = false
        activeAbortController.value = null

        let errorMsg = err.message || '未知错误'

        if (errorMsg.includes('timeout') || errorMsg.includes('超时')) {
          errorMsg = '请求超时，AI 反应较慢，请重试'
        } else if (errorMsg.includes('Failed to fetch') || errorMsg.includes('网络')) {
          errorMsg = '网络连接失败，请检查网络后重试'
        }

        // 检查是否是客户端错误（不应该重试）
        const isClientError = errorMsg.includes('401') || errorMsg.includes('400') || errorMsg.includes('未选择')

        if (isClientError) {
          if (t) { t.content = '❌ ' + errorMsg; t.isStreaming = false }
        } else {
          // 服务端错误，使用降级回应
          if (t) {
            t.content = ''
            t.isStreaming = true
            isStreaming.value = true

            // 模拟流式输出降级内容
            const fallback = getFallbackResponse(currentAgent.value)
            let i = 0
            const streamFallback = () => {
              if (abortController.signal.aborted || i >= fallback.length) {
                if (t) { t.isStreaming = false }
                isStreaming.value = false
                scrollToBottom()
                refreshSessionList()
                return
              }
              t.content += fallback[i]
              i++
              throttledScroll()
              setTimeout(streamFallback, 15)
            }
            streamFallback()
          }
        }
        scrollToBottom()
        refreshSessionList()
      }
    )
  } catch (e) {
    const t = messages.value[aiIdx]
    if (t) { t.content = '请求异常：' + (e.message || '未知错误'); t.isStreaming = false }
    messages.value = [...messages.value]
    isStreaming.value = false
    isRetrying.value = false
    activeAbortController.value = null
    scrollToBottom()
    refreshSessionList()
  }
}

function getAgentIcon(k) { const a = agents.find(x => x.key === k); return a ? a.icon : '🤖' }
function getAgentLabel(k) { const a = agents.find(x => x.key === k); return a ? a.label + ' Agent' : 'AI 助手' }
function formatSessionTime(t) { if (!t) return ''; const d = new Date(t), now = new Date(), diff = (now - d) / 1000; if (diff < 3600) return Math.floor(diff / 60) + '分钟前'; if (diff < 86400) return Math.floor(diff / 3600) + '小时前'; return d.toLocaleDateString('zh-CN') }
function formatMsgTime(t) { return t ? new Date(t).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '' }
function scrollToBottom() { nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight }) }
function copyMessage(c) { navigator.clipboard.writeText(c) }

// 多Agent协作状态
const orchestratorResult = ref(null)
const isOrchestrating = ref(false)
const showAnalysisHistory = ref(false)
const analysisHistory = ref([])
const analysisHistoryLoading = ref(false)

// 分析历史相关方法
async function loadAnalysisHistory() {
  const pid = store.currentProjectId
  if (!pid) return
  analysisHistoryLoading.value = true
  try {
    const data = await chatApi.getAnalysisHistory(pid)
    analysisHistory.value = data || []
  } catch (e) {
    console.error('加载分析历史失败:', e)
    analysisHistory.value = []
  } finally {
    analysisHistoryLoading.value = false
  }
}

function viewAnalysis(item) {
  orchestratorResult.value = {
    editorResult: { content: item.editorResult },
    characterResult: { content: item.characterResult },
    styleResult: { content: item.styleResult },
    readerResult: { content: item.readerResult },
    summary: item.summary,
    totalCostMs: item.totalCostMs
  }
  showAnalysisHistory.value = false
}

async function deleteAnalysis(id) {
  if (!confirm('确定要删除这条分析记录吗？')) return
  const pid = store.currentProjectId
  if (!pid) return
  try {
    await chatApi.deleteAnalysis(pid, id)
    analysisHistory.value = analysisHistory.value.filter(item => item.id !== id)
  } catch (e) {
    console.error('删除分析记录失败:', e)
    alert('删除失败: ' + (e.message || '未知错误'))
  }
}

function formatAnalysisTime(isoString) {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function toggleAnalysisHistory() {
  showAnalysisHistory.value = !showAnalysisHistory.value
  if (showAnalysisHistory.value) {
    await loadAnalysisHistory()
  }
}

function quickPrompt(prompt) {
  if (isStreaming.value) return
  inputText.value = prompt.text
  nextTick(() => sendMessage())
}

// ═══════════════════════════════════════════
//  多Agent协作分析
// ═══════════════════════════════════════════

async function triggerOrchestrator() {
  const pid = store.currentProjectId
  if (!pid) {
    alert('请先选择一个作品')
    return
  }

  if (isOrchestrating.value) return

  const currentChapter = chapters.value.length > 0 ? chapters.value[chapters.value.length - 1] : null
  const chapterContent = currentChapter?.content || ''
  const chapterTitle = currentChapter?.title || ''
  const chapterIndex = currentChapter?.sortOrder || chapters.value.length

  if (!chapterContent || chapterContent.length < 50) {
    alert('章节内容太少，请先写一些内容（至少50字）再进行综合分析')
    return
  }

  isOrchestrating.value = true
  orchestratorResult.value = null

  try {
    const data = await chatApi.orchestrate(pid, {
      chapterContent,
      chapterTitle,
      chapterIndex,
      goldSamples: '',
      readerType: '普通'
    })

    if (data) {
      orchestratorResult.value = {
        editorResult: { content: data.editorResult?.content },
        characterResult: { content: data.characterResult?.content },
        styleResult: { content: data.styleResult?.content },
        readerResult: { content: data.readerResult?.content },
        summary: data.summary,
        totalCostMs: data.totalCostMs
      }
      messages.value.push({
        role: 'system',
        content: '🤖 多Agent综合分析完成，请查看下方报告',
        agent: 'orchestrator',
        createTime: new Date().toISOString(),
        isStreaming: false
      })
      scrollToBottom()
    }
  } catch (e) {
    console.error('多Agent协作分析失败:', e)
    alert('分析失败: ' + (e.message || '未知错误'))
  } finally {
    isOrchestrating.value = false
  }
}

function closeOrchestrator() {
  orchestratorResult.value = null
}

// ═══════════════════════════════════════════
//  初始化（完全基于后端 API）
// ═══════════════════════════════════════════

onMounted(async () => {
  // 清理 localStorage 中的旧会话数据，避免干扰
  try {
    localStorage.removeItem('novelcraft_chat_sessions')
    localStorage.removeItem('novelcraft_current_session')
  } catch (_) { /* ignore */ }

  const pid = store.currentProjectId
  if (!pid) {
    createNewSession()
    return
  }

  loadingSessions.value = true
  try {
    const data = await chatApi.listSessions(pid)
    if (Array.isArray(data) && data.length > 0) {
      sessions.value = data.map(s => ({
        id: s.id,
        sessionId: s.sessionId || s.id,
        title: s.title || '新对话',
        preview: s.preview || '',
        createTime: s.createTime || new Date().toISOString(),
        updateTime: s.updateTime || s.createTime || new Date().toISOString()
      }))
      // 切换到第一个会话
      await switchSession(sessions.value[0].id)
    } else {
      // 无历史会话，创建新会话
      await createNewSession()
    }
  } catch (e) {
    console.warn('加载会话列表失败:', e)
    await createNewSession()
  } finally {
    loadingSessions.value = false
  }
})
</script>

<style scoped>
/* 主容器：全屏布局 */
.ai-chat { padding: 8px 12px 12px; max-width: 100%; margin: 0; height: 100vh; display: flex; flex-direction: column; background: #f8f6f2; box-sizing: border-box; }
/* 头部：固定高度 */
.chat-header-bar { display: flex; justify-content: space-between; align-items: center; height: 52px; padding: 0 12px; border-bottom: 1px solid #f0ece6; margin-bottom: 8px; flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 16px; }
.chat-title { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0; }
.header-status { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #94a3b8; background: #fff; padding: 4px 14px; border-radius: 20px; border: 1px solid #f0ece6; }
.status-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; }
.status-dot.online { background: #10b981; animation: blink 1.8s ease-in-out infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.status-divider { color: #d4d0ca; }
.status-agent { color: #6b6560; font-weight: 500; }
.header-right { display: flex; align-items: center; gap: 10px; }
.token-display { display: flex; align-items: center; gap: 8px; background: #fff; padding: 4px 14px 4px 10px; border-radius: 20px; border: 1px solid #f0ece6; font-size: 12px; color: #94a3b8; }
.token-icon { font-size: 14px; }
.token-bar { width: 60px; height: 3px; background: #f0ece6; border-radius: 2px; overflow: hidden; }
.token-fill { height: 100%; background: #d97706; border-radius: 2px; transition: width 0.4s; }
.btn-header { padding: 6px 14px; font-size: 12px; font-weight: 600; background: #d97706; color: #fff; border: none; border-radius: 8px; cursor: pointer; transition: background 0.2s; display: flex; align-items: center; gap: 6px; }
.btn-header:hover { background: #b45309; }
/* 上下文按钮 */
.btn-context { background: #fff; color: #6b6560; border: 1px solid #e8e3dc; }
.btn-context:hover { border-color: #d97706; color: #d97706; }
.btn-context.active { background: #d97706; color: #fff; border-color: #d97706; }
.btn-context-icon { font-size: 14px; }
/* 聊天主体：填满剩余空间 */
.chat-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-height: 0; }
/* 侧边栏：固定定位滑动抽屉 */
.chat-sidebar { position: fixed; left: 0; top: 0; width: 280px; height: 100vh; background: #fff; border-right: 1px solid #f0ece6; display: flex; flex-direction: column; z-index: 101; transform: translateX(-100%); transition: transform 0.3s ease; box-shadow: 4px 0 20px rgba(0,0,0,0.1); }
.chat-sidebar.open { transform: translateX(0); }
.sidebar-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #f0ece6; }
.sidebar-actions { display: flex; gap: 6px; }
.sidebar-title { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.btn-add-session, .btn-close-sidebar { width: 28px; height: 28px; border: 1px solid #e8e3dc; border-radius: 8px; background: #fff; color: #6b6560; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.btn-add-session:hover, .btn-close-sidebar:hover { background: #f5f3ef; border-color: #d0c8be; }
.session-list { flex: 1; overflow-y: auto; padding: 8px; }
.session-item { padding: 10px 12px; border-radius: 10px; cursor: pointer; transition: all 0.2s; margin-bottom: 4px; }
.session-item:hover { background: #faf8f5; }
.session-item.active { background: #fef3c7; }
.session-info { display: flex; flex-direction: column; gap: 2px; }
.session-title { font-size: 13px; font-weight: 600; color: #1a1a2e; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-preview { font-size: 11px; color: #94a3b8; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.session-time { font-size: 10px; color: #b8b0a8; }
.btn-delete-session { background: none; border: none; font-size: 12px; cursor: pointer; opacity: 0; transition: opacity 0.2s; padding: 2px; }
.session-item:hover .btn-delete-session { opacity: 0.6; }
.btn-delete-session:hover { opacity: 1 !important; }
.session-empty { text-align: center; padding: 40px 16px; color: #94a3b8; font-size: 13px; }
.session-empty .empty-icon { font-size: 32px; display: block; margin-bottom: 8px; }
.empty-hint { display: block; font-size: 11px; margin-top: 4px; }
/* 主聊天区域：填满剩余空间 */
.chat-main { flex: 1; display: flex; flex-direction: column; background: #fff; border: 1px solid #f0ece6; border-radius: 14px; overflow: hidden; min-height: 0; }
.chat-toolbar { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-bottom: 1px solid #f0ece6; flex-wrap: wrap; }
.agent-tabs { display: flex; gap: 4px; }
.agent-tab { display: flex; align-items: center; gap: 4px; padding: 6px 12px; border: 1px solid #e8e3dc; border-radius: 20px; background: #fff; font-size: 12px; font-weight: 600; color: #6b6560; cursor: pointer; transition: all 0.2s; }
.agent-tab:hover { border-color: #d0c8be; }
.agent-tab.active { background: #d97706; color: #fff; border-color: #d97706; }
.agent-icon { font-size: 14px; }
.agent-badge { font-size: 10px; padding: 0 6px; border-radius: 8px; background: rgba(0,0,0,0.06); color: #94a3b8; }
.agent-tab.active .agent-badge { background: rgba(255,255,255,0.2); color: #fff; }
/* 消息容器：占据全部剩余空间 */
.messages-container { flex: 1; overflow-y: auto; padding: 12px 16px; min-height: 0; }
.messages-empty { text-align: center; padding: 60px 20px; color: #94a3b8; }
.messages-empty .empty-illustration { font-size: 48px; margin-bottom: 12px; }
.messages-empty h3 { font-size: 18px; color: #1a1a2e; margin: 0 0 8px; }
.messages-empty p { font-size: 14px; margin: 0 0 20px; }
.quick-prompts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.quick-prompt { padding: 6px 14px; border: 1px solid #e8e3dc; border-radius: 20px; background: #fff; font-size: 12px; color: #6b6560; cursor: pointer; transition: all 0.2s; }
.quick-prompt:hover { border-color: #d97706; color: #d97706; }
.quick-prompt.active { background: #d97706; color: #fff; border-color: #d97706; }
.message { display: flex; gap: 10px; margin-bottom: 16px; }
.message.user { flex-direction: row-reverse; }
.message-avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; font-size: 16px; }
.message.user .message-avatar { background: #d97706; }
.message.ai .message-avatar { background: #f3efe8; }
.message-body { max-width: 75%; }
.message.user .message-body { text-align: right; }
.message-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.message.user .message-meta { justify-content: flex-end; }
.message-sender { font-size: 12px; font-weight: 600; color: #6b6560; }
.message-time { font-size: 11px; color: #b8b0a8; }
.message-content { padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.message.user .message-content { background: #d97706; color: #fff; border-top-right-radius: 4px; }
.message.ai .message-content { background: #faf8f5; color: #1a1a2e; border-top-left-radius: 4px; }
.typing-indicator { display: flex; gap: 4px; padding: 8px 14px; }
.typing-indicator span { width: 6px; height: 6px; border-radius: 50%; background: #d97706; animation: bounce 1.4s infinite ease-in-out; }
.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.16s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.32s; }
@keyframes bounce { 0%, 80%, 100% { transform: translateY(0); } 40% { transform: translateY(-6px); } }

/* 系统消息：居中提示文本 */
.system-message { text-align: center; padding: 6px 0; width: 100%; }
.system-msg-text { display: inline-block; font-size: 12px; color: #b8b0a8; background: #f5f3ef; padding: 4px 14px; border-radius: 12px; font-weight: 500; }
.message-actions { display: flex; gap: 4px; margin-top: 4px; }
.message.user .message-actions { justify-content: flex-end; }
.message-actions button { background: none; border: none; font-size: 14px; cursor: pointer; padding: 2px 4px; opacity: 0.5; transition: opacity 0.2s; }
.message-actions button:hover { opacity: 1; }
/* 输入区：固定底部，不压缩 */
.input-area { flex-shrink: 0; padding: 8px 14px 10px; border-top: 1px solid #f0ece6; }
.input-wrapper { border: 1.5px solid #e8e3dc; border-radius: 12px; overflow: hidden; transition: border-color 0.2s; }
.input-wrapper:focus-within { border-color: #d97706; }
.input-textarea { width: 100%; padding: 10px 14px; border: none; outline: none; resize: none; font-size: 14px; font-family: inherit; background: #faf8f5; box-sizing: border-box; }
.input-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 6px 14px; background: #faf8f5; }
.input-char-count { font-size: 11px; color: #b8b0a8; }
.input-actions { display: flex; gap: 8px; align-items: center; }
.btn-stop { padding: 6px 14px; background: #dc2626; color: #fff; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 4px; }
.btn-stop:hover { background: #b91c1c; }
.stop-icon { font-size: 12px; }
.btn-send { padding: 6px 20px; background: #e8e3dc; color: #fff; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 4px; }
.btn-send.active { background: #d97706; }
.btn-send:disabled { opacity: 0.5; cursor: not-allowed; }
.send-spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.4); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.input-hint { display: flex; justify-content: center; align-items: center; font-size: 11px; color: #b8b0a8; margin-top: 6px; flex-wrap: wrap; gap: 8px; }
.retry-hint { color: #d97706; font-weight: 500; }
.ai-disclaimer { color: #9ca3af; }
/* 上下文弹窗 */
.context-popup { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 100; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.3); animation: fadeIn 0.2s ease; }
.context-popup-inner { width: 320px; max-height: 80vh; background: #fff; border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); overflow: hidden; animation: slideUp 0.3s ease; }
.context-popup-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f0ece6; }
.context-popup-title { font-size: 16px; font-weight: 600; color: #1a1a2e; }
.context-popup-close { width: 28px; height: 28px; border: none; border-radius: 8px; background: #f5f3ef; color: #6b6560; font-size: 18px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.context-popup-close:hover { background: #e8e3dc; }
.context-popup-body { padding: 20px; overflow-y: auto; max-height: calc(80vh - 60px); }
.context-item { margin-bottom: 16px; }
.context-label { display: block; font-size: 12px; color: #94a3b8; margin-bottom: 4px; }
.context-value { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.context-divider { height: 1px; background: #f0ece6; margin: 20px 0; }
.context-modules { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.context-module { padding: 10px; background: #faf8f5; border-radius: 10px; font-size: 13px; color: #6b6560; text-align: center; cursor: pointer; transition: all 0.2s; }
.context-module:hover { background: #f3efe8; }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

/* 响应式 */
/* 遮罩层 */
.sidebar-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.3); z-index: 100; }
/* 汉堡菜单按钮 */
.btn-menu { background: none; border: none; font-size: 20px; cursor: pointer; padding: 4px 8px; color: #6b6560; transition: color 0.2s; }
.btn-menu:hover { color: #1a1a2e; }

/* 多Agent协作按钮 */
.orchestrator-btn { display: flex; align-items: center; gap: 4px; padding: 6px 14px; border: 1.5px solid #7c3aed; border-radius: 20px; background: #fff; font-size: 12px; font-weight: 600; color: #7c3aed; cursor: pointer; transition: all 0.2s; margin-left: auto; }
.orchestrator-btn:hover { background: #7c3aed; color: #fff; }
.orchestrator-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.orchestrator-btn.loading { background: #f5f3ef; color: #94a3b8; border-color: #e8e3dc; }
.orchestrator-spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid #e8e3dc; border-top-color: #7c3aed; border-radius: 50%; animation: spin 0.7s linear infinite; }

/* 历史按钮 */
.history-btn { display: flex; align-items: center; gap: 4px; padding: 6px 14px; border: 1.5px solid #10b981; border-radius: 20px; background: #fff; font-size: 12px; font-weight: 600; color: #10b981; cursor: pointer; transition: all 0.2s; }
.history-btn:hover { background: #10b981; color: #fff; }
.history-btn.active { background: #10b981; color: #fff; }

/* 多Agent协作结果 */
.orchestrator-result { padding: 16px; border-top: 1px solid #f0ece6; background: #faf8f5; max-height: 50vh; overflow-y: auto; }
.orchestrator-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.orchestrator-header h3 { font-size: 16px; color: #1a1a2e; margin: 0; }
.orchestrator-close { width: 28px; height: 28px; border: none; border-radius: 8px; background: #f5f3ef; color: #6b6560; font-size: 18px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.orchestrator-close:hover { background: #e8e3dc; }
.orchestrator-cost { font-size: 12px; color: #94a3b8; margin-bottom: 12px; }
.agent-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 16px; }
.agent-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.agent-card-header { padding: 10px 14px; font-size: 13px; font-weight: 600; color: #fff; }
.agent-card.editor .agent-card-header { background: #d97706; }
.agent-card.character .agent-card-header { background: #0891b2; }
.agent-card.style .agent-card-header { background: #7c3aed; }
.agent-card.reader .agent-card-header { background: #059669; }
.agent-card-body { padding: 12px 14px; font-size: 13px; line-height: 1.6; color: #1a1a2e; max-height: 150px; overflow-y: auto; white-space: pre-wrap; }
.orchestrator-summary { background: #fef3c7; border-radius: 12px; padding: 14px 16px; }
.summary-header { font-size: 14px; font-weight: 600; color: #92400e; margin-bottom: 8px; }
.summary-content { font-size: 14px; line-height: 1.6; color: #78350f; white-space: pre-wrap; }

/* 分析历史面板 */
.analysis-history-panel { padding: 16px; border-top: 1px solid #f0ece6; background: #faf8f5; max-height: 60vh; overflow-y: auto; }
.analysis-history-panel .history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.analysis-history-panel .history-header h3 { font-size: 16px; color: #1a1a2e; margin: 0; }
.analysis-history-panel .close-btn { width: 28px; height: 28px; border: none; border-radius: 8px; background: #f5f3ef; color: #6b6560; font-size: 18px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.analysis-history-panel .close-btn:hover { background: #e8e3dc; }
.analysis-history-panel .history-loading, .analysis-history-panel .history-empty { text-align: center; padding: 24px; color: #94a3b8; font-size: 14px; }
.analysis-history-panel .history-list { display: flex; flex-direction: column; gap: 8px; }
.analysis-history-panel .history-item { position: relative; padding: 12px 14px; background: #fff; border-radius: 10px; cursor: pointer; transition: all 0.2s; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.analysis-history-panel .history-item:hover { background: #fef9e7; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
.analysis-history-panel .history-item-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.analysis-history-panel .history-chapter { font-size: 13px; font-weight: 600; color: #1a1a2e; }
.analysis-history-panel .history-time { font-size: 11px; color: #94a3b8; }
.analysis-history-panel .history-item-summary { font-size: 12px; color: #6b6560; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: calc(100% - 40px); }
.analysis-history-panel .history-delete { position: absolute; top: 8px; right: 8px; width: 24px; height: 24px; border: none; border-radius: 6px; background: transparent; color: #94a3b8; font-size: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.analysis-history-panel .history-delete:hover { background: #fee2e2; color: #dc2626; }

@media (max-width: 768px) { .ai-chat { padding: 8px; } .header-right { gap: 6px; } .btn-header { padding: 6px 10px; font-size: 11px; } }
</style>