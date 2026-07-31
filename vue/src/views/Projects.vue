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

        <form @submit.prevent="handleSubmit" class="create-form">
          <!-- 作品名称 -->
          <div class="form-row">
            <label class="form-label">作品名称 <span class="required">*</span></label>
            <div class="input-group">
              <input v-model="form.title" class="form-input" placeholder="输入作品名称..." required>
              <button type="button" class="btn-ai" :disabled="aiLoading === 'naming'" @click="aiGenerate('naming')">
                <span v-if="aiLoading === 'naming'" class="spinner"></span>
                <span v-else-if="aiGenerated === 'naming'" class="check">✓</span>
                <span v-else>✨</span>
                <span class="btn-text">{{ aiGenerated === 'naming' ? '已生成' : 'AI 起名' }}</span>
              </button>
            </div>
          </div>

          <!-- 作品类型 -->
          <div class="form-row">
            <label class="form-label">作品类型</label>
            <div class="input-group">
              <select v-model="form.genre" class="form-select">
                <option value="">选择类型</option>
                <option v-for="t in templates" :key="t.value" :value="t.value">{{ t.icon }} {{ t.label }}</option>
              </select>
            </div>
          </div>

          <!-- 作品简介 -->
          <div class="form-row">
            <label class="form-label">作品简介</label>
            <div class="input-group">
              <textarea v-model="form.description" class="form-textarea" rows="3" placeholder="简要描述作品的故事背景和核心看点..."></textarea>
              <button type="button" class="btn-ai btn-ai-right" :disabled="aiLoading === 'polish'" @click="aiGenerate('polish')">
                <span v-if="aiLoading === 'polish'" class="spinner"></span>
                <span v-else-if="aiGenerated === 'polish'" class="check">✓</span>
                <span v-else>✨</span>
                <span class="btn-text">{{ aiGenerated === 'polish' ? '已润色' : 'AI 润色' }}</span>
              </button>
            </div>
          </div>

          <!-- 核心设定 + AI生成设定 -->
          <div class="form-row">
            <label class="form-label">核心设定 <span class="optional">（选填）</span></label>
            <div class="input-group">
              <textarea v-model="form.coreSetting" class="form-textarea" rows="4" placeholder="描述世界观、力量体系、核心冲突等..."></textarea>
              <button type="button" class="btn-ai btn-ai-right" :disabled="aiLoading === 'setting'" @click="aiGenerate('setting')">
                <span v-if="aiLoading === 'setting'" class="spinner"></span>
                <span v-else-if="aiGenerated === 'setting'" class="check">✓</span>
                <span v-else>✨</span>
                <span class="btn-text">{{ aiGenerated === 'setting' ? '已生成' : '生成设定' }}</span>
              </button>
            </div>
          </div>

          <!-- 生成的大纲预览 + AI生成大纲 -->
          <div class="form-row">
            <label class="form-label">生成的大纲预览</label>
            <div class="input-group">
              <div class="outline-preview" :class="{ 'has-content': outlineResult }">
                <div v-if="outlineResult" class="outline-content">{{ outlineResult }}</div>
                <div v-else class="outline-placeholder">
                  <span class="placeholder-icon">📋</span>
                  <span>点击「AI 生成大纲」后，大纲内容将显示在这里</span>
                </div>
              </div>
              <button type="button" class="btn-ai btn-ai-right" :disabled="aiLoading === 'outline'" @click="aiGenerate('outline')">
                <span v-if="aiLoading === 'outline'" class="spinner"></span>
                <span v-else-if="aiGenerated === 'outline'" class="check">✓</span>
                <span v-else>✨</span>
                <span class="btn-text">{{ aiGenerated === 'outline' ? '已生成' : 'AI 生成大纲' }}</span>
              </button>
            </div>
          </div>

          <!-- 目标字数 + 标签 -->
          <div class="form-row form-row-grid">
            <div class="form-cell">
              <label class="form-label">目标字数</label>
              <input v-model.number="form.targetWordCount" type="number" class="form-input" placeholder="如：200000">
            </div>
            <div class="form-cell">
              <label class="form-label">标签（逗号分隔）</label>
              <input v-model="form.tags" class="form-input" placeholder="如：热血,成长,战斗">
            </div>
          </div>

          <!-- 底部按钮 -->
          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="closeModal">取消</button>
            <button type="submit" class="btn-submit" :disabled="saving || !form.title">
              <span v-if="saving" class="spinner"></span>
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

function closeModal() { showModal.value = false; editingId.value = null; aiLoading.value = '' }

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
const aiLoading = ref('')
const aiGenerated = ref('')
const outlineResult = ref('')

function aiGenerate(key) {
  const types = { naming: 'AI 起名', polish: 'AI 润色', setting: 'AI 生成设定', outline: 'AI 生成大纲' }
  aiLoading.value = key
  aiGenerated.value = ''
  setTimeout(() => {
    let result = ''
    if (key === 'naming') {
      const names = ['星辰剑诀', '苍穹之下', '万古第一神', '帝国崩塌时', '剑道独尊', '虚空彼岸', '永夜君王']
      result = names[Math.floor(Math.random() * names.length)]
      form.title = result
    } else if (key === 'polish') {
      result = '当帝国的最后一位剑圣在刑场上睁开眼睛，所有人都以为他死了——三年。'
      form.description = result
    } else if (key === 'setting') {
      result = `一、世界背景\n时代：灵气复苏后三千年\n地理：九州大陆，中央为人类帝国\n\n二、核心力量体系\n等级：练气→筑基→金丹→元婴→化神`
      form.coreSetting = result
    } else if (key === 'outline') {
      result = `第一幕（起）：少年楚云帆在山中偶得星辰剑诀残卷，踏上修行之路。\n\n第二幕（承）：结识女剑客柳如烟、神秘老者，经历第一次正面对抗。\n\n第三幕（转）：真相揭露，剑诀关系世界存亡，接受命运守护所爱。\n\n第四幕（合）：帝国皇宫之巅决战，建立新秩序。`
      form.coreSetting = result
      outlineResult.value = result
    }
    aiLoading.value = ''
    if (result) aiGenerated.value = key
    setTimeout(() => { aiGenerated.value = '' }, 1500)
  }, 600)
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

/* ══════════ 新建作品表单样式 ══════════ */
.create-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.form-label .required {
  color: #ef4444;
  margin-left: 2px;
}

.form-label .optional {
  font-weight: 400;
  color: #94a3b8;
  font-size: 12px;
}

.input-group {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.form-input,
.form-select,
.form-textarea {
  flex: 1;
  padding: 10px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  background: #fafbfc;
  outline: none;
  transition: all 0.2s;
  font-family: inherit;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: #818cf8;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #cbd5e1;
}

.form-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

/* ─── AI 按钮（统一尺寸）─── */
.btn-ai {
  padding: 10px 18px;
  background: linear-gradient(135deg, #818cf8, #6366f1);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
  flex-shrink: 0;
  height: 42px;
}

.btn-ai:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.35);
}

.btn-ai:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-ai.btn-ai-right {
  margin-top: 2px;
  align-self: flex-start;
}

.btn-ai .spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.btn-ai .check {
  font-size: 14px;
}

.btn-ai .btn-text {
  font-size: 14px;
}

/* ─── 大纲展示区域 ─── */
.outline-preview {
  flex: 1;
  border: 1.5px dashed #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  min-height: 120px;
  background: #fafbfc;
  transition: all 0.3s;
}

.outline-preview.has-content {
  border-style: solid;
  border-color: #c7d2fe;
  background: #f8faff;
}

.outline-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
  min-height: 80px;
}

.placeholder-icon {
  font-size: 28px;
  opacity: 0.5;
}

.outline-content {
  font-size: 13px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ─── 底部操作按钮 ─── */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.btn-cancel {
  padding: 12px 24px;
  background: transparent;
  color: #64748b;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.btn-submit {
  padding: 12px 32px;
  background: #1a1a2e;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-submit:hover:not(:disabled) {
  background: #2a2a4e;
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-submit .spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* ─── 响应式 ─── */
@media (max-width: 600px) {
  .form-row-grid {
    grid-template-columns: 1fr;
  }

  .input-group {
    flex-direction: column;
  }

  .btn-ai {
    width: 100%;
    justify-content: center;
  }

  .btn-ai.btn-ai-right {
    align-self: stretch;
  }

  .form-actions {
    flex-direction: column;
  }

  .btn-cancel,
  .btn-submit {
    width: 100%;
    justify-content: center;
  }
}
</style>