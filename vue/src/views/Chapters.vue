<template>
  <div class="chapters">
    <!-- ==================== 头部 ==================== -->
    <div class="section-header">
      <div>
        <h2 class="section-title">📑 章节管理</h2>
        <p class="section-subtitle">
          {{ project?.title || '未命名作品' }} ·
          {{ chapters.length }} 章 ·
          {{ formatNumber(totalWords) }} 字
        </p>
      </div>
      <div class="header-actions">
        <BaseButton size="sm" @click="openCreateDialog">＋ 新建章节</BaseButton>
      </div>
    </div>

    <!-- ==================== 统计卡片 ==================== -->
    <div class="stats-grid">
      <StatCard label="总章节" :value="chapters.length" color="accent" />
      <StatCard
        label="已完成"
        :value="publishedCount"
        :change="chapters.length > 0 ? ((publishedCount / chapters.length) * 100).toFixed(1) + '%' : '0%'"
        color="teal"
      />
      <StatCard label="进行中" :value="draftCount" subtext="草稿" color="accent" />
      <StatCard
        label="总字数"
        :value="formatNumber(totalWords)"
        subtext="平均字数"
        color="emerald"
      />
    </div>

    <!-- ==================== 筛选栏 ==================== -->
    <div class="filter-bar">
      <input
        v-model="searchKeyword"
        class="search-input"
        placeholder="🔍 搜索章节标题或内容..."
      />
      <select v-model="filterStatus" class="filter-select">
        <option value="all">全部状态</option>
        <option value="draft">草稿</option>
        <option value="published">已发布</option>
        <option value="completed">已完成</option>
      </select>
      <div class="filter-tabs">
        <span
          class="filter-tab"
          :class="{ active: sortBy === 'updatedAt' }"
          @click="sortBy = 'updatedAt'"
        >最近更新</span>
        <span
          class="filter-tab"
          :class="{ active: sortBy === 'wordCount' }"
          @click="sortBy = 'wordCount'"
        >字数</span>
        <span
          class="filter-tab"
          :class="{ active: sortBy === 'sortOrder' }"
          @click="sortBy = 'sortOrder'"
        >章节序号</span>
        <button
          class="sort-direction-btn"
          @click="toggleSortDirection"
          :title="sortOrder === 'desc' ? '点击切换为正序' : '点击切换为倒序'"
        >
          {{ sortOrder === 'desc' ? '↓' : '↑' }} {{ sortOrder === 'desc' ? '倒序' : '正序' }}
        </button>
      </div>
    </div>

    <!-- ==================== 章节列表 ==================== -->
    <div class="chapters-card">
      <template v-if="filteredAndSortedChapters.length === 0">
        <div class="empty-state">
          <span class="empty-icon">📭</span>
          <p>暂无章节，点击「新建章节」开始创作</p>
        </div>
      </template>

      <!-- 章节表格 -->
      <table class="chapters-table">
        <thead>
          <tr>
            <th style="width:60px;">章节</th>
            <th style="min-width:140px;">标题</th>
            <th style="min-width:200px;">内容预览</th>
            <th style="width:80px;">字数</th>
            <th style="width:80px;">状态</th>
            <th style="width:100px;">更新时间</th>
            <th style="width:120px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="ch in filteredAndSortedChapters"
            :key="ch.id"
              class="chapter-row"
              :class="{ 'current-row': isCurrentChapter(ch) }"
              @click.self="viewChapter(ch)"
            >
              <td class="chapter-num">
                {{ getChapterNumber(ch) }}
              </td>
              <td class="chapter-title">
                {{ ch.title || '未命名章节' }}
                <span v-if="isCurrentChapter(ch)" class="current-badge">← 当前</span>
              </td>
              <td class="chapter-preview">
                <span v-if="ch.content" class="preview-text">
                  {{ truncateContent(ch.content, 60) }}
                </span>
                <span v-else class="preview-empty">（暂无内容）</span>
              </td>
              <td class="chapter-words">{{ formatNumber(ch.wordCount || 0) }}</td>
              <td>
                <BaseTag :color="getStatusColor(ch.status)">
                  {{ getStatusLabel(ch.status) }}
                </BaseTag>
              </td>
              <td class="update-time">{{ formatTime(ch.updateTime) }}</td>
              <td class="chapter-actions">
                <BaseButton
                  variant="outline"
                  size="sm"
                  @click.stop="viewChapter(ch)"
                  title="查看/编辑内容"
                >📖 查看</BaseButton>
                <BaseButton
                  variant="outline"
                  size="sm"
                  @click.stop="toggleStatus(ch)"
                  :title="ch.status === 'published' ? '转为草稿' : '发布章节'"
                >{{ ch.status === 'published' ? '📥 草稿' : '📤 发布' }}</BaseButton>
                <BaseButton
                  variant="outline"
                  size="sm"
                  @click.stop="editChapter(ch)"
                  title="编辑章节"
                >✏️ 编辑</BaseButton>
                <BaseButton
                  variant="outline"
                  size="sm"
                  @click.stop="deleteChapter(ch)"
                  title="删除章节"
                  style="color: var(--rose);"
                >🗑️ 删除</BaseButton>
              </td>
            </tr>
          </tbody>
        </table>
    </div>

    <!-- ==================== 新建/编辑章节弹窗 ==================== -->
    <ChapterDialog
      v-if="dialogVisible"
      :mode="dialogMode"
      :chapter="editingChapter"
      :project-id="project?.id"
      @close="dialogVisible = false"
      @saved="onChapterSaved"
    />

    <!-- ==================== 查看内容弹窗 ==================== -->
    <ContentPreviewDialog
      v-if="previewVisible"
      :chapter="previewChapter"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseTag from '@/components/common/BaseTag.vue'
import StatCard from '@/components/common/StatCard.vue'
import ChapterDialog from '@/components/chapter/ChapterDialog.vue'
import ContentPreviewDialog from '@/components/chapter/ContentPreviewDialog.vue'
import { useNovelStore } from '@/stores/novel'
import { useRouter } from 'vue-router'

const store = useNovelStore()
const router = useRouter()

// ─── 状态 ───
const searchKeyword = ref('')
const filterStatus = ref('all')
const sortBy = ref('updatedAt')
const sortOrder = ref('desc')
const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingChapter = ref(null)
const previewVisible = ref(false)
const previewChapter = ref(null)

// ─── 数据 ───
const project = computed(() => store.currentProject)
const chapters = computed(() => store.chapters || [])

// ─── 计算统计 ───
const publishedCount = computed(() =>
  chapters.value.filter(c => c.status === 'published' || c.status === 'completed').length
)
const draftCount = computed(() =>
  chapters.value.filter(c => c.status === 'draft' || c.status === 'writing').length
)
const totalWords = computed(() =>
  chapters.value.reduce((sum, c) => sum + (c.wordCount || 0), 0)
)

// ─── 过滤与排序 ───
const filteredAndSortedChapters = computed(() => {
  let list = [...chapters.value]

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(c =>
      c.title?.toLowerCase().includes(keyword) ||
      c.content?.toLowerCase().includes(keyword)
    )
  }

  if (filterStatus.value !== 'all') {
    list = list.filter(c => c.status === filterStatus.value)
  }

  const sortMap = {
    updatedAt: (a, b) => {
      const valA = new Date(a.updateTime || 0)
      const valB = new Date(b.updateTime || 0)
      return sortOrder.value === 'desc' ? valA - valB : valB - valA
    },
    wordCount: (a, b) => {
      const valA = a.wordCount || 0
      const valB = b.wordCount || 0
      return sortOrder.value === 'desc' ? valA - valB : valB - valA
    },
    sortOrder: (a, b) => {
      const valA = a.sortOrder ?? 0
      const valB = b.sortOrder ?? 0
      return sortOrder.value === 'desc' ? valA - valB : valB - valA
    }
  }
  list.sort(sortMap[sortBy.value] || sortMap.updatedAt)

  return list
})

// ─── 排序方向切换 ───
function toggleSortDirection() {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
}

// ─── 辅助函数 ───
function formatNumber(n) {
  if (n == null) return '0'
  return n.toLocaleString()
}

function formatTime(time) {
  if (!time) return '--'
  const d = new Date(time)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return d.toLocaleDateString('zh-CN')
}

function truncateContent(text, maxLen = 60) {
  if (!text) return ''
  const plain = text.replace(/\s+/g, ' ')
  return plain.length > maxLen ? plain.slice(0, maxLen) + '...' : plain
}

function getChapterNumber(ch) {
  return `CH.${ch.sortOrder ?? ch.id}`
}

// 状态值与后端 NovelChapter.status 保持一致：draft(草稿)/writing(写作中)/published(已发布)/completed(已完成)
function getStatusLabel(status) {
  const map = { draft: '草稿', writing: '写作中', published: '已发布', completed: '已完成' }
  return map[status] || status || '草稿'
}

function getStatusColor(status) {
  const map = { draft: 'amber', writing: 'accent', published: 'emerald', completed: 'emerald' }
  return map[status] || 'amber'
}

function isCurrentChapter(ch) {
  return store.currentChapterId === ch.id
}

// ─── 交互方法 ───
function openCreateDialog() {
  dialogMode.value = 'create'
  editingChapter.value = null
  dialogVisible.value = true
  console.log('[Chapters] 打开新建章节弹窗')
}

function editChapter(ch) {
  if (!ch) {
    console.warn('[Chapters] 编辑失败：章节数据为空')
    return
  }
  console.log('[Chapters] 编辑章节:', ch.id, ch.title)
  dialogMode.value = 'edit'
  editingChapter.value = ch
  dialogVisible.value = true
}

function viewChapter(ch) {
  if (!ch) {
    console.warn('[Chapters] 查看失败：章节数据为空')
    return
  }
  console.log('[Chapters] 查看章节:', ch.id, ch.title)
  previewChapter.value = ch
  previewVisible.value = true
}

async function deleteChapter(ch) {
  if (!ch) {
    console.warn('[Chapters] 删除失败：章节数据为空')
    return
  }
  console.log('[Chapters] 删除章节:', ch.id, ch.title)
  if (!confirm(`确定要删除「${ch.title || '未命名章节'}」吗？\n此操作不可恢复！`)) return
  try {
    await store.deleteChapter(project.value?.id, ch.id)
    console.log('[Chapters] 章节已删除:', ch.title)
  } catch (e) {
    console.error('[Chapters] 删除失败：', e.message)
    alert('删除失败：' + e.message)
  }
}

async function toggleStatus(ch) {
  if (!ch) {
    console.warn('[Chapters] 切换状态失败：章节数据为空')
    return
  }
  console.log('[Chapters] 切换状态:', ch.id, ch.title, '当前状态:', ch.status)
  try {
    await store.toggleChapterStatus(project.value?.id, ch.id)
    const newStatus = ch.status === 'published' ? 'draft' : 'published'
    // 立即刷新章节列表确保状态同步
    await store.fetchChapters(project.value?.id)
    console.log('[Chapters] 章节状态已切换为：', newStatus)
  } catch (e) {
    console.error('[Chapters] 状态切换失败：', e.message)
    alert('状态切换失败：' + e.message)
  }
}

function onChapterSaved() {
  dialogVisible.value = false
  if (project.value?.id) {
    store.fetchChapters(project.value.id)
  }
}

// ─── 生命周期 ───
onMounted(() => {
  if (project.value?.id) {
    store.fetchChapters(project.value.id)
  }
})

watch(() => project.value?.id, (newId) => {
  if (newId) {
    store.fetchChapters(newId)
  }
})
</script>

<style scoped>
/* ============================================================
   整体布局
   ============================================================ */
.chapters {
  animation: fadeSlideIn 0.4s ease;
  padding: 4px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 1.5rem;
}

.section-title {
  font-family: var(--font-display);
  font-size: 1.6rem;
  font-weight: 700;
}

.section-subtitle {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-top: 0.3rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* ─── 统计卡片 ─── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

/* ─── 筛选栏 ─── */
.filter-bar {
  display: flex;
  gap: 0.8rem;
  margin-bottom: 1.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 180px;
  max-width: 320px;
  padding: 0.5rem 0.8rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 0.78rem;
  background: #faf8f5;
  font-family: var(--font-body);
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: var(--accent);
}

.filter-select {
  padding: 0.5rem 0.8rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 0.75rem;
  background: var(--card);
  color: var(--text-secondary);
  cursor: pointer;
  outline: none;
}

.filter-tabs {
  display: flex;
  gap: 0.4rem;
  margin-left: auto;
}

.filter-tab {
  padding: 0.3rem 0.8rem;
  border-radius: 20px;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-secondary);
  transition: all 0.15s ease;
}

.filter-tab:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.filter-tab.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.sort-direction-btn {
  padding: 0.3rem 0.8rem;
  border-radius: 20px;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-secondary);
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: 4px;
}

.sort-direction-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

/* ─── 章节卡片 ─── */
.chapters-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 1rem;
}

/* ─── 章节表格 ─── */
.chapters-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.82rem;
}

.chapters-table th {
  text-align: left;
  padding: 0.6rem 1rem;
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  background: #faf8f5;
  border-bottom: 1px solid var(--border);
  font-weight: 600;
}

.chapters-table td {
  padding: 0.6rem 1rem;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

.chapter-row {
  cursor: pointer;
  transition: background 0.15s ease;
}

.chapter-row:hover {
  background: rgba(0, 0, 0, 0.02);
}

.chapter-num {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.chapter-title {
  font-weight: 500;
}

.current-badge {
  font-size: 0.6rem;
  color: var(--accent);
  margin-left: 0.4rem;
  background: rgba(217, 119, 6, 0.1);
  padding: 0 6px;
  border-radius: 10px;
}

/* 内容预览 */
.chapter-preview {
  max-width: 220px;
}

.preview-text {
  font-size: 0.75rem;
  color: var(--text-secondary);
  line-height: 1.4;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-empty {
  font-size: 0.7rem;
  color: var(--text-muted);
  font-style: italic;
}

.chapter-words {
  font-size: 0.78rem;
  color: var(--text-secondary);
}

.update-time {
  font-size: 0.72rem;
  color: var(--text-muted);
}

/* 当前行高亮 */
.current-row {
  background: rgba(217, 119, 6, 0.04);
  border-left: 3px solid var(--accent);
}

/* 操作按钮 */
.chapter-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ─── 空状态 ─── */
.empty-state {
  text-align: center;
  padding: 2.5rem 0;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 2.5rem;
  display: block;
  margin-bottom: 0.5rem;
}

/* ─── 响应式 ─── */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .chapters-table th,
  .chapters-table td {
    padding: 0.4rem 0.6rem;
    font-size: 0.7rem;
  }
  .chapter-preview {
    max-width: 120px;
  }
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-tabs {
    margin-left: 0;
    justify-content: center;
  }
  .search-input {
    max-width: 100%;
  }
}

@media (max-width: 640px) {
  .chapters-table thead {
    display: none;
  }
  .chapters-table tbody tr {
    display: block;
    border-bottom: 1px solid var(--border);
    padding: 0.6rem 0.8rem;
  }
  .chapters-table td {
    display: flex;
    justify-content: space-between;
    border: none;
    padding: 4px 0;
  }
  .chapter-preview {
    max-width: 100%;
  }
  .chapter-actions {
    justify-content: flex-end;
  }
}
</style>