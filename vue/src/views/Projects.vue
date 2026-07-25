<template>
  <div class="my-works-page">
    <!-- ══════════ 顶部标题栏 ══════════ -->
    <header class="page-header">
      <div class="header-left">
        <h1 class="page-title">📚 我的作品</h1>
        <span class="header-subtitle">管理你的创作，让灵感成真</span>
      </div>
      <div class="header-actions">
        <router-link to="/ai-setup" class="btn-setup">
          🚀 AI 先导创作
        </router-link>
        <button class="btn-new" @click="openCreateModal()">
          <span class="btn-icon">＋</span> 新建作品
        </button>
      </div>
    </header>

    <!-- ══════════ 统计卡片区 ══════════ -->
    <section class="stats-grid" v-if="!loading">
      <div class="stat-card stat-card-total">
        <div class="stat-icon">📖</div>
        <div class="stat-content">
          <span class="stat-number">{{ projects.length }}</span>
          <span class="stat-label">总作品</span>
        </div>
        <div class="stat-trend up">+0%</div>
      </div>
      <div class="stat-card stat-card-ongoing">
        <div class="stat-icon">✍️</div>
        <div class="stat-content">
          <span class="stat-number">{{ ongoingCount }}</span>
          <span class="stat-label">连载中</span>
        </div>
        <div class="stat-trend">{{ ongoingCount > 0 ? '创作中' : '—' }}</div>
      </div>
      <div class="stat-card stat-card-done">
        <div class="stat-icon">✅</div>
        <div class="stat-content">
          <span class="stat-number">{{ completedCount }}</span>
          <span class="stat-label">已完成</span>
        </div>
        <div class="stat-trend">{{ completedCount > 0 ? '已完成' : '—' }}</div>
      </div>
      <div class="stat-card stat-card-words">
        <div class="stat-icon">📝</div>
        <div class="stat-content">
          <span class="stat-number">{{ totalWords }}</span>
          <span class="stat-label">总字数</span>
        </div>
        <div class="stat-trend up">累计</div>
      </div>
    </section>

    <!-- ══════════ 作品列表 ══════════ -->
    <section class="works-list" v-if="!loading">
      <!-- 列表工具栏 -->
      <div class="list-toolbar" v-if="projects.length > 0">
        <div class="toolbar-left">
          <span class="list-title">我的作品集</span>
          <span class="list-count">共 {{ projects.length }} 部作品</span>
        </div>
        <div class="toolbar-right">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索作品..."
              class="search-input"
            />
          </div>
          <button class="btn-sort" @click="toggleSort">
            <span>📅</span> 最近更新
          </button>
          <button class="btn-view" @click="toggleView">
            <span>{{ viewMode === 'grid' ? '☰' : '⊞' }}</span>
          </button>
        </div>
      </div>

      <!-- 作品卡片网格 -->
      <div class="works-grid" :class="viewMode === 'grid' ? 'grid-view' : 'list-view'" v-if="filteredProjects.length > 0">
        <div
          v-for="project in filteredProjects"
          :key="project.id"
          class="work-card"
          @click="goToWorkspace(project)"
        >
          <!-- 卡片封面区 -->
          <div class="card-cover" :style="{ background: getCoverGradient(project) }">
            <div class="cover-badge" v-if="!project.status || project.status === '连载中' || project.status === 'ongoing'">连载中</div>
            <div class="cover-badge done" v-else-if="project.status === '已完成' || project.status === 'completed'">已完成</div>
            <div class="cover-badge draft" v-else>草稿</div>
            <div class="cover-emoji">{{ getProjectEmoji(project) }}</div>
            <!-- 悬停删除按钮 -->
            <button
              class="card-delete-btn"
              @click.stop="confirmDelete(project)"
              :disabled="deletingId === project.id"
              title="删除作品"
            >
              <span v-if="deletingId === project.id" class="delete-spinner"></span>
              <span v-else>🗑️</span>
            </button>
          </div>

          <!-- 卡片内容 -->
          <div class="card-body">
            <div class="card-header-row">
              <h3 class="card-title">{{ project.title || '未命名' }}</h3>
              <button class="card-menu" @click.stop="openCardMenu(project, $event)">⋯</button>
            </div>
            <div class="card-tags" v-if="getProjectTags(project).length > 0">
              <span v-for="tag in getProjectTags(project).slice(0, 3)" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <div class="card-desc" v-if="project.description">{{ project.description }}</div>
            <div class="card-stats">
              <div class="stat-item">
                <span class="stat-value">{{ formatCardNumber(project.wordCount || 0) }}</span>
                <span class="stat-unit">字</span>
              </div>
              <span class="stat-divider">·</span>
              <div class="stat-item">
                <span class="stat-value">{{ project.chapterCount || 0 }}</span>
                <span class="stat-unit">章</span>
              </div>
              <span class="stat-divider">·</span>
              <div class="stat-item">
                <span class="stat-value">{{ getProjectDuration(project) }}</span>
                <span class="stat-unit">月</span>
              </div>
            </div>
            <div class="card-progress" v-if="project.targetWordCount">
              <div class="progress-header">
                <span class="progress-label">目标 {{ formatWan(project.targetWordCount) }}字，完成</span>
                <span class="progress-percent">{{ getProgressPercent(project) }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: Math.min(getProgressPercent(project), 100) + '%' }"></div>
              </div>
            </div>
            <div class="card-footer">
              <span class="card-time">🕐 {{ formatUpdateTime(project) }}</span>
              <button class="btn-continue" @click.stop="goToWorkspace(project)">
                继续写作 →
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="projects.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <h3>还没有作品</h3>
        <p>开始你的第一部作品，让故事从这里诞生</p>
        <button class="btn-new-large" @click="openCreateModal()">＋ 创建第一部作品</button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="empty-state">
        <div class="empty-icon">⏳</div>
        <h3>加载中...</h3>
      </div>
    </section>

    <!-- 卡片操作菜单弹窗 -->
    <div
      v-if="cardMenuTarget"
      class="fixed inset-0 z-50"
      @click="cardMenuTarget = null"
    >
      <div
        class="absolute bg-white border border-[#f0ece6] rounded-xl shadow-lg py-2 min-w-[140px] z-50"
        :style="{ top: cardMenuY + 'px', left: cardMenuX + 'px' }"
        @click.stop
      >
        <button @click="editProject(cardMenuTarget); cardMenuTarget = null" class="w-full text-left px-4 py-2.5 text-sm hover:bg-[#faf7f2] text-[#334155] transition-colors">✏️ 编辑</button>
        <button @click="handleDelete(cardMenuTarget); cardMenuTarget = null" class="w-full text-left px-4 py-2.5 text-sm hover:bg-[#fef2f2] text-[#be123c] transition-colors">🗑 删除</button>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <DeleteConfirmDialog
      v-model="showDeleteDialog"
      :work="workToDelete"
      :loading="deletingId !== null"
      @confirm="handleDeleteConfirm"
      @cancel="cancelDelete"
    />

    <!-- 新建/编辑 弹窗 -->
    <div v-if="showModal" class="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex items-center justify-center p-5" @click.self="closeModal">
      <div class="bg-white rounded-[20px] shadow-2xl w-full max-w-[820px] max-h-[94vh] overflow-y-auto p-8" style="animation:modalIn 0.25s ease">
        <!-- 标题栏 -->
        <div class="flex justify-between items-center mb-7">
          <h2 class="text-2xl font-bold text-[#0f0f1a]">{{ editingId ? '编辑作品' : '✏️ 新建作品' }}</h2>
          <button @click="closeModal" class="text-[28px] leading-none text-[#94a3b8] hover:text-[#475569] transition-colors px-1">✕</button>
        </div>

        <form @submit.prevent="handleSubmit">
          <!-- 作品名称 + AI起名 -->
          <div class="mb-5">
            <label class="block text-sm font-semibold text-[#334155] mb-1.5">作品名称 <span class="font-normal text-[#94a3b8] text-xs ml-1.5">*</span></label>
            <div class="flex gap-2.5 items-center">
              <input v-model="form.title" class="flex-1 px-3.5 py-2.5 border border-[#e2e8f0] rounded-[10px] text-sm bg-[#fafbfc] focus:border-[#818cf8] focus:shadow-[0_0_0_3px_rgba(99,102,241,0.12)] focus:bg-white outline-none transition-all" placeholder="输入作品名称..." required>
              <button type="button" class="ai-btn" @click="aiGenerate('naming')"><span>✨</span> AI 起名</button>
            </div>
          </div>

          <!-- 类型 + 子类型 -->
          <div class="grid grid-cols-2 gap-4 mb-5">
            <div>
              <label class="block text-sm font-semibold text-[#334155] mb-1.5">作品类型</label>
              <select v-model="form.genre" class="w-full px-3.5 py-2.5 border border-[#e2e8f0] rounded-[10px] text-sm bg-[#fafbfc] focus:border-[#818cf8] outline-none transition-all">
                <option value="">选择类型</option>
                <option v-for="t in templates" :key="t.value" :value="t.value">{{ t.icon }} {{ t.label }}</option>
              </select>
            </div>
            <div class="flex gap-2.5 items-end">
              <div class="flex-1">
                <label class="block text-sm font-semibold text-[#334155] mb-1.5">子类型</label>
                <input v-model="form.subGenre" class="w-full px-3.5 py-2.5 border border-[#e2e8f0] rounded-[10px] text-sm bg-[#fafbfc] focus:border-[#818cf8] focus:bg-white outline-none transition-all" placeholder="如：东方仙侠...">
              </div>
              <button type="button" class="ai-btn-sm" @click="aiGenerate('setting')">✨ 生成设定</button>
            </div>
          </div>

          <!-- 作品简介 + AI润色 -->
          <div class="mb-5">
            <label class="block text-sm font-semibold text-[#334155] mb-1.5">作品简介</label>
            <div class="flex gap-2.5 items-start">
              <textarea v-model="form.description" rows="3" class="flex-1 px-3.5 py-2.5 border border-[#e2e8f0] rounded-[10px] text-sm bg-[#fafbfc] focus:border-[#818cf8] focus:shadow-[0_0_0_3px_rgba(99,102,241,0.12)] focus:bg-white outline-none transition-all resize-y" placeholder="简要描述作品的故事背景和核心看点..."></textarea>
              <button type="button" class="ai-btn mt-0.5" @click="aiGenerate('polish')"><span>✨</span> AI 润色</button>
            </div>
          </div>

          <!-- 详细设定 + AI大纲 -->
          <div class="mb-5">
            <div class="flex items-center justify-between mb-1.5">
              <label class="text-sm font-semibold text-[#334155]">核心设定 <span class="font-normal text-[#94a3b8] text-xs ml-1.5">（选填）</span></label>
              <button type="button" class="ai-btn-sm" @click="aiGenerate('outline')">✨ AI 生成大纲</button>
            </div>
            <textarea v-model="form.coreSetting" rows="4" class="w-full px-3.5 py-2.5 border border-[#e2e8f0] rounded-[10px] text-sm bg-[#fafbfc] focus:border-[#818cf8] focus:shadow-[0_0_0_3px_rgba(99,102,241,0.12)] focus:bg-white outline-none transition-all resize-y" placeholder="描述世界观、力量体系、核心冲突等..."></textarea>
          </div>

          <!-- AI 横向快捷操作 -->
          <div class="flex flex-wrap gap-2.5 mb-5">
            <button type="button" class="ai-action-btn" v-for="act in quickAiActions" :key="act.key" @click="aiGenerate(act.key)">{{ act.icon }} {{ act.label }}</button>
          </div>

          <!-- AI 预览区域 -->
          <div v-if="aiPreview" class="bg-[#f8fafc] border border-[#e2e8f0] rounded-2xl p-5 mb-5 transition-all">
            <div class="flex justify-between items-center mb-2.5 text-xs text-[#94a3b8] font-medium">
              <span>✨ AI 生成结果</span>
              <span>{{ aiPreviewType }}</span>
            </div>
            <div class="text-sm text-[#0f0f1a] leading-relaxed whitespace-pre-wrap">{{ aiPreview }}</div>
            <div class="flex gap-2.5 mt-3 pt-3 border-t border-[#e2e8f0]">
              <button type="button" class="px-4 py-1.5 bg-[#6366f1] text-white rounded-lg text-xs font-medium hover:bg-[#4f46e5] transition-colors" @click="acceptAiPreview">✅ 应用</button>
              <button type="button" class="px-4 py-1.5 bg-[#f1f5f9] text-[#475569] rounded-lg text-xs font-medium hover:bg-[#e2e8f0] transition-colors" @click="aiGenerate(aiPreviewKey)">🔄 重新生成</button>
              <button type="button" class="px-4 py-1.5 text-[#818cf8] border border-[#e2e8f0] rounded-lg text-xs font-medium hover:bg-[#f8fafc] transition-colors" @click="aiPreview = ''">关闭</button>
            </div>
          </div>

          <!-- 目标字数 + 标签 -->
          <div class="grid grid-cols-2 gap-4 mb-6">
            <div>
              <label class="block text-sm font-semibold text-[#334155] mb-1.5">目标字数</label>
              <input v-model.number="form.targetWordCount" type="number" class="w-full px-3.5 py-2.5 border border-[#e2e8f0] rounded-[10px] text-sm bg-[#fafbfc] focus:border-[#818cf8] focus:bg-white outline-none transition-all" placeholder="如：200000">
            </div>
            <div>
              <label class="block text-sm font-semibold text-[#334155] mb-1.5">标签（逗号分隔）</label>
              <input v-model="form.tags" class="w-full px-3.5 py-2.5 border border-[#e2e8f0] rounded-[10px] text-sm bg-[#fafbfc] focus:border-[#818cf8] focus:bg-white outline-none transition-all" placeholder="如：热血,成长,战斗">
            </div>
          </div>

          <!-- 底部按钮 -->
          <div class="flex justify-end gap-3 pt-5 border-t border-[#f1f5f9]">
            <button type="button" @click="closeModal" class="px-6 py-2.5 text-sm font-medium text-[#94a3b8] hover:bg-[#f1f5f9] rounded-[10px] transition-colors">取消</button>
            <button type="submit" class="px-8 py-2.5 bg-[#0f0f1a] text-white rounded-[10px] text-sm font-semibold hover:bg-[#1e1e2f] transition-colors disabled:opacity-50" :disabled="saving || !form.title">
              {{ saving ? '保存中...' : editingId ? '保存修改' : '确认创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import DeleteConfirmDialog from '@/components/common/DeleteConfirmDialog.vue'
import { formatWordCount, formatWan, formatTime, formatNumber, formatRelativeTime } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const store = useNovelStore()
const projects = computed(() => store.projects)
const loading = ref(true)

// 组件挂载时重新拉取数据
onMounted(async () => {
  await store.fetchProjects()
  loading.value = false
})

// keep-alive 激活时也刷新（兜底）
onActivated(async () => {
  loading.value = true
  await store.fetchProjects()
  loading.value = false
})

// 路由变化时重新拉取数据
watch(() => route.path, async (path) => {
  if (path === '/my-works') {
    loading.value = true
    await store.fetchProjects()
    loading.value = false
  }
})

// ─── 搜索与视图 ───
const searchKeyword = ref('')
const viewMode = ref('grid')

// ─── 卡片菜单 ───
const cardMenuTarget = ref(null)
const cardMenuX = ref(0)
const cardMenuY = ref(0)

// ─── 删除确认弹窗 ───
const showDeleteDialog = ref(false)
const workToDelete = ref(null)
const deletingId = ref(null)

// ─── 统计概览 ───
const ongoingCount = computed(() => projects.value.filter(p => !p.status || p.status === '连载中' || p.status === 'ongoing').length)
const completedCount = computed(() => projects.value.filter(p => p.status === '已完成' || p.status === 'completed').length)
const totalWords = computed(() => {
  const total = projects.value.reduce((s, p) => s + (p.wordCount || p.targetWordCount || 0), 0)
  return formatWordCount(total)
})

const filteredProjects = computed(() => {
  if (!searchKeyword.value.trim()) return projects.value
  const kw = searchKeyword.value.trim().toLowerCase()
  return projects.value.filter(p => {
    if (p.title && p.title.toLowerCase().includes(kw)) return true
    const tags = getProjectTags(p)
    if (tags.some(t => t.toLowerCase().includes(kw))) return true
    return false
  })
})

// ─── 辅助方法 ───
const genreEmojis = { '玄幻': '⚔️', '都市': '🏙️', '科幻': '🚀', '悬疑': '🔍', '言情': '💕', '历史': '📜' }
const genreGradients = {
  '玄幻': 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
  '都市': 'linear-gradient(135deg, #1e293b 0%, #334155 50%, #475569 100%)',
  '科幻': 'linear-gradient(135deg, #0c0a3e 0%, #1b1464 50%, #2d1b69 100%)',
  '悬疑': 'linear-gradient(135deg, #1a0a0a 0%, #3d1212 50%, #5c1a1a 100%)',
  '言情': 'linear-gradient(135deg, #3b1d2e 0%, #5c2a4a 50%, #7c3a6a 100%)',
  '历史': 'linear-gradient(135deg, #1a1a0a 0%, #3d3d12 50%, #5c4a1a 100%)'
}

function getProjectEmoji(p) { return genreEmojis[p.genre] || '📖' }
function getCoverGradient(p) { return genreGradients[p.genre] || 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)' }
function getProjectTags(p) {
  const tags = []
  if (p.genre) tags.push(p.genre)
  if (p.subGenre) tags.push(p.subGenre)
  if (p.tags) {
    const t = typeof p.tags === 'string' ? p.tags.split(',') : p.tags
    tags.push(...t.filter(Boolean).map(s => s.trim()))
  }
  return [...new Set(tags)]
}
function getProjectDuration(p) {
  if (!p.createTime) return '-'
  const created = new Date(p.createTime)
  const now = new Date()
  return Math.max(1, Math.ceil((now - created) / (1000 * 60 * 60 * 24 * 30)))
}
function getProgressPercent(p) {
  if (!p.targetWordCount || p.targetWordCount === 0) return 0
  return Math.min(Math.round(((p.wordCount || 0) / p.targetWordCount) * 100), 100)
}
// 使用公共格式化函数
const formatCardNumber = formatNumber
const formatUpdateTime = (p) => {
  const t = p.updateTime || p.createTime
  if (!t) return '—'
  return formatRelativeTime(t)
}

// ─── 表单 ───
const showModal = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({ title: '', description: '', genre: '', subGenre: '', targetWordCount: null, tags: '', coreSetting: '' })

const templates = [
  { icon: '⚔️', label: '玄幻修真', value: '玄幻' },
  { icon: '🏙️', label: '都市异能', value: '都市' },
  { icon: '🚀', label: '科幻末世', value: '科幻' },
  { icon: '🔍', label: '悬疑推理', value: '悬疑' },
  { icon: '💕', label: '言情', value: '言情' },
  { icon: '📜', label: '历史架空', value: '历史' }
]

// ─── 导航 ───
function goToWorkspace(project) {
  store.selectProject(project)
  router.push(`/my-works/${project.id}`)
}

// ─── 弹窗 ───
function openCreateModal() {
  editingId.value = null
  Object.assign(form, { title: '', description: '', genre: '', subGenre: '', targetWordCount: null, tags: '', coreSetting: '' })
  showModal.value = true
}

function editProject(project) {
  editingId.value = project.id
  Object.assign(form, {
    title: project.title || '', description: project.description || '',
    genre: project.genre || '', subGenre: project.subGenre || '',
    targetWordCount: project.targetWordCount || null, tags: project.tags || '',
    coreSetting: project.coreSetting || ''
  })
  showModal.value = true
}

function closeModal() { showModal.value = false; editingId.value = null; aiPreview.value = '' }

async function handleSubmit() {
  if (!form.title.trim() || saving.value) return
  saving.value = true
  try {
    const data = {
      title: form.title.trim(), description: form.description || '',
      genre: form.genre || '', subGenre: form.subGenre || '',
      targetWordCount: form.targetWordCount || null, tags: form.tags || '',
      coreSetting: form.coreSetting || ''
    }
    if (editingId.value) {
      await store.updateProject(editingId.value, data)
    } else {
      await store.createProject(data)
    }
    projects.value = store.projects
    closeModal()
  } catch (e) {
    alert('操作失败：' + (e.message || '请稍后重试'))
  } finally {
    saving.value = false
  }
}

function confirmDelete(project) {
  workToDelete.value = project
  showDeleteDialog.value = true
}

function cancelDelete() {
  showDeleteDialog.value = false
  workToDelete.value = null
}

async function handleDeleteConfirm() {
  if (!workToDelete.value) return
  const projectId = workToDelete.value.id
  deletingId.value = projectId
  try {
    await store.deleteProject(projectId)
    projects.value = store.projects
    showDeleteDialog.value = false
    workToDelete.value = null
  } catch (e) {
    alert('删除失败：' + (e.message || '请稍后重试'))
  } finally {
    deletingId.value = null
  }
}

async function handleDelete(project) {
  workToDelete.value = project
  showDeleteDialog.value = true
}

function openCardMenu(project, event) {
  cardMenuTarget.value = project
  cardMenuX.value = event.clientX - 140
  cardMenuY.value = event.clientY + 8
}

// ─── 视图 ───
function toggleSort() { /* 排序切换 */ }
function toggleView() { viewMode.value = viewMode.value === 'grid' ? 'list' : 'grid' }

// ─── AI 生成功能 ───
const aiPreview = ref('')
const aiPreviewType = ref('')
const aiPreviewKey = ref('')

const quickAiActions = [
  { key: 'naming', icon: '📛', label: 'AI 起名' },
  { key: 'polish', icon: '✨', label: 'AI 润色' },
  { key: 'setting', icon: '🌍', label: 'AI 生成设定' },
  { key: 'outline', icon: '📋', label: 'AI 生成大纲' }
]

const aiTemplates = {
  naming: `你是一位资深小说编辑。请根据以下信息生成10个吸引人的作品名。\n\n【作品类型】：${form.genre || '未指定'}\n【简介】：${form.description || '未填写'}\n【核心设定】：${form.coreSetting || '未填写'}\n\n要求：每个名字≤7字，包含传统/网文/文艺/悬念/创新风格，附带简短解读。`,
  polish: `你是一位小说营销编辑。请润色以下简介，提供3个版本（悬念优先/情绪优先/设定优先），每版50-120字。\n\n【原始简介】：${form.description || '未填写'}\n【作品类型】：${form.genre || '未指定'}`,
  setting: `你是一位世界架构师。请根据以下信息生成完整世界观：背景/力量体系/势力/冲突/主角定位/故事基调。\n\n【作品名】：${form.title || '未命名'}\n【类型】：${form.genre || '未指定'}\n【简介】：${form.description || '未填写'}`,
  outline: `你是一位小说大纲规划师。请按"起承转合"四幕结构生成完整大纲。\n\n【作品名】：${form.title || '未命名'}\n【类型】：${form.genre || '未指定'}\n【设定】：${form.coreSetting || form.description || '未填写'}\n\n输出：第一幕(起)、第二幕(承)、第三幕(转)、第四幕(合)，每幕包含：场景/盟友/挑战/成长。`
}

const aiDemoResults = {
  naming: `1. 《星辰剑诀》—— 传统风格：古典仙侠韵味，突出剑道传承主线\n2. 《苍穹之下》—— 文艺风格：意境深远，暗示主角的渺小与伟大\n3. 《万古第一神》—— 网文风格：霸气直接，符合玄幻读者期待\n4. 《帝国崩塌时》—— 悬念风格：制造时间紧迫感\n5. 《剑道独尊》—— 传统风格：简洁有力，突出唯一性\n6. 《虚空彼岸》—— 创新风格：科幻与玄幻融合\n7. 《永夜君王》—— 网文风格：暗黑史诗感\n8. 《她自深渊来》—— 文艺风格：第一人称代入感强\n9. 《谁的帝国？》—— 悬念风格：问题式标题引发好奇\n10. 《星辉纪元》—— 创新风格：宏大叙事感`,
  polish: `版本A（悬念优先）：\n当帝国的最后一位剑圣在刑场上睁开眼睛，所有人都以为他死了——三年。这是一个关于复仇的故事，也是一个关于选择的谜题。\n\n版本B（情绪优先）：\n他曾是帝国最耀眼的星辰，如今是阴沟里最卑微的乞丐。但当他再次握紧那把剑时，整个大陆都将为之颤抖。\n\n版本C（设定优先）：\n在一个以剑为尊的世界，星辰剑诀是万古第一神功。少年楚云帆偶然获得传承，从此踏上了一条与整个修真界为敌的道路。`,
  setting: `一、世界背景\n时代：灵气复苏后三千年，修真文明鼎盛\n地理：九州大陆，中央为人类帝国，四方为异族领地\n社会：以宗门为核心的修真等级社会\n\n二、核心力量体系\n来源：天地灵气 + 星辰之力\n等级：练气→筑基→金丹→元婴→化神→合体→大乘\n禁忌：星辰剑诀（传说修至大成可斩星辰）\n\n三、主要势力\nA-星辰剑宗：守护剑诀传承，中立势力\nB-帝国皇室：掌控资源分配，维护统治\nC-暗影组织：收集剑诀碎片，目的不明\n\n四、核心冲突\n表层：剑诀争夺战\n深层：修真文明与星辰本源存亡\n\n五、主角定位\n起点：寒门少年，身世成谜\n优势：天生星辰体质\n路径：从被追杀到守护者\n\n六、基调：热血成长 + 史诗权谋`,
  outline: `第一幕：起（1-30%）\n开篇：少年楚云帆在山中偶得星辰剑诀残卷\n日常：在剑宗外门修炼，受人欺凌\n催化：帝国暗卫来袭，师父以命相护\n决定：踏上寻找完整剑诀之路\n\n第二幕：承（31-60%）\n适应：从外门弟子到独行剑客的转变\n盟友：结识女剑客柳如烟、神秘老者\n挑战：第一次正面对抗帝国暗卫小队\n成长：初步掌握星辰剑意，实力跃升\n\n第三幕：转（61-85%）\n挫折：柳如烟身世揭露，两人产生裂痕\n真相：剑诀关系世界存亡，主角是"钥匙"\n动摇：面对真相，是否值得牺牲一切\n振作：接受命运，决心守护所爱\n\n第四幕：合（86-100%）\n决战：帝国皇宫之巅，星辰对决\n战场：星空为幕，剑气纵横三万里\n结局：建立新秩序，守护者而非统治者\n收束：星辰永恒，守护不息`
}

function aiGenerate(key) {
  aiPreviewKey.value = key
  const types = { naming: 'AI 起名', polish: 'AI 润色', setting: 'AI 生成设定', outline: 'AI 生成大纲' }
  aiPreviewType.value = types[key] || 'AI 生成'
  aiPreview.value = '⏳ AI 正在生成中...'
  setTimeout(() => { aiPreview.value = aiDemoResults[key] || '生成完成，请查看结果。' }, 800)
}

function acceptAiPreview() {
  const key = aiPreviewKey.value
  if (key === 'naming') form.title = aiPreview.value.split('\n')[0].replace(/^\d+\.\s*《([^》]+)》.*/, '$1')
  else if (key === 'polish') form.description = aiPreview.value.replace(/版本[ABC].*?\n/g, '').trim().split('\n')[0]
  else if (key === 'setting') form.coreSetting = aiPreview.value
  aiPreview.value = ''
}
</script>

<style scoped>
/* ══════════ 页面容器 ══════════ */
.my-works-page {
  padding: 24px 32px 40px;
  max-width: 1200px;
  margin: 0 auto;
  background: #faf8f5;
  min-height: 100vh;
  font-family: var(--font-body);
}

/* ══════════ 顶部标题栏 ══════════ */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0ece6;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: -0.3px;
  margin: 0;
}

.header-subtitle {
  font-size: 14px;
  color: #a8a4a0;
  font-weight: 400;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-new {
  background: #1a1a2e;
  color: #ffffff;
  border: none;
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 2px 12px rgba(26, 26, 46, 0.15);
}

.btn-new:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(26, 26, 46, 0.25);
  background: #2a2a4e;
}

.btn-new:active { transform: translateY(0); }
.btn-icon { font-size: 18px; font-weight: 300; }

.btn-setup {
  background: linear-gradient(135deg, #818cf8, #6366f1);
  color: #fff; border: none;
  padding: 10px 22px; border-radius: 10px;
  font-size: 14px; font-weight: 600; text-decoration: none;
  cursor: pointer; transition: all 0.25s ease;
  display: flex; align-items: center; gap: 6px;
  box-shadow: 0 2px 12px rgba(99,102,241,0.2);
}
.btn-setup:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(99,102,241,0.35);
}

/* ══════════ 统计卡片区 ══════════ */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 18px 22px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0ece6;
  transition: all 0.25s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  font-size: 28px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-card-total .stat-icon { background: #f0f0ff; }
.stat-card-ongoing .stat-icon { background: #f0fdf4; }
.stat-card-done .stat-icon { background: #ecfdf5; }
.stat-card-words .stat-icon { background: #fff7ed; }

.stat-content { flex: 1; }

.stat-number {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  display: block;
  line-height: 1.2;
}

.stat-unit { font-size: 14px; font-weight: 500; color: #a8a4a0; }
.stat-label { font-size: 13px; color: #a8a4a0; font-weight: 400; }

.stat-trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
  flex-shrink: 0;
  color: #a8a4a0;
  background: #f5f2ed;
}

.stat-trend.up { color: #10b981; background: #ecfdf5; }

/* ══════════ 作品列表工具栏 ══════════ */
.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left { display: flex; align-items: baseline; gap: 12px; }
.list-title { font-size: 16px; font-weight: 600; color: #1a1a2e; }
.list-count { font-size: 13px; color: #a8a4a0; }

.toolbar-right { display: flex; align-items: center; gap: 10px; }

.search-box {
  display: flex;
  align-items: center;
  background: #ffffff;
  border: 1px solid #f0ece6;
  border-radius: 10px;
  padding: 0 12px;
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: #1a1a2e;
  box-shadow: 0 0 0 3px rgba(26, 26, 46, 0.06);
}

.search-icon { font-size: 14px; color: #c8c4c0; margin-right: 8px; }

.search-input {
  border: none; outline: none; background: transparent;
  padding: 8px 0; font-size: 13px; color: #1a1a2e;
  width: 160px; transition: width 0.2s;
}

.search-input:focus { width: 200px; }
.search-input::placeholder { color: #c8c4c0; }

.btn-sort, .btn-view {
  background: #ffffff; border: 1px solid #f0ece6;
  border-radius: 10px; padding: 8px 14px;
  font-size: 13px; color: #6b6560;
  cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; gap: 4px;
}

.btn-sort:hover, .btn-view:hover {
  background: #f5f2ed; border-color: #d8d0c8;
}

/* ══════════ 作品卡片 ══════════ */
.works-grid { display: grid; gap: 14px; }
.works-grid.grid-view { grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); }
.works-grid.list-view { grid-template-columns: 1fr; }

.work-card {
  background: #ffffff; border-radius: 12px; overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  border: 1px solid #f0ece6;
  cursor: pointer; transition: all 0.25s ease;
}

.work-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
  border-color: #d8d0c8;
}

.card-cover {
  height: 56px; position: relative;
  display: flex; align-items: center; justify-content: center;
}

.cover-badge {
  position: absolute; top: 5px; left: 5px;
  background: rgba(255, 255, 255, 0.2); backdrop-filter: blur(4px);
  color: #fff; font-size: 10px; font-weight: 600;
  padding: 2px 8px; border-radius: 16px;
}

.cover-badge.done { background: rgba(16, 185, 129, 0.3); }
.cover-badge.draft { background: rgba(168, 164, 160, 0.3); }

.cover-emoji {
  font-size: 24px; filter: drop-shadow(0 2px 3px rgba(0,0,0,0.25));
}

/* ── 悬停删除按钮 ── */
.card-delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  opacity: 0;
  transform: scale(0.85);
  transition: all 0.2s ease;
  z-index: 5;
}

.card-cover:hover .card-delete-btn {
  opacity: 1;
  transform: scale(1);
}

.card-delete-btn:hover {
  background: rgba(190, 18, 60, 0.85);
  border-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 2px 12px rgba(190, 18, 60, 0.4);
}

.card-delete-btn:disabled {
  opacity: 0.5;
  cursor: wait;
}

.delete-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.card-body { padding: 10px 14px 12px; }

.card-header-row {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 5px;
}

.card-title {
  font-size: 14px; font-weight: 700; color: #1a1a2e;
  margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.card-menu {
  background: none; border: none; font-size: 16px;
  color: #c8c4c0; cursor: pointer; padding: 1px 4px;
  border-radius: 4px; transition: all 0.2s;
}

.card-menu:hover { background: #f0ece6; color: #6b6560; }

.card-tags { display: flex; gap: 4px; margin-bottom: 6px; flex-wrap: wrap; }

.tag {
  font-size: 10px; font-weight: 500; color: #6b6560;
  background: #f5f2ed; padding: 1px 7px; border-radius: 4px;
}

/* ── 简介 ── */
.card-desc {
  font-size: 12px; color: #6b6560; line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-stats {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 10px;
}

.stat-item { display: flex; align-items: baseline; gap: 2px; }
.stat-value { font-size: 13px; font-weight: 700; color: #1a1a2e; }
.stat-unit { font-size: 11px; color: #a8a4a0; }
.stat-divider { color: #e0dcd6; }

.card-progress { margin-bottom: 10px; }

.progress-header {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 4px;
}

.progress-label { font-size: 11px; color: #a8a4a0; }
.progress-percent { font-size: 12px; font-weight: 700; color: #1a1a2e; }

.progress-bar {
  height: 4px; background: #f0ece6; border-radius: 2px; overflow: hidden;
}

.progress-fill {
  height: 100%; border-radius: 2px;
  background: linear-gradient(90deg, #1a1a2e 0%, #3a3a5e 100%);
  transition: width 0.6s ease;
}

.card-footer {
  display: flex; justify-content: space-between;
  align-items: center; padding-top: 8px;
  border-top: 1px solid #f0ece6;
}

.card-time { font-size: 11px; color: #a8a4a0; }

.btn-continue {
  background: none; border: none; font-size: 12px;
  font-weight: 600; color: #1a1a2e; cursor: pointer;
  transition: all 0.2s; padding: 2px 0;
}

.btn-continue:hover { color: #6366f1; }

/* ══════════ 空状态 ══════════ */
.empty-state {
  text-align: center; padding: 60px 20px;
}

.empty-icon { font-size: 56px; margin-bottom: 16px; }
.empty-state h3 { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px; }
.empty-state p { font-size: 14px; color: #a8a4a0; margin: 0 0 24px; }

.btn-new-large {
  background: #1a1a2e; color: #fff; border: none;
  padding: 14px 32px; border-radius: 12px;
  font-size: 16px; font-weight: 600; cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 12px rgba(26, 26, 46, 0.15);
}

.btn-new-large:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(26, 26, 46, 0.25);
  background: #2a2a4e;
}

/* ══════════ AI 按钮 ══════════ */
.ai-btn {
  background: linear-gradient(135deg, #818cf8, #6366f1);
  color: #fff; border: none;
  padding: 7px 16px; border-radius: 10px;
  font-size: 13px; font-weight: 600;
  cursor: pointer; white-space: nowrap;
  display: flex; align-items: center; gap: 4px;
  transition: transform 0.15s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(99,102,241,0.2);
  flex-shrink: 0;
}

.ai-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(99,102,241,0.3); }

.ai-btn-sm {
  background: #f1f5f9; color: #334155; border: none;
  padding: 5px 12px; border-radius: 8px;
  font-size: 12px; font-weight: 500;
  cursor: pointer; white-space: nowrap;
  transition: background 0.2s; flex-shrink: 0;
}

.ai-btn-sm:hover { background: #e2e8f0; }

.ai-action-btn {
  background: #f1f5f9; color: #334155; border: none;
  padding: 6px 15px; border-radius: 10px;
  font-size: 13px; font-weight: 500;
  cursor: pointer; white-space: nowrap;
  transition: background 0.2s;
}

.ai-action-btn:hover { background: #e2e8f0; }

@keyframes modalIn {
  from { opacity: 0; transform: translateY(18px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
</style>
