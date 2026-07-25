<template>
  <div class="outline-panel">
    <!-- 顶部栏 -->
    <div class="outline-header">
      <div class="header-left">
        <span class="header-title">📐 三幕式结构</span>
        <span class="header-divider">|</span>
        <span class="header-stats">{{ sortedNodes.length }} 节点 · {{ totalWords }} 字</span>
      </div>
      <div class="header-center">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            v-model="searchQuery"
            class="search-input"
            placeholder="搜索大纲节点..."
            @input="onSearchInput"
          />
          <button v-if="searchQuery" class="search-clear" @click="searchQuery = ''">✕</button>
        </div>
        <select v-model="statusFilter" class="filter-select">
          <option value="all">全部状态</option>
          <option value="draft">📝 草稿</option>
          <option value="pending">🔧 待修改</option>
          <option value="completed">✅ 已完成</option>
        </select>
      </div>
      <div class="header-right">
        <button class="btn-add-header" @click="handleAddNode(null)">
          <span class="btn-add-icon">+</span>
          新建节点
        </button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="outline-body">
      <!-- 左侧列表区 -->
      <div class="outline-list">
        <!-- 空状态 -->
        <div v-if="sortedNodes.length === 0" class="empty-state">
          <div class="empty-icon">📋</div>
          <div class="empty-text">暂无大纲节点</div>
          <div class="empty-hint">点击右下角 "+ 新建节点" 开始创建故事结构</div>
        </div>

        <!-- 幕分组 -->
        <div v-for="act in acts" :key="act.key" class="act-group" @dragover.prevent="onDragOver($event, act.key)" @drop.prevent="onDrop($event, act.key)">
          <!-- 幕标题 -->
          <div class="act-header" :style="{ borderLeftColor: act.color }">
            <div class="act-header-left">
              <span class="act-icon">{{ act.icon }}</span>
              <span class="act-title">{{ act.label }}</span>
              <span class="act-progress" :class="{ full: actNodes(act.key).length > 0 }">
                ({{ completedCount(act.key) }}/{{ actNodes(act.key).length }})
              </span>
            </div>
            <div class="act-header-right">
              <button class="btn-act-add" @click.stop="handleAddNode(act.key)" title="在此幕下添加节点">
                + 在此幕添加
              </button>
            </div>
          </div>

          <!-- 幕内节点列表 -->
          <div v-if="actNodes(act.key).length === 0" class="act-empty">
            暂无节点，点击上方按钮添加
          </div>
          <div
            v-for="(node, index) in actNodes(act.key)"
            :key="node.id"
            class="node-card"
            :class="{
              'is-dragging': dragState.nodeId === node.id,
              'is-key-event': node.isKeyEvent,
              'is-selected': selectedNodes.has(node.id),
              'highlight-flash': highlightId === node.id
            }"
            draggable="true"
            @dragstart="onDragStart($event, node, act.key, index)"
            @dragend="onDragEnd"
            @click.stop="toggleSelect(node.id)"
          >
            <!-- 拖拽手柄 -->
            <div class="drag-handle" title="拖拽排序">⋮⋮</div>

            <!-- 序号 -->
            <span class="node-number">{{ index + 1 }}</span>

            <!-- 类型图标 -->
            <span class="node-type-icon">{{ typeIcon(node.type) }}</span>

            <!-- 节点内容 -->
            <div class="node-content">
              <div class="node-title-row">
                <span class="node-title">{{ node.title || '未命名节点' }}</span>
                <span v-if="node.isKeyEvent" class="key-event-badge" title="核心情节点">⭐</span>
              </div>
              <div v-if="node.description" class="node-desc">{{ truncateText(node.description, 60) }}</div>
            </div>

            <!-- 字数 -->
            <span v-if="node.estimatedWords" class="node-words">{{ node.estimatedWords }}字</span>

            <!-- 状态标签 -->
            <span class="status-badge" :class="'status-' + (node.nodeStatus || 'draft')">
              {{ statusLabel(node.nodeStatus || 'draft') }}
            </span>

            <!-- 关联章节 -->
            <span v-if="node.chapterId && getChapterLabel(node.chapterId)" class="chapter-link" :title="'关联章节：' + getChapterLabel(node.chapterId)">
              📎{{ getChapterLabel(node.chapterId).replace('第', '').replace('章', '章') }}
            </span>

            <!-- 操作按钮 -->
            <div class="node-actions">
              <button class="btn-icon" title="编辑" @click.stop="handleEditNode(node)">✏️</button>
              <button class="btn-icon btn-icon-danger" title="删除" @click.stop="handleDeleteNode(node)">🗑️</button>
            </div>
          </div>

          <!-- 拖拽放置提示 -->
          <div v-if="dragState.active && dragOverAct === act.key" class="drop-indicator">
            放置到此幕
          </div>
        </div>
      </div>

      <!-- 右侧结构评分 -->
      <div class="structure-sidebar">
        <div class="sidebar-title">📊 结构评分</div>
        <div class="score-main">
          <span class="score-value">87</span>
          <span class="score-total">/100</span>
        </div>
        <div class="score-bar">
          <div class="score-bar-fill" style="width:87%"></div>
        </div>
        <div class="score-items">
          <div v-for="dim in structureScores" :key="dim.label" class="score-item">
            <span class="score-label">{{ dim.label }}</span>
            <span class="score-num" :class="scoreColorClass(dim.score)">{{ dim.score }}</span>
          </div>
        </div>
        <div class="sidebar-tip">
          章节密度：{{ chapterDensity }} 字/章
        </div>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <Transition name="batch-slide">
      <div v-if="selectedNodes.size > 0" class="batch-bar">
        <span class="batch-count">已选 {{ selectedNodes.size }} 项</span>
        <div class="batch-actions">
          <select v-model="batchStatusValue" class="batch-select">
            <option value="draft">📝 草稿</option>
            <option value="pending">🔧 待修改</option>
            <option value="completed">✅ 已完成</option>
          </select>
          <button class="btn-batch btn-batch-status" @click="handleBatchStatus">应用状态</button>
          <button class="btn-batch btn-batch-delete" @click="handleBatchDelete">批量删除</button>
          <button class="btn-batch btn-batch-cancel" @click="selectedNodes.clear()">取消选择</button>
        </div>
      </div>
    </Transition>

    <!-- 大纲节点编辑弹窗 -->
    <OutlineNodeDialog
      v-model="dialogVisible"
      :project-id="projectId"
      :edit-node="editingNode"
      :default-act="dialogDefaultAct"
      @saved="onDialogSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useNovelStore } from '@/stores/novel'
import OutlineNodeDialog from './OutlineNodeDialog.vue'

const store = useNovelStore()

const props = defineProps({
  projectId: { type: [Number, String], required: true }
})

// ─── 常量 ───
const acts = [
  { key: 'first_act', label: '第一幕：建置', icon: '📖', color: '#3b82f6' },
  { key: 'second_act', label: '第二幕：对抗', icon: '⚔️', color: '#f59e0b' },
  { key: 'third_act', label: '第三幕：解决', icon: '🏆', color: '#ef4444' }
]

const structureScores = [
  { label: '叙事节奏', score: 92 },
  { label: '幕间平衡', score: 85 },
  { label: '高潮分布', score: 78 },
  { label: '人物弧线对齐', score: 90 }
]

// ─── 状态 ───
const searchQuery = ref('')
const statusFilter = ref('all')
const dialogVisible = ref(false)
const editingNode = ref(null)
const dialogDefaultAct = ref(null)
const highlightId = ref(null)
const selectedNodes = ref(new Set())
const batchStatusValue = ref('draft')
const dragState = ref({ active: false, nodeId: null, sourceAct: null, sourceIndex: -1 })
const dragOverAct = ref(null)

// ─── 计算属性 ───
const allOutlines = computed(() => {
  const list = (store.outlines || []).map(n => ({ ...n }))
  // 按 sortOrder 排序
  list.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))

  const total = list.length
  if (total === 0) return list

  // 自动分配未设置幕的节点（按排序位置均匀分布到三幕）
  const perAct = Math.max(1, Math.ceil(total / 3))
  const actKeys = ['first_act', 'second_act', 'third_act']
  return list.map((n, i) => {
    if (!n.act) {
      return { ...n, act: actKeys[Math.min(Math.floor(i / perAct), 2)] }
    }
    return n
  })
})

const filteredOutlines = computed(() => {
  let list = [...allOutlines.value]
  // 状态筛选
  if (statusFilter.value !== 'all') {
    list = list.filter(n => (n.nodeStatus || 'draft') === statusFilter.value)
  }
  // 关键词搜索
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(n =>
      (n.title || '').toLowerCase().includes(q) ||
      (n.description || '').toLowerCase().includes(q)
    )
  }
  return list
})

const sortedNodes = computed(() => {
  return [...allOutlines.value].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

const totalWords = computed(() =>
  sortedNodes.value.reduce((sum, n) => sum + (n.estimatedWords || 0), 0)
)

const chapterDensity = computed(() =>
  sortedNodes.value.length > 0 ? Math.round(totalWords.value / sortedNodes.value.length) : 0
)

const suggestedAct = computed(() => {
  // 智能推荐：找出节点数最少的幕
  const counts = acts.map(a => ({
    key: a.key,
    count: actNodes(a.key).length
  }))
  counts.sort((a, b) => a.count - b.count)
  return counts[0]?.key || 'first_act'
})

// ─── 方法 ───
function actNodes(actKey) {
  return filteredOutlines.value.filter(n => n.act === actKey)
}

function completedCount(actKey) {
  const nodes = allOutlines.value.filter(n => n.act === actKey && n.nodeStatus === 'completed')
  return nodes.length
}

function typeIcon(type) {
  const map = { volume: '📚', chapter: '📄', scene: '🎬' }
  return map[type] || '📋'
}

function statusLabel(status) {
  const map = { draft: '📝草稿', pending: '🔧待修改', completed: '✅完成' }
  return map[status] || '📝草稿'
}

function truncateText(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

function scoreColorClass(score) {
  if (score >= 80) return 'score-green'
  if (score >= 60) return 'score-amber'
  return 'score-red'
}

function getChapterLabel(chapterId) {
  const ch = store.chapters.find(c => String(c.id) === String(chapterId))
  if (!ch) return ''
  const sortOrder = ch.sortOrder || ch.chapterNumber || ''
  return sortOrder ? `第${sortOrder}章` : ch.title || ''
}

function onSearchInput() {
  // 搜索时清除选择
  selectedNodes.value.clear()
}

function toggleSelect(nodeId) {
  const newSet = new Set(selectedNodes.value)
  if (newSet.has(nodeId)) {
    newSet.delete(nodeId)
  } else {
    newSet.add(nodeId)
  }
  selectedNodes.value = newSet
}

// ─── 拖拽 ───
function onDragStart(event, node, actKey, index) {
  dragState.value = { active: true, nodeId: node.id, sourceAct: actKey, sourceIndex: index }
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', node.id)
}

function onDragOver(event, actKey) {
  if (!dragState.value.active) return
  dragOverAct.value = actKey
  event.dataTransfer.dropEffect = 'move'
}

function onDrop(event, targetAct) {
  if (!dragState.value.active) return
  const { nodeId, sourceAct } = dragState.value

  if (!nodeId) {
    resetDrag()
    return
  }

  // 找到被拖拽的节点
  const node = allOutlines.value.find(n => n.id === nodeId)
  if (!node) {
    resetDrag()
    return
  }

  // 获取目标幕的所有节点（未筛选）
  const targetNodes = [...allOutlines.value]
    .filter(n => (n.act || 'first_act') === targetAct)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))

  const isSameAct = sourceAct === targetAct

  // 构建新的排序
  const allItems = [...allOutlines.value]
    .filter(n => n.id !== nodeId)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))

  // 重算排序
  let sortCounter = 0
  const batchItems = []

  for (const item of allItems) {
    sortCounter++
    const act = item.act || 'first_act'
    batchItems.push({
      id: item.id,
      projectId: Number(props.projectId),
      sortOrder: sortCounter,
      act: act,
      nodeNumber: 0 // 暂不计算幕内序号
    })
  }

  // 把拖拽的节点加到最后（目标幕）
  sortCounter++
  batchItems.push({
    id: nodeId,
    projectId: Number(props.projectId),
    sortOrder: sortCounter,
    act: targetAct,
    nodeNumber: 0
  })

  // 如果跨幕，使用 batchSortAct
  if (!isSameAct) {
    const sortActItems = batchItems.map(b => ({
      id: b.id,
      projectId: b.projectId,
      act: targetAct === (allOutlines.value.find(n => n.id === b.id)?.act || 'first_act') ? (allOutlines.value.find(n => n.id === b.id)?.act || 'first_act') : targetAct,
      sortOrder: b.sortOrder,
      nodeNumber: 0
    }))
    // 修正 act 字段
    for (const item of sortActItems) {
      const existing = allOutlines.value.find(n => n.id === item.id)
      if (String(item.id) === String(nodeId)) {
        item.act = targetAct
      } else {
        item.act = existing?.act || 'first_act'
      }
    }
    store.batchSortActOutlines(props.projectId, sortActItems)
  } else {
    store.batchSortOutlines(props.projectId, batchItems.map(b => ({
      id: b.id,
      projectId: b.projectId,
      sortOrder: b.sortOrder,
      nodeNumber: 0
    })))
  }

  resetDrag()
}

function onDragEnd() {
  resetDrag()
}

function resetDrag() {
  dragState.value = { active: false, nodeId: null, sourceAct: null, sourceIndex: -1 }
  dragOverAct.value = null
}

// ─── 节点操作 ───
function handleAddNode(actKey) {
  dialogDefaultAct.value = actKey || suggestedAct.value
  editingNode.value = null
  dialogVisible.value = true
}

function handleEditNode(node) {
  dialogDefaultAct.value = node.act || 'first_act'
  editingNode.value = {
    id: node.id,
    title: node.title || '',
    description: node.description || '',
    estimatedWords: node.estimatedWords || null,
    type: node.type || '',
    act: node.act || 'first_act',
    nodeStatus: node.nodeStatus || 'draft',
    chapterId: node.chapterId || null,
    isKeyEvent: node.isKeyEvent || false
  }
  dialogVisible.value = true
}

async function handleDeleteNode(node) {
  if (!window.confirm(`确定要删除大纲节点「${node.title}」吗？此操作不可恢复。`)) return
  try {
    await store.deleteOutline(props.projectId, node.id)
  } catch (e) {
    console.error('删除大纲节点失败：', e.message)
  }
}

async function onDialogSaved() {
  await store.refreshOutlines(props.projectId)
  // 高亮新节点
  const lastNode = store.outlines[store.outlines.length - 1]
  if (lastNode) {
    highlightId.value = lastNode.id
    setTimeout(() => { highlightId.value = null }, 1500)
  }
}

// ─── 批量操作 ───
async function handleBatchStatus() {
  if (selectedNodes.value.size === 0) return
  const ids = Array.from(selectedNodes.value)
  try {
    await store.batchStatusOutlines(props.projectId, ids, batchStatusValue.value)
    selectedNodes.value.clear()
  } catch (e) {
    console.error('批量更新状态失败：', e.message)
  }
}

async function handleBatchDelete() {
  if (selectedNodes.value.size === 0) return
  if (!window.confirm(`确定要删除选中的 ${selectedNodes.value.size} 个大纲节点吗？此操作不可恢复。`)) return
  const ids = Array.from(selectedNodes.value)
  try {
    await store.batchDeleteOutlines(props.projectId, ids)
    selectedNodes.value.clear()
  } catch (e) {
    console.error('批量删除失败：', e.message)
  }
}
</script>

<style scoped>
/* ─── 布局 ─── */
.outline-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

/* ─── 顶部栏 ─── */
.outline-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #e8e3dc;
  background: #faf8f5;
  flex-shrink: 0;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.header-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1815;
}

.header-divider {
  color: #d4cec6;
  font-size: 13px;
}

.header-stats {
  font-size: 11px;
  color: #9c9690;
  white-space: nowrap;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  justify-content: center;
}

.search-box {
  position: relative;
  width: 220px;
}

.search-icon {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 5px 28px 5px 26px;
  border: 1.5px solid #e8e3dc;
  border-radius: 8px;
  font-size: 12px;
  outline: none;
  background: #fff;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.search-input:focus {
  border-color: #d97706;
}

.search-clear {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 11px;
  color: #9c9690;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
}

.filter-select {
  padding: 5px 10px;
  border: 1.5px solid #e8e3dc;
  border-radius: 8px;
  font-size: 12px;
  outline: none;
  background: #fff;
  cursor: pointer;
  color: #1a1815;
}
.filter-select:focus {
  border-color: #d97706;
}

.header-right {
  flex-shrink: 0;
}

.btn-add-header {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  background: #d97706;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  white-space: nowrap;
}
.btn-add-header:hover {
  background: #b45309;
}
.btn-add-icon {
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
}

/* ─── 主体 ─── */
.outline-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.outline-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 32px;
}

/* ─── 空状态 ─── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9c9690;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}
.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: #6b6560;
  margin-bottom: 6px;
}
.empty-hint {
  font-size: 12px;
  color: #b0a9a2;
}

/* ─── 幕分组 ─── */
.act-group {
  margin-bottom: 20px;
  border: 1px solid #f0ece6;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  transition: border-color 0.2s;
}

.act-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-left: 4px solid #3b82f6;
  background: #faf8f5;
  border-bottom: 1px solid #f0ece6;
}

.act-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.act-icon {
  font-size: 18px;
}

.act-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1815;
}

.act-progress {
  font-size: 11px;
  color: #9c9690;
  font-weight: 500;
}
.act-progress.full {
  color: #16a34a;
}

.act-header-right {
  flex-shrink: 0;
}

.btn-act-add {
  padding: 3px 10px;
  background: transparent;
  border: 1.5px dashed #d4cec6;
  border-radius: 6px;
  font-size: 11px;
  color: #9c9690;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.btn-act-add:hover {
  border-color: #d97706;
  color: #d97706;
  background: #fffbeb;
}

.act-empty {
  padding: 20px;
  text-align: center;
  font-size: 12px;
  color: #b0a9a2;
  font-style: italic;
}

/* ─── 节点卡片 ─── */
.node-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-bottom: 1px solid #f5f2ed;
  transition: all 0.2s;
  cursor: pointer;
  user-select: none;
}
.node-card:last-child {
  border-bottom: none;
}
.node-card:hover {
  background: #faf8f5;
}
.node-card.is-dragging {
  opacity: 0.4;
  background: #fef3c7;
}
.node-card.is-key-event {
  background: #fffbeb;
}
.node-card.is-key-event:hover {
  background: #fef3c7;
}
.node-card.is-selected {
  background: #e8f4fd;
}
.node-card.highlight-flash {
  animation: flashHighlight 1.5s ease-out;
}

@keyframes flashHighlight {
  0%, 100% { background: transparent; }
  20% { background: #fef3c7; }
  40% { background: #fef9c3; }
  60% { background: #fef3c7; }
}

.drag-handle {
  font-size: 11px;
  color: #d4cec6;
  cursor: grab;
  padding: 2px 4px;
  flex-shrink: 0;
  letter-spacing: -2px;
  line-height: 1;
}
.drag-handle:active {
  cursor: grabbing;
}

.node-number {
  font-size: 11px;
  font-weight: 700;
  color: #9c9690;
  min-width: 18px;
  text-align: center;
  flex-shrink: 0;
}

.node-type-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.node-content {
  flex: 1;
  min-width: 0;
}

.node-title-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.node-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1815;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.key-event-badge {
  font-size: 11px;
  flex-shrink: 0;
}

.node-desc {
  font-size: 11px;
  color: #9c9690;
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 260px;
}

.node-words {
  font-size: 11px;
  color: #9c9690;
  white-space: nowrap;
  flex-shrink: 0;
}

/* ─── 状态标签 ─── */
.status-badge {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}
.status-draft {
  background: #f5f2ed;
  color: #9c9690;
}
.status-pending {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}
.status-completed {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.chapter-link {
  font-size: 10px;
  color: #6366f1;
  background: #eef2ff;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
  cursor: default;
}

/* ─── 操作按钮 ─── */
.node-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}
.node-card:hover .node-actions {
  opacity: 1;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  transition: all 0.2s;
  line-height: 1;
}
.btn-icon:hover {
  background: #fff;
}

.drop-indicator {
  padding: 8px;
  text-align: center;
  font-size: 11px;
  color: #d97706;
  background: #fffbeb;
  border: 2px dashed #fde68a;
  margin: 4px 8px;
  border-radius: 8px;
}

/* ─── 右侧评分面板 ─── */
.structure-sidebar {
  width: 180px;
  border-left: 1px solid #e8e3dc;
  padding: 16px 14px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #faf8f5;
}

.sidebar-title {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  color: #9c9690;
  margin-bottom: 10px;
  letter-spacing: 0.5px;
}

.score-main {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin-bottom: 4px;
}

.score-value {
  font-size: 28px;
  font-weight: 800;
  color: #d97706;
  font-family: var(--font-display, sans-serif);
  line-height: 1;
}

.score-total {
  font-size: 13px;
  color: #9c9690;
}

.score-bar {
  width: 100%;
  height: 6px;
  background: #f3efe8;
  border-radius: 3px;
  margin-bottom: 12px;
  overflow: hidden;
}

.score-bar-fill {
  height: 100%;
  background: #d97706;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.score-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.score-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.score-label {
  font-size: 11px;
  color: #9c9690;
}

.score-num {
  font-size: 12px;
  font-weight: 700;
}
.score-green { color: #16a34a; }
.score-amber { color: #d97706; }
.score-red { color: #dc2626; }

.sidebar-tip {
  font-size: 10px;
  color: #9c9690;
  line-height: 1.5;
  padding-top: 10px;
  border-top: 1px solid #f3efe8;
}

/* ─── 批量操作栏 ─── */
.batch-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border-top: 2px solid #d97706;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.08);
  z-index: 20;
}

.batch-count {
  font-size: 12px;
  font-weight: 600;
  color: #1a1815;
  white-space: nowrap;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-select {
  padding: 5px 10px;
  border: 1.5px solid #e8e3dc;
  border-radius: 6px;
  font-size: 12px;
  outline: none;
  background: #fff;
  cursor: pointer;
}

.btn-batch {
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-batch-status {
  background: #d97706;
  color: #fff;
}
.btn-batch-status:hover {
  background: #b45309;
}

.btn-batch-delete {
  background: #dc2626;
  color: #fff;
}
.btn-batch-delete:hover {
  background: #b91c1c;
}

.btn-batch-cancel {
  background: #f5f2ed;
  color: #6b6560;
}
.btn-batch-cancel:hover {
  background: #e8e3dc;
}

/* ─── 批量操作栏过渡 ─── */
.batch-slide-enter-active,
.batch-slide-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}
.batch-slide-enter-from,
.batch-slide-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>
