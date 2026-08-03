<template>
  <div class="inspiration-section">
    <!-- ─── 顶部统计条 ─── -->
    <div class="inspiration-stats-bar">
      <div class="stat-item">
        <span class="stat-num">{{ store.typeCounts.all }}</span>
        <span class="stat-label">总灵感</span>
      </div>
      <div class="stat-item">
        <span class="stat-num accent">{{ store.typeCounts['对白灵感'] }}</span>
        <span class="stat-label">对白</span>
      </div>
      <div class="stat-item">
        <span class="stat-num green">{{ store.typeCounts['场景描写'] }}</span>
        <span class="stat-label">场景</span>
      </div>
      <div class="stat-item">
        <span class="stat-num purple">{{ store.typeCounts['细节设定'] }}</span>
        <span class="stat-label">细节</span>
      </div>
      <div class="stat-item">
        <span class="stat-num gray">{{ store.typeCounts['参考资料'] }}</span>
        <span class="stat-label">参考</span>
      </div>
      <div class="stat-item" v-if="store.typeCounts['ai'] > 0">
        <span class="stat-num ai-color">{{ store.typeCounts['ai'] }}</span>
        <span class="stat-label">AI</span>
      </div>
    </div>

    <!-- ─── 筛选标签栏 ─── -->
    <div class="filter-tab-bar">
      <button
        v-for="tab in filterTabs"
        :key="tab.value"
        class="filter-tab-btn"
        :class="{ active: store.filterType === tab.value }"
        @click="store.setFilter(tab.value)"
      >
        {{ tab.icon }} {{ tab.label }}
        <span class="tab-count" v-if="store.typeCounts[tab.value] > 0">{{ store.typeCounts[tab.value] }}</span>
      </button>
    </div>

    <!-- ─── 搜索与排序栏 ─── -->
    <div class="search-sort-bar">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchInput"
          type="text"
          placeholder="搜索灵感内容、标签..."
          class="search-input"
          @input="onSearchInput"
        />
        <button v-if="store.searchQuery" class="search-clear" @click="clearSearch">✕</button>
      </div>
      <div class="sort-select-wrapper">
        <select v-model="sortModel" class="sort-select" @change="store.setSortBy(sortModel)">
          <option value="time-desc">最新优先</option>
          <option value="time-asc">最早优先</option>
          <option value="chapter">按章节</option>
          <option value="used">未使用优先</option>
        </select>
      </div>
    </div>

    <!-- ─── 灵感卡片列表 ─── -->
    <div class="inspiration-list" v-if="store.filteredInspirations.length > 0">
      <div
        v-for="item in store.filteredInspirations"
        :key="item.id"
        class="inspiration-card"
        :class="{ 'is-highlight': item.isHighlight, 'is-used': item.isUsed, 'new-item': item._isNew }"
        @click="openActionSheet(item)"
      >
        <!-- 类型徽章 -->
        <div class="card-type-badge" :class="'type-' + (item.type || '对白灵感')">
          {{ typeIcon(item.type) }} {{ typeLabel(item.type) }}
        </div>

        <!-- 内容 -->
        <div class="card-content">{{ item.content || '' }}</div>

        <!-- 底部信息 -->
        <div class="card-footer">
          <span class="card-time">{{ formatTime(item.createTime) }}</span>
          <div class="card-tags" v-if="item.tags">
            <span class="card-tag" v-for="tag in parseTags(item.tags)" :key="tag">{{ tag }}</span>
          </div>
        </div>

        <!-- 已使用标记 -->
        <div v-if="item.isUsed" class="used-badge">已使用</div>

        <!-- 高亮点 -->
        <div v-if="item.isHighlight" class="highlight-dot"></div>
      </div>
    </div>

    <!-- ─── 空状态 ─── -->
    <div v-else class="empty-state">
      <div class="empty-icon-wrap">
        <span v-if="store.filterType === 'all'">💡</span>
        <span v-else-if="store.filterType === 'ai'">🤖</span>
        <span v-else-if="store.filterType === '对白灵感'">💬</span>
        <span v-else-if="store.filterType === '场景描写'">🎬</span>
        <span v-else-if="store.filterType === '细节设定'">🔍</span>
        <span v-else>📚</span>
      </div>
      <p class="empty-text">
        {{ emptyText }}
      </p>
      <button class="btn-empty-add" @click="openCreateDialog">
        {{ emptyButtonText }}
      </button>
    </div>

    <!-- ─── AI 灵感生成器 ─── -->
    <div class="ai-generator-panel">
      <div class="ai-gen-header" @click="aiPanelCollapsed = !aiPanelCollapsed">
        <span class="ai-gen-title">🤖 AI 灵感生成器</span>
        <span class="ai-gen-toggle">{{ aiPanelCollapsed ? '展开' : '收起' }}</span>
      </div>
      <div v-show="!aiPanelCollapsed" class="ai-gen-body">
        <!-- AI 生成结果 -->
        <div v-if="store.aiResults.length > 0" class="ai-results">
          <div class="ai-results-scroll">
            <div
              v-for="(result, idx) in store.aiResults"
              :key="result.id || idx"
              class="ai-result-card"
              :class="'type-' + result.type"
            >
              <div class="ai-result-type">{{ typeIcon(result.type) }} {{ typeLabel(result.type) }}</div>
              <div class="ai-result-content">{{ result.content || '' }}</div>
              <button class="btn-accept" @click.stop="acceptResult(result)">
                ✅ 采纳
              </button>
            </div>
          </div>
          <div class="ai-actions">
            <button class="btn-regenerate" @click="handleGenerate" :disabled="store.aiGenerating">
              🔄 换一批
            </button>
          </div>
        </div>

        <!-- 加载骨架屏 -->
        <div v-if="store.aiGenerating" class="ai-loading">
          <div class="skeleton-card" v-for="i in 3" :key="i">
            <div class="skeleton-line w-20"></div>
            <div class="skeleton-line w-full"></div>
            <div class="skeleton-line w-3/4"></div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="ai-input-row">
          <input
            v-model="aiKeywords"
            type="text"
            class="ai-input"
            placeholder="输入关键词，如：决斗 黄昏 巨人"
            @keydown.enter="handleGenerate"
          />
          <button
            class="btn-ai-generate"
            @click="handleGenerate"
            :disabled="store.aiGenerating || !aiKeywords.trim()"
          >
            <span v-if="store.aiGenerating" class="spinner-sm"></span>
            <span v-else>✨ AI 生成</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ═══════ ActionSheet 底部操作面板 ═══════ -->
    <Teleport to="body">
      <Transition name="action-sheet">
        <div v-if="showSheet" class="sheet-overlay" @click.self="closeSheet">
          <div class="sheet-panel">
            <div class="sheet-handle"></div>
            <div class="sheet-header">
              <h3>{{ typeIcon(sheetItem?.type) }} {{ typeLabel(sheetItem?.type) }}</h3>
            </div>

            <!-- 详情模式 -->
            <div v-if="sheetMode === 'detail'" class="sheet-body">
              <div class="detail-content">{{ sheetItem?.content || '' }}</div>
              <div class="detail-meta">
                <div class="meta-row">
                  <span class="meta-label">创建时间</span>
                  <span class="meta-value">{{ formatTime(sheetItem?.createTime) }}</span>
                </div>
                <div class="meta-row" v-if="sheetItem?.chapterName">
                  <span class="meta-label">关联章节</span>
                  <span class="meta-value">{{ sheetItem.chapterName }}</span>
                </div>
                <div class="meta-row" v-if="sheetItem?.tags">
                  <span class="meta-label">标签</span>
                  <span class="meta-value tags-value">
                    <span v-for="tag in parseTags(sheetItem.tags)" :key="tag" class="meta-tag">{{ tag }}</span>
                  </span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">来源</span>
                  <span class="meta-value">{{ sheetItem?.source === 'ai' ? 'AI 生成' : '手动录入' }}</span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">状态</span>
                  <span class="meta-value" :class="sheetItem?.isUsed ? 'used' : 'pending'">
                    {{ sheetItem?.isUsed ? '已使用' : '待使用' }}
                  </span>
                </div>
              </div>
            </div>

            <!-- 编辑模式 -->
            <div v-if="sheetMode === 'edit'" class="sheet-body">
              <div class="form-group">
                <label>灵感内容 <span class="required">*</span></label>
                <textarea v-model="editForm.content" rows="3" placeholder="输入灵感内容..."></textarea>
              </div>
              <div class="form-group">
                <label>类型</label>
                <select v-model="editForm.type">
                  <option v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.icon }} {{ t.label }}</option>
                </select>
              </div>
              <div class="form-group">
                <label>关联章节</label>
                <select v-model="editForm.chapterId">
                  <option value="">不关联</option>
                  <option v-for="ch in chapters" :key="ch.id" :value="ch.id">{{ ch.title || '第' + (ch.sortOrder || '?') + '章' }}</option>
                </select>
              </div>
              <div class="form-group">
                <label>标签</label>
                <div class="tag-input-wrap">
                  <div class="tag-list" v-if="editForm.tagsArray.length > 0">
                    <span v-for="(tag, ti) in editForm.tagsArray" :key="ti" class="edit-tag">
                      {{ tag }}
                      <button class="tag-remove" @click="removeEditTag(ti)">✕</button>
                    </span>
                  </div>
                  <div class="tag-add-row">
                    <input v-model="editTagInput" type="text" placeholder="输入标签后按回车" @keydown.enter.prevent="addEditTag" />
                    <button class="tag-add-btn" @click="addEditTag">+</button>
                  </div>
                </div>
              </div>
              <div class="form-row-inline">
                <label class="toggle-label">
                  <span>高亮置顶</span>
                  <label class="toggle-switch-sm">
                    <input type="checkbox" v-model="editForm.isHighlight" />
                    <span class="toggle-slider-sm"></span>
                  </label>
                </label>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="sheet-actions">
              <button v-if="sheetMode === 'detail'" class="sheet-btn btn-edit" @click="switchToEdit">
                ✏️ 编辑
              </button>
              <button class="sheet-btn" :class="sheetItem?.isUsed ? 'btn-unused' : 'btn-used'" @click="handleToggleUsed">
                {{ sheetItem?.isUsed ? '↩️ 标记待用' : '✅ 标记已使用' }}
              </button>
              <button class="sheet-btn btn-copy" @click="copyContent">
                📋 复制内容
              </button>
              <button class="sheet-btn btn-delete" @click="handleDelete">
                🗑️ 删除
              </button>
              <button v-if="sheetMode === 'edit'" class="sheet-btn btn-save" @click="handleEditSave">
                💾 保存修改
              </button>
              <button class="sheet-btn btn-cancel-sheet" @click="closeSheet">
                取消
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══════ 新建灵感弹窗 ═══════ -->
    <Teleport to="body">
      <Transition name="dialog-fade">
        <div v-if="showCreateDialog" class="modal-overlay" @click.self="showCreateDialog = false">
          <div class="modal-content insp-modal">
            <div class="modal-header">
              <h2>📝 {{ createDialogTitle }}</h2>
              <button class="close" @click="showCreateDialog = false">✕</button>
            </div>

            <div class="form-group">
              <label>灵感类型 <span class="required">*</span></label>
              <select v-model="createForm.type">
                <option v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.icon }} {{ t.label }}</option>
              </select>
            </div>

            <div class="form-group">
              <label>灵感内容 <span class="required">*</span></label>
              <textarea v-model="createForm.content" rows="3" placeholder="输入灵感内容..." maxlength="1000"></textarea>
              <span class="char-count">{{ (createForm.content || '').length }} / 1000</span>
            </div>

            <div class="form-group">
              <label>关联章节</label>
              <select v-model="createForm.chapterId">
                <option value="">不关联</option>
                <option v-for="ch in chapters" :key="ch.id" :value="ch.id">
                  {{ ch.title || '第' + (ch.sortOrder || '?') + '章' }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label>标签</label>
              <div class="tag-input-wrap">
                <div class="tag-list" v-if="createForm.tagsArray.length > 0">
                  <span v-for="(tag, ti) in createForm.tagsArray" :key="ti" class="edit-tag">
                    {{ tag }}
                    <button class="tag-remove" @click="removeCreateTag(ti)">✕</button>
                  </span>
                </div>
                <div class="tag-add-row">
                  <input v-model="createTagInput" type="text" placeholder="输入标签后按回车" @keydown.enter.prevent="addCreateTag" />
                  <button class="tag-add-btn" @click="addCreateTag">+</button>
                </div>
              </div>
            </div>

            <div class="form-row-inline">
              <label class="toggle-label">
                <span>高亮置顶</span>
                <label class="toggle-switch-sm">
                  <input type="checkbox" v-model="createForm.isHighlight" />
                  <span class="toggle-slider-sm"></span>
                </label>
              </label>
            </div>

            <div class="modal-footer">
              <button class="btn-cancel" @click="showCreateDialog = false">取消</button>
              <button class="btn-confirm" @click="handleCreate" :disabled="!createForm.content.trim()">
                🚀 确认新增
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, nextTick } from 'vue'
import { useInspirationStore } from '@/stores/inspiration'
import { useNovelStore } from '@/stores/novel'
import { requireVip } from '@/services/vipService'

const props = defineProps({
  projectId: { type: [String, Number], default: null }
})

console.log('[InspirationSection] script setup executing, projectId:', props.projectId)

const emit = defineEmits(['refresh'])

const store = useInspirationStore()
const novelStore = useNovelStore()

const chapters = computed(() => novelStore.chapters || [])

// ─── 类型配置 ───
const typeOptions = [
  { value: '对白灵感', icon: '💬', label: '对白灵感' },
  { value: '场景描写', icon: '🎬', label: '场景描写' },
  { value: '细节设定', icon: '🔍', label: '细节设定' },
  { value: '参考资料', icon: '📚', label: '参考资料' }
]

const filterTabs = [
  { value: 'all', icon: '📋', label: '全部' },
  { value: '对白灵感', icon: '💬', label: '对白灵感' },
  { value: '场景描写', icon: '🎬', label: '场景描写' },
  { value: '细节设定', icon: '🔍', label: '细节设定' },
  { value: '参考资料', icon: '📚', label: '参考资料' },
  { value: 'ai', icon: '🤖', label: 'AI生成' }
]

const TYPE_EN_TO_CN = {
  'dialogue': '对白灵感',
  'scene': '场景描写',
  'detail': '细节设定',
  'reference': '参考资料'
}

function typeLabel(type) {
  const found = typeOptions.find(t => t.value === type)
  if (found) return found.label
  // 兼容旧数据：英文type转中文
  return TYPE_EN_TO_CN[type] || type
}

function typeIcon(type) {
  const found = typeOptions.find(t => t.value === type)
  if (found) return found.icon
  // 兼容旧数据：英文type转中文
  const chineseType = TYPE_EN_TO_CN[type]
  const foundCn = typeOptions.find(t => t.value === chineseType)
  return foundCn ? foundCn.icon : '💡'
}

function parseTags(tags) {
  if (!tags) return []
  return tags.split(/[,，、\s]+/).filter(Boolean)
}

// ─── 时间格式化 ───
function formatTime(date) {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

// ─── 搜索 ───
const searchInput = ref('')
let searchTimer = null

function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    store.setSearchQuery(searchInput.value)
  }, 300)
}

function clearSearch() {
  searchInput.value = ''
  store.setSearchQuery('')
}

const sortModel = ref('time-desc')

// ─── 空状态 ───
const emptyText = computed(() => {
  if (store.filterType === 'all') return '暂无灵感素材，点击按钮或使用 AI 生成器创建第一条灵感吧'
  if (store.filterType === 'ai') return '暂无AI生成灵感，点击下方 AI 灵感生成器创作吧'
  const map = { '对白灵感': '暂无对白灵感', '场景描写': '暂无场景描写', '细节设定': '暂无细节设定', '参考资料': '暂无参考资料' }
  return map[store.filterType] || '暂无数据'
})

const emptyButtonText = computed(() => {
  if (store.filterType === 'all') return '+ 记录灵感'
  if (store.filterType === 'ai') return '🤖 使用 AI 生成'
  const map = { '对白灵感': '+ 新增对白', '场景描写': '+ 新增场景', '细节设定': '+ 新增设定', '参考资料': '+ 新增资料' }
  return map[store.filterType] || '+ 记录灵感'
})

// ─── ActionSheet ───
const showSheet = ref(false)
const sheetItem = ref(null)
const sheetMode = ref('detail') // 'detail' | 'edit'

function openActionSheet(item) {
  console.log('[InspirationSection] card clicked:', item?.id, item?.type)
  sheetItem.value = { ...item, tagsArray: parseTags(item.tags) }
  sheetMode.value = 'detail'
  showSheet.value = true
  // 设置编辑表单
  editForm.content = item.content || ''
  editForm.type = item.type || '对白灵感'
  editForm.chapterId = item.chapterId || ''
  editForm.isHighlight = !!item.isHighlight
  editForm.tagsArray = [...parseTags(item.tags)]
}

function closeSheet() {
  showSheet.value = false
  sheetItem.value = null
}

function switchToEdit() {
  sheetMode.value = 'edit'
}

// ─── 编辑表单 ───
const editForm = reactive({
  content: '',
  type: '对白灵感',
  chapterId: '',
  isHighlight: false,
  tagsArray: []
})
const editTagInput = ref('')

function addEditTag() {
  const tag = editTagInput.value.trim()
  if (tag && !editForm.tagsArray.includes(tag)) {
    editForm.tagsArray.push(tag)
  }
  editTagInput.value = ''
}

function removeEditTag(index) {
  editForm.tagsArray.splice(index, 1)
}

function handleEditSave() {
  if (!editForm.content.trim() || !sheetItem.value) return
  const chapter = chapters.value.find(c => c.id === editForm.chapterId)
  store.updateInspiration(sheetItem.value.id, {
    content: editForm.content.trim(),
    type: editForm.type,
    chapterId: editForm.chapterId || null,
    chapterName: chapter ? (chapter.title || '第' + (chapter.sortOrder || '?') + '章') : '',
    isHighlight: editForm.isHighlight,
    tags: editForm.tagsArray.join(', ')
  })
  closeSheet()
}

// ─── 标记已使用 ───
function handleToggleUsed() {
  if (!sheetItem.value) return
  store.toggleUsed(sheetItem.value.id)
  sheetItem.value.isUsed = !sheetItem.value.isUsed
}

// ─── 复制内容 ───
function copyContent() {
  if (!sheetItem.value) return
  navigator.clipboard.writeText(sheetItem.value.content).then(() => {
    // 简短反馈
  }).catch(() => {})
  closeSheet()
}

// ─── 删除 ───
function handleDelete() {
  if (!sheetItem.value) return
  if (!confirm('确定要删除这条灵感吗？此操作不可恢复！')) return
  store.deleteInspiration(sheetItem.value.id)
  closeSheet()
}

// ─── 新建灵感弹窗 ───
const showCreateDialog = ref(false)
const createForm = reactive({
  type: 'all',
  content: '',
  chapterId: '',
  isHighlight: false,
  tagsArray: []
})
const createTagInput = ref('')

const createDialogTitle = computed(() => {
  if (createForm.type === 'all') return '记录灵感'
  const map = { '对白灵感': '新增对白灵感', '场景描写': '新增场景描写', '细节设定': '新增细节设定', '参考资料': '新增参考资料' }
  return map[createForm.type] || '记录灵感'
})

function openCreateDialog() {
  // 自动填充当前筛选类型
  createForm.type = store.filterType !== 'all' ? store.filterType : '对白灵感'
  createForm.content = ''
  createForm.chapterId = ''
  createForm.isHighlight = false
  createForm.tagsArray = []
  showCreateDialog.value = true
}

function addCreateTag() {
  const tag = createTagInput.value.trim()
  if (tag && !createForm.tagsArray.includes(tag)) {
    createForm.tagsArray.push(tag)
  }
  createTagInput.value = ''
}

function removeCreateTag(index) {
  createForm.tagsArray.splice(index, 1)
}

function handleCreate() {
  if (!createForm.content.trim()) return
  const chapter = chapters.value.find(c => c.id === createForm.chapterId)
  store.createInspiration({
    type: createForm.type !== 'all' ? createForm.type : '对白灵感',
    content: createForm.content.trim(),
    chapterId: createForm.chapterId || null,
    chapterName: chapter ? (chapter.title || '第' + (chapter.sortOrder || '?') + '章') : '',
    isHighlight: createForm.isHighlight,
    tags: createForm.tagsArray.join(', ')
  })
  showCreateDialog.value = false
}

// 暴露给父组件
defineExpose({ openCreateDialog })

// ─── AI 生成器 ───
const aiKeywords = ref('')
const aiPanelCollapsed = ref(false)

async function handleGenerate() {
  if (!aiKeywords.trim() || store.aiGenerating) return
  if (!requireVip('角色灵感生成')) return
  await store.generateAI(aiKeywords.value)
}

function acceptResult(result) {
  const item = store.acceptAiResult(result)
  if (item) {
    // 高亮新卡片
    item._isNew = true
    setTimeout(() => { item._isNew = false }, 2000)
  }
}

// ─── 初始化 ───
onMounted(() => {
  if (props.projectId) {
    store.init(props.projectId, chapters.value)
  }
})

watch(() => props.projectId, (pid) => {
  if (pid) {
    store.init(pid, chapters.value)
  }
})
</script>

<style scoped>
/* ═══════════════════════════════════════
   灵感素材模块 · 赤焰主题
   ═══════════════════════════════════════ */

.inspiration-section {
  animation: fadeSlideIn 0.35s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0;
}

/* ─── 统计条 ─── */
.inspiration-stats-bar {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(26, 24, 21, 0.03);
  border: 1px solid rgba(217, 119, 6, 0.10);
  border-radius: 12px;
  backdrop-filter: blur(8px);
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
  font-family: var(--font-display);
  line-height: 1.2;
}

.stat-num.accent { color: #f59e0b; }
.stat-num.green { color: #10b981; }
.stat-num.purple { color: #8b5cf6; }
.stat-num.gray { color: #6b7280; }
.stat-num.ai-color { color: #ec4899; }

.stat-label {
  display: block;
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 1px;
}

/* ─── 筛选标签栏 ─── */
.filter-tab-bar {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.filter-tab-btn {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.filter-tab-btn:hover {
  border-color: rgba(217, 119, 6, 0.3);
  color: var(--accent);
  background: rgba(217, 119, 6, 0.04);
}

.filter-tab-btn.active {
  background: linear-gradient(135deg, rgba(217, 119, 6, 0.12), rgba(239, 68, 68, 0.08));
  border-color: rgba(217, 119, 6, 0.3);
  color: #d97706;
  box-shadow: 0 0 12px rgba(217, 119, 6, 0.08);
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  font-size: 10px;
  font-weight: 700;
  background: rgba(217, 119, 6, 0.12);
  color: #d97706;
}

.filter-tab-btn.active .tab-count {
  background: rgba(217, 119, 6, 0.2);
}

/* ─── 搜索与排序栏 ─── */
.search-sort-bar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(26, 24, 21, 0.03);
  border: 1px solid var(--border);
  border-radius: 10px;
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: rgba(217, 119, 6, 0.3);
  box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.06);
}

.search-icon {
  font-size: 14px;
  opacity: 0.5;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: var(--text);
  font-family: inherit;
}

.search-input::placeholder { color: var(--text-muted); }

.search-clear {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-muted);
  padding: 2px;
}

.search-clear:hover { color: var(--text); }

.sort-select-wrapper {
  position: relative;
}

.sort-select {
  padding: 6px 28px 6px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  background: rgba(26, 24, 21, 0.02);
  outline: none;
  cursor: pointer;
  font-family: inherit;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%239c9690' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
}

.sort-select:focus {
  border-color: rgba(217, 119, 6, 0.3);
}

/* ─── 灵感卡片列表 ─── */
.inspiration-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.inspiration-card {
  position: relative;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(217, 119, 6, 0.10);
  border-radius: 12px;
  padding: 14px 14px 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  overflow: hidden;
}

.inspiration-card:hover {
  transform: translateY(-2px);
  border-color: rgba(217, 119, 6, 0.25);
  box-shadow: 0 4px 20px rgba(217, 119, 6, 0.08), 0 0 30px rgba(217, 119, 6, 0.04);
}

.inspiration-card:active {
  transform: scale(0.98);
}

.inspiration-card.is-highlight {
  border-color: rgba(217, 119, 6, 0.3);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 20px rgba(217, 119, 6, 0.06);
}

.inspiration-card.is-used {
  opacity: 0.65;
}

.inspiration-card.new-item {
  animation: cardGlow 2s ease;
}

@keyframes cardGlow {
  0% { box-shadow: 0 0 0 rgba(217, 119, 6, 0); }
  30% { box-shadow: 0 0 24px rgba(217, 119, 6, 0.2); border-color: rgba(217, 119, 6, 0.4); }
  100% { box-shadow: 0 0 0 rgba(217, 119, 6, 0); }
}

.card-type-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 10px;
  margin-bottom: 8px;
}

.card-type-badge.type-dialogue {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}

.card-type-badge.type-scene {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.card-type-badge.type-detail {
  background: rgba(139, 92, 246, 0.12);
  color: #8b5cf6;
}

.card-type-badge.type-reference {
  background: rgba(107, 114, 128, 0.12);
  color: #6b7280;
}

.card-content {
  font-size: 13px;
  color: var(--text);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  line-clamp: 3;
  overflow: hidden;
  margin-bottom: 8px;
  word-break: break-word;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-time {
  font-size: 10px;
  color: var(--text-muted);
}

.card-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.card-tag {
  font-size: 9px;
  padding: 1px 7px;
  border-radius: 8px;
  background: rgba(217, 119, 6, 0.06);
  color: var(--text-muted);
}

.used-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 9px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(107, 114, 128, 0.15);
  color: var(--text-muted);
}

.highlight-dot {
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, #f59e0b, #ef4444);
  border-radius: 3px 0 0 3px;
}

/* ─── 空状态 ─── */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px dashed var(--border);
  border-radius: 16px;
}

.empty-icon-wrap {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.empty-text {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.btn-empty-add {
  padding: 8px 24px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  background: linear-gradient(135deg, #d97706, #f59e0b);
  color: #fff;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-empty-add:hover {
  box-shadow: 0 4px 16px rgba(217, 119, 6, 0.25);
  transform: translateY(-1px);
}

/* ─── AI 灵感生成器 ─── */
.ai-generator-panel {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(217, 119, 6, 0.10);
  border-radius: 12px;
  overflow: hidden;
}

.ai-gen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
}

.ai-gen-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
}

.ai-gen-toggle {
  font-size: 11px;
  color: var(--accent);
}

.ai-gen-body {
  padding: 0 14px 14px;
}

.ai-results {
  margin-bottom: 12px;
}

.ai-results-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 0 8px;
  scroll-snap-type: x mandatory;
}

.ai-results-scroll::-webkit-scrollbar { height: 3px; }
.ai-results-scroll::-webkit-scrollbar-thumb { background: var(--border-hover); border-radius: 2px; }

.ai-result-card {
  min-width: 220px;
  max-width: 260px;
  flex-shrink: 0;
  scroll-snap-align: start;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.9);
  position: relative;
}

.ai-result-card.type-dialogue { border-left: 3px solid #f59e0b; }
.ai-result-card.type-scene { border-left: 3px solid #10b981; }
.ai-result-card.type-detail { border-left: 3px solid #8b5cf6; }

.ai-result-type {
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-secondary);
}

.ai-result-content {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text);
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  line-clamp: 4;
  overflow: hidden;
}

.btn-accept {
  padding: 4px 14px;
  border: none;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  background: linear-gradient(135deg, #d97706, #f59e0b);
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-accept:hover {
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.25);
}

.ai-actions {
  text-align: center;
}

.btn-regenerate {
  padding: 6px 20px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-regenerate:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.btn-regenerate:disabled { opacity: 0.4; cursor: not-allowed; }

.ai-loading {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.skeleton-card {
  flex: 1;
  padding: 12px;
  border-radius: 10px;
  background: rgba(26, 24, 21, 0.03);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.skeleton-line {
  height: 10px;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(26,24,21,0.04), rgba(26,24,21,0.10), rgba(26,24,21,0.04));
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line.w-20 { width: 60px; }
.skeleton-line.w-3\/4 { width: 75%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.ai-input-row {
  display: flex;
  gap: 8px;
}

.ai-input {
  flex: 1;
  padding: 8px 14px;
  border: 1.5px solid var(--border);
  border-radius: 10px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.2s;
}

.ai-input:focus {
  border-color: rgba(217, 119, 6, 0.3);
  box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.06);
}

.btn-ai-generate {
  padding: 8px 20px;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  background: linear-gradient(135deg, #f97316, #dc2626);
  color: #fff;
  cursor: pointer;
  transition: all 0.25s;
  white-space: nowrap;
  font-family: inherit;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
}

.btn-ai-generate:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.25);
  transform: translateY(-1px);
}

.btn-ai-generate:disabled { opacity: 0.5; cursor: not-allowed; }

/* ─── ActionSheet ─── */
.sheet-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.sheet-panel {
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  background: #fff;
  border-radius: 20px 20px 0 0;
  padding: 8px 20px 24px;
  overflow-y: auto;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.08);
}

.sheet-handle {
  width: 36px;
  height: 4px;
  background: var(--border);
  border-radius: 2px;
  margin: 0 auto 12px;
}

.sheet-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 12px;
}

.sheet-body {
  margin-bottom: 16px;
}

.detail-content {
  font-size: 16px;
  line-height: 1.7;
  color: var(--text);
  padding: 12px;
  background: rgba(26, 24, 21, 0.02);
  border-radius: 10px;
  margin-bottom: 12px;
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-row {
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.meta-label {
  color: var(--text-muted);
  min-width: 60px;
  flex-shrink: 0;
}

.meta-value {
  color: var(--text-secondary);
  font-weight: 500;
}

.meta-value.used { color: #10b981; }
.meta-value.pending { color: #d97706; }

.tags-value {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.meta-tag {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 8px;
  background: rgba(217, 119, 6, 0.06);
  color: var(--text-secondary);
}

.sheet-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}

.sheet-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  background: #fff;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
}

.sheet-btn:hover { border-color: var(--border-hover); background: rgba(26, 24, 21, 0.02); }

.btn-edit { color: var(--accent); border-color: rgba(217, 119, 6, 0.2); }
.btn-edit:hover { background: rgba(217, 119, 6, 0.04); }

.btn-used { color: #10b981; border-color: rgba(16, 185, 129, 0.2); }
.btn-used:hover { background: rgba(16, 185, 129, 0.04); }

.btn-unused { color: #d97706; border-color: rgba(217, 119, 6, 0.2); }
.btn-unused:hover { background: rgba(217, 119, 6, 0.04); }

.btn-delete { color: #ef4444; border-color: rgba(239, 68, 68, 0.15); }
.btn-delete:hover { background: rgba(239, 68, 68, 0.04); }

.btn-save {
  background: linear-gradient(135deg, #d97706, #f59e0b);
  color: #fff;
  border: none;
}

.btn-save:hover { box-shadow: 0 2px 8px rgba(217, 119, 6, 0.2); }

.btn-cancel-sheet {
  color: var(--text-muted);
  border-color: transparent;
}

/* ─── 弹窗 ─── */
.insp-modal .form-group {
  margin-bottom: 14px;
}

.insp-modal .form-group label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.insp-modal .form-group .required { color: #ef4444; }

.insp-modal .form-group input,
.insp-modal .form-group select,
.insp-modal .form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1.5px solid var(--border);
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  background: rgba(255, 255, 255, 0.8);
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.insp-modal .form-group input:focus,
.insp-modal .form-group select:focus,
.insp-modal .form-group textarea:focus {
  border-color: rgba(217, 119, 6, 0.3);
}

.insp-modal .form-group textarea {
  min-height: 70px;
  resize: vertical;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 2px;
}

.form-row-inline {
  margin-bottom: 14px;
}

.toggle-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.toggle-switch-sm {
  position: relative;
  width: 36px;
  height: 20px;
  display: inline-block;
}

.toggle-switch-sm input { display: none; }

.toggle-slider-sm {
  position: absolute;
  inset: 0;
  border-radius: 10px;
  background: var(--border);
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-slider-sm::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.toggle-switch-sm input:checked + .toggle-slider-sm {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}

.toggle-switch-sm input:checked + .toggle-slider-sm::before {
  transform: translateX(16px);
}

/* ─── 标签输入 ─── */
.tag-input-wrap {
  border: 1.5px solid var(--border);
  border-radius: 8px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.8);
}

.tag-list {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.edit-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(217, 119, 6, 0.08);
  color: var(--accent);
  font-weight: 500;
}

.tag-remove {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 10px;
  color: var(--text-muted);
  padding: 0;
  line-height: 1;
}

.tag-remove:hover { color: #ef4444; }

.tag-add-row {
  display: flex;
  gap: 4px;
}

.tag-add-row input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 12px;
  background: transparent;
  font-family: inherit;
  color: var(--text);
}

.tag-add-row input::placeholder { color: var(--text-muted); }

.tag-add-btn {
  padding: 2px 8px;
  border: none;
  border-radius: 4px;
  background: rgba(217, 119, 6, 0.1);
  color: var(--accent);
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
}

.tag-add-btn:hover { background: rgba(217, 119, 6, 0.2); }

/* ─── 过渡动画 ─── */
.action-sheet-enter-active { transition: all 0.3s ease-out; }
.action-sheet-leave-active { transition: all 0.25s ease-in; }
.action-sheet-enter-from .sheet-panel { transform: translateY(100%); }
.action-sheet-leave-to .sheet-panel { transform: translateY(100%); }
.action-sheet-enter-from { opacity: 0; }
.action-sheet-leave-to { opacity: 0; }

.dialog-fade-enter-active { transition: all 0.2s ease-out; }
.dialog-fade-leave-active { transition: all 0.15s ease-in; }
.dialog-fade-enter-from { opacity: 0; }
.dialog-fade-leave-to { opacity: 0; }

/* ─── 响应式 ─── */
@media (max-width: 820px) {
  .inspiration-list { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .inspiration-stats-bar { gap: 4px; padding: 8px 10px; }
  .stat-num { font-size: 16px; }
  .search-sort-bar { flex-direction: column; }
  .sort-select-wrapper { align-self: flex-end; }
  .ai-results-scroll { gap: 8px; }
  .ai-result-card { min-width: 180px; }
}
</style>