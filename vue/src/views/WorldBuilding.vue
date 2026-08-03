<template>
  <div class="world-building-page" :class="{ 'inline-mode': inlineMode }">
    <!-- ─── 顶部导航栏（仅非内联模式显示） ─── -->
    <div v-if="!inlineMode" class="top-nav-bar">
      <button class="back-to-work-btn" @click="goBackToWorkspace" title="返回写作工作台">
        ← 返回作品
      </button>
      <span class="nav-separator">|</span>
      <span class="current-project-name">📖 {{ projectTitle }}</span>
    </div>

    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">🌍 世界观构建</h1>
        <p class="page-subtitle">构建你的奇幻世界，从地理到信仰，从历史到文明</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="openCreateDialog">
          + 新建设定
        </button>
      </div>
    </div>

    <div class="page-content">
      <div class="main-area">
        <div v-if="!selectedCategoryId && !selectedSettingId" class="category-view">
          <div class="search-bar">
            <div class="search-input-wrap">
              <span class="search-icon">🔍</span>
              <input
                v-model="store.searchQuery"
                type="text"
                class="search-input"
                placeholder="搜索设定名称或内容..."
              />
            </div>
            <div class="filter-group">
              <select v-model="store.statusFilter" class="filter-select">
                <option value="all">全部状态</option>
                <option value="draft">草稿</option>
                <option value="completed">已完成</option>
                <option value="needs_work">待完善</option>
              </select>
              <select v-model="store.sortBy" class="filter-select">
                <option value="updatedAt">按更新时间</option>
                <option value="createdAt">按创建时间</option>
                <option value="name">按名称</option>
              </select>
            </div>
          </div>

          <div class="categories-grid">
            <div
              v-for="(cat, index) in store.categories"
              :key="cat.id"
              class="category-card"
              :style="{ '--cat-color': cat.color, '--delay': index * 0.05 + 's' }"
              :class="{ active: selectedCategoryId === cat.id }"
              @click="selectCategory(cat.id)"
            >
              <div class="category-header">
                <span class="category-icon">{{ cat.icon }}</span>
                <span class="category-name">{{ cat.name }}</span>
              </div>
              <p class="category-desc">{{ cat.description }}</p>

              <div class="category-entries">
                <div
                  v-for="setting in getCategoryPreviewSettings(cat.id)"
                  :key="setting.id"
                  class="entry-preview"
                  @click.stop="selectSetting(setting.id)"
                >
                  <span class="entry-title">{{ setting.name }}</span>
                  <span class="entry-status" :style="{ color: getStatusColor(setting.status) }">
                    {{ getStatusIcon(setting.status) }}
                  </span>
                </div>
                <div v-if="getCategoryEntryCount(cat.id) > 3" class="entry-more" @click.stop="selectCategory(cat.id)">
                  还有 {{ getCategoryEntryCount(cat.id) - 3 }} 项... 点击查看
                </div>
                <div v-if="getCategoryEntryCount(cat.id) === 0" class="entry-empty">
                  暂无设定 ✨
                </div>
              </div>

              <div class="category-footer">
                <span class="entry-count">共 {{ getCategoryEntryCount(cat.id) }} 项</span>
                <div class="category-stats">
                  <span class="stat completed" title="已完成">{{ getCategoryStat(cat.id, 'completed') }}</span>
                  <span class="stat draft" title="草稿">{{ getCategoryStat(cat.id, 'draft') }}</span>
                  <span class="stat needs-work" title="待完善">{{ getCategoryStat(cat.id, 'needsWork') }}</span>
                </div>
              </div>


            </div>
          </div>
        </div>

        <div v-else-if="selectedCategoryId && !selectedSettingId" class="category-detail-view">
          <div class="detail-header">
            <button class="back-btn" @click="store.clearSelection">
              ← 返回分类
            </button>
            <div class="detail-breadcrumb">
              <span @click="goBackToWorkspace" class="breadcrumb-item">{{ projectTitle }}</span>
              <span class="breadcrumb-sep">/</span>
              <span @click="store.clearSelection" class="breadcrumb-item">世界观</span>
              <span class="breadcrumb-sep">/</span>
              <span class="breadcrumb-item current">{{ getCategoryName(selectedCategoryId) }}</span>
            </div>
          </div>
          <div class="category-settings-list">
            <div
              v-for="setting in getCategoryAllSettings(selectedCategoryId)"
              :key="setting.id"
              class="setting-list-item"
              @click="selectSetting(setting.id)"
            >
              <span class="setting-status" :style="{ color: getStatusColor(setting.status) }">
                {{ getStatusIcon(setting.status) }}
              </span>
              <span class="setting-name">{{ setting.name }}</span>
              <span class="setting-status-text">{{ getStatusLabel(setting.status) }}</span>
            </div>
            <div v-if="getCategoryEntryCount(selectedCategoryId) === 0" class="empty-category">
              暂无设定 ✨
            </div>
          </div>
        </div>

        <div v-else class="detail-view">
          <div class="detail-header">
            <button class="back-btn" @click="store.clearSelection">
              ← 返回
            </button>
            <div class="detail-breadcrumb">
              <span @click="goBackToWorkspace" class="breadcrumb-item">{{ projectTitle }}</span>
              <span class="breadcrumb-sep">/</span>
              <span @click="store.clearSelection" class="breadcrumb-item">世界观</span>
              <span class="breadcrumb-sep">/</span>
              <span @click="store.clearSelection" class="breadcrumb-item">{{ getCategoryName(selectedSetting?.category) }}</span>
              <span class="breadcrumb-sep">/</span>
              <span class="breadcrumb-item current">{{ selectedSetting?.name }}</span>
            </div>
          </div>

          <div class="detail-content" v-if="selectedSetting">
            <div class="detail-main">
              <div class="detail-title-row">
                <input
                  v-if="editingTitle"
                  v-model="editingName"
                  class="detail-title-input"
                  @blur="saveTitleEdit"
                  @keyup.enter="saveTitleEdit"
                  ref="titleInput"
                />
                <h2 v-else class="detail-title" @dblclick="startTitleEdit">
                  {{ selectedSetting.name }}
                </h2>
                <div class="status-selector">
                  <button
                    v-for="(config, status) in STATUS_CONFIG"
                    :key="status"
                    class="status-btn"
                    :class="{ active: selectedSetting.status === status }"
                    :style="selectedSetting.status === status ? { background: config.bg, color: config.color } : {}"
                    @click="updateStatus(status)"
                  >
                    {{ config.icon }} {{ config.label }}
                  </button>
                </div>
              </div>

              <div class="detail-body">
                <div
                  class="content-editor"
                  :class="{ editing: editingContent }"
                  @dblclick="startContentEdit"
                >
                  <textarea
                    v-if="editingContent"
                    v-model="editingContentText"
                    class="content-textarea"
                    rows="15"
                    ref="contentTextarea"
                    @blur="saveContentEdit"
                  ></textarea>
                  <div v-else class="content-display" v-html="formatContent(selectedSetting.content)"></div>
                  <span v-if="!selectedSetting.content && !editingContent" class="content-placeholder">
                    双击此处编辑内容...
                  </span>
                </div>
              </div>

              <div class="detail-relations" v-if="relatedSettings.length > 0">
                <h4 class="relations-title">📌 关联设定</h4>
                <div class="relations-list">
                  <div
                    v-for="related in relatedSettings"
                    :key="related.id"
                    class="relation-item"
                    @click="selectSetting(related.id)"
                  >
                    <span class="relation-icon">{{ getCategoryIcon(related.category) }}</span>
                    <span class="relation-name">{{ related.name }}</span>
                    <span class="relation-category">{{ getCategoryName(related.category) }}</span>
                  </div>
                </div>
              </div>

              <div class="detail-meta">
                <span>创建于 {{ formatDate(selectedSetting.createdAt) }}</span>
                <span>更新于 {{ formatDate(selectedSetting.updatedAt) }}</span>
              </div>
            </div>

            <div class="detail-actions">
              <button class="btn btn-outline" @click="openRelationDialog">
                🔗 关联设定
              </button>
              <button class="btn btn-edit" @click="openEditDialog">
                ✏️ 编辑详情
              </button>
              <button class="btn btn-delete" @click="confirmDelete">
                🗑️ 删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="toast-fade">
        <div v-if="toast.show" class="toast" :class="`toast-${toast.type}`">
          <span>{{ toast.message }}</span>
        </div>
      </Transition>

      <Transition name="modal-fade">
        <div v-if="showCreateDialog" class="modal-overlay" @click.self="closeCreateDialog">
          <div class="modal-content" :class="{ 'modal-inline': inlineMode }">
            <h3 class="modal-title">{{ editingSetting ? '编辑设定' : '新建设定' }}</h3>
            <div class="modal-body">
              <div class="form-group">
                <label class="form-label">所属分类</label>
                <select v-model="formData.category" class="form-select">
                  <option v-for="cat in store.categories" :key="cat.id" :value="cat.id">
                    {{ cat.icon }} {{ cat.name }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">设定名称</label>
                <input
                  v-model="formData.name"
                  class="form-input"
                  placeholder="如：赤红裂谷、火焰神殿..."
                  @input="checkNameConflict"
                />
                <span v-if="nameConflict" class="form-error">名称已存在</span>
              </div>
              <div class="form-group">
                <label class="form-label">详细描述</label>
                <textarea
                  v-model="formData.content"
                  class="form-textarea"
                  rows="6"
                  placeholder="描述这个设定的详细信息..."
                ></textarea>
              </div>
              <div class="form-group">
                <label class="form-label">状态</label>
                <div class="status-radio-group">
                  <label
                    v-for="(config, status) in STATUS_CONFIG"
                    :key="status"
                    class="status-radio"
                    :class="{ active: formData.status === status }"
                  >
                    <input type="radio" v-model="formData.status" :value="status" />
                    {{ config.icon }} {{ config.label }}
                  </label>
                </div>
              </div>
            </div>
            <div class="modal-actions">
              <button class="btn btn-ghost" @click="closeCreateDialog">取消</button>
              <button class="btn btn-primary" @click="saveSetting" :disabled="!formData.name || nameConflict">
                {{ editingSetting ? '保存修改' : '创建' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="modal-fade">
        <div v-if="showRelationDialog" class="modal-overlay" @click.self="closeRelationDialog">
          <div class="modal-content" :class="{ 'modal-inline': inlineMode }">
            <h3 class="modal-title">🔗 关联设定</h3>
            <div class="modal-body">
              <p class="relation-hint">选择要关联到「{{ selectedSetting?.name }}」的其他设定</p>
              <div class="relation-options">
                <label
                  v-for="setting in availableForRelation"
                  :key="setting.id"
                  class="relation-option"
                  :class="{ selected: formData.relatedSettings.includes(setting.id) }"
                >
                  <input
                    type="checkbox"
                    :value="setting.id"
                    v-model="formData.relatedSettings"
                  />
                  <span class="option-icon">{{ getCategoryIcon(setting.category) }}</span>
                  <span class="option-name">{{ setting.name }}</span>
                  <span class="option-category">{{ getCategoryName(setting.category) }}</span>
                </label>
              </div>
            </div>
            <div class="modal-actions">
              <button class="btn btn-ghost" @click="closeRelationDialog">取消</button>
              <button class="btn btn-primary" @click="saveRelations">保存关联</button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="modal-fade">
        <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
          <div class="modal-content modal-confirm" :class="{ 'modal-inline': inlineMode }">
            <h3 class="modal-title">⚠️ 删除确认</h3>
            <div class="modal-body">
              <p>确定要删除「{{ selectedSetting?.name }}」吗？此操作不可恢复。</p>
            </div>
            <div class="modal-actions">
              <button class="btn btn-ghost" @click="showDeleteConfirm = false">取消</button>
              <button class="btn btn-danger" @click="doDelete">确认删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <div v-if="store.loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <span>加载世界观设定中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorldBuildingStore, STATUS_CONFIG } from '@/stores/world-building'
import { useNovelStore } from '@/stores/novel'
import { storeToRefs } from 'pinia'

const props = defineProps({
  inlineMode: { type: Boolean, default: false }
})

const route = useRoute()
const router = useRouter()
const store = useWorldBuildingStore()
const novelStore = useNovelStore()

// ─── 响应式状态（使用 storeToRefs 确保正确追踪 Pinia 响应性） ───
const { selectedCategoryId, selectedSettingId } = storeToRefs(store)
const selectedSetting = computed(() => store.selectedSetting)

// ─── 监听 novelStore.worldSettings 变化，同步到本地 store ───
// 无条件同步（含空数组）：编辑界面保存/删除全部设定后，本地 store 也要清空/更新，
// 避免旧数据残留导致页面显示与数据库不一致
watch(() => novelStore.worldSettings, (newSettings) => {
  if (Array.isArray(newSettings)) {
    // 当 novelStore.worldSettings 更新时，同步到 worldBuildingStore
    store.settings.splice(0, store.settings.length, ...newSettings.map(s => ({
      ...s,
      status: s.status || 'draft',
      relatedSettings: s.relatedSettings || []
    })))
  }
}, { deep: true })

// ─── 项目信息与导航 ───
const projectTitle = computed(() => {
  const title = novelStore.currentProject?.title
  return title || '未命名作品'
})

function goBackToWorkspace() {
  const pid = route.params.projectId || novelStore.currentProjectId
  if (pid) {
    router.push(`/my-works/${pid}?tool=world`)
  } else {
    router.push('/my-works')
  }
}

// ─── ESC 键逐级返回 ───
function handleKeydown(e) {
  if (e.key === 'Escape') {
    goBackToWorkspace()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const editingTitle = ref(false)
const editingName = ref('')
const editingContent = ref(false)
const editingContentText = ref('')
const titleInput = ref(null)
const contentTextarea = ref(null)

const toast = reactive({ show: false, message: '', type: 'success' })
const showToast = (message, type = 'success') => {
  toast.message = message
  toast.type = type
  toast.show = true
  setTimeout(() => { toast.show = false }, 2500)
}

const showCreateDialog = ref(false)
const showRelationDialog = ref(false)
const showDeleteConfirm = ref(false)
const editingSetting = ref(null)
const nameConflict = ref(false)

const formData = reactive({
  category: 'geography',
  name: '',
  content: '',
  status: 'draft',
  relatedSettings: []
})

const getCategoryIcon = (categoryId) => {
  const cat = store.categories.find(c => c.id === categoryId)
  return cat ? cat.icon : '📋'
}

const getCategoryName = (categoryId) => {
  const cat = store.categories.find(c => c.id === categoryId)
  return cat ? cat.name : categoryId
}

const getCategoryColor = (categoryId) => {
  // 优先从动态分类列表获取颜色（支持 AI 生成的中文分类）
  const cat = store.categories.find(c => c.id === categoryId)
  if (cat && cat.color) return cat.color
  const colors = {
    geography: '#3b82f6',
    history: '#f59e0b',
    culture: '#ec4899',
    magic: '#8b5cf6',
    technology: '#14b8a6',
    races: '#a855f7',
    religion: '#f97316',
    politics: '#22c55e'
  }
  return colors[categoryId] || '#6366f1'
}

const getCategoryPreviewSettings = (categoryId) => {
  return store.settingsByCategory[categoryId]?.slice(0, 3) || []
}

const getCategoryAllSettings = (categoryId) => {
  return store.settingsByCategory[categoryId] || []
}

const getCategoryEntryCount = (categoryId) => {
  return store.categoryStats[categoryId]?.total || 0
}

const getCategoryStat = (categoryId, stat) => {
  const stats = store.categoryStats[categoryId]
  if (!stats) return 0
  if (stat === 'completed') return stats.completed
  if (stat === 'draft') return stats.draft
  if (stat === 'needsWork') return stats.needsWork
  return 0
}

const getStatusColor = (status) => STATUS_CONFIG[status]?.color || '#94a3b8'
const getStatusIcon = (status) => STATUS_CONFIG[status]?.icon || '○'
const getStatusLabel = (status) => STATUS_CONFIG[status]?.label || status

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br/>')
}

const relatedSettings = computed(() => {
  if (!selectedSetting.value) return []
  return store.getRelatedSettings(selectedSetting.value)
})

const availableForRelation = computed(() => {
  if (!selectedSetting.value) return store.settings
  return store.settings.filter(s => s.id !== selectedSetting.value.id)
})

const selectCategory = (categoryId) => {
  store.selectCategory(categoryId)
}

const selectSetting = (settingId) => {
  store.selectSetting(settingId)
}

const startTitleEdit = () => {
  if (!selectedSetting.value) return
  editingName.value = selectedSetting.value.name
  editingTitle.value = true
  nextTick(() => titleInput.value?.focus())
}

const saveTitleEdit = async () => {
  if (!selectedSetting.value || !editingName.value.trim()) {
    editingTitle.value = false
    return
  }
  const success = await store.updateSetting(selectedSetting.value.id, { name: editingName.value.trim() })
  if (success) {
    showToast('标题已更新')
  }
  editingTitle.value = false
}

const startContentEdit = () => {
  if (!selectedSetting.value) return
  editingContentText.value = selectedSetting.value.content || ''
  editingContent.value = true
  nextTick(() => contentTextarea.value?.focus())
}

const saveContentEdit = async () => {
  if (!selectedSetting.value) {
    editingContent.value = false
    return
  }
  const success = await store.updateSetting(selectedSetting.value.id, { content: editingContentText.value })
  if (success) {
    showToast('内容已保存')
  }
  editingContent.value = false
}

const updateStatus = async (status) => {
  if (!selectedSetting.value) return
  const success = await store.updateSetting(selectedSetting.value.id, { status })
  if (success) {
    showToast('状态已更新为：' + STATUS_CONFIG[status].label)
  }
}

const checkNameConflict = () => {
  const name = formData.name.trim().toLowerCase()
  nameConflict.value = store.settings.some(
    s => s.name.toLowerCase() === name && s.id !== editingSetting.value?.id
  )
}

const openCreateDialog = (categoryId = null) => {
  editingSetting.value = null
  formData.category = categoryId || store.selectedCategoryId || 'geography'
  formData.name = ''
  formData.content = ''
  formData.status = 'draft'
  formData.relatedSettings = []
  nameConflict.value = false
  showCreateDialog.value = true
}

const openEditDialog = () => {
  if (!selectedSetting.value) return
  editingSetting.value = selectedSetting.value
  formData.category = selectedSetting.value.category
  formData.name = selectedSetting.value.name
  formData.content = selectedSetting.value.content || ''
  formData.status = selectedSetting.value.status || 'draft'
  formData.relatedSettings = [...(selectedSetting.value.relatedSettings || [])]
  nameConflict.value = false
  showCreateDialog.value = true
}

const closeCreateDialog = () => {
  showCreateDialog.value = false
  editingSetting.value = null
}

const saveSetting = async () => {
  if (!formData.name.trim() || nameConflict.value) return

  if (editingSetting.value) {
    const success = await store.updateSetting(editingSetting.value.id, {
      name: formData.name.trim(),
      category: formData.category,
      content: formData.content,
      status: formData.status
    })
    if (success) {
      showToast('设定已更新')
      closeCreateDialog()
    } else {
      showToast('更新失败', 'error')
    }
  } else {
    const result = await store.createSetting({
      name: formData.name.trim(),
      category: formData.category,
      content: formData.content,
      status: formData.status
    })
    if (result) {
      showToast('新设定已创建')
      closeCreateDialog()
    } else {
      showToast('创建失败', 'error')
    }
  }
}

const openRelationDialog = () => {
  if (!selectedSetting.value) return
  formData.relatedSettings = [...(selectedSetting.value.relatedSettings || [])]
  showRelationDialog.value = true
}

const closeRelationDialog = () => {
  showRelationDialog.value = false
}

const saveRelations = async () => {
  if (!selectedSetting.value) return
  const success = await store.updateSetting(selectedSetting.value.id, {
    relatedSettings: formData.relatedSettings
  })
  if (success) {
    showToast('关联已保存')
  }
  closeRelationDialog()
}

const confirmDelete = () => {
  showDeleteConfirm.value = true
}

const doDelete = async () => {
  if (!selectedSetting.value) return
  const success = await store.deleteSetting(selectedSetting.value.id)
  if (success) {
    showToast('设定已删除')
    store.clearSelection()
  } else {
    showToast('删除失败', 'error')
  }
  showDeleteConfirm.value = false
}

onMounted(async () => {
  const projectId = novelStore.currentProjectId
  if (projectId) {
    await store.fetchSettings(projectId)
  }
})
</script>

<style scoped>
.world-building-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 1.5rem;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%);
  color: #e8e3dc;
  animation: fadeSlideIn 0.4s ease;
}

/* ─── 内联模式（嵌入 Workspace 面板） ─── */
.world-building-page.inline-mode {
  height: auto;
  min-height: 0;
  padding: 0;
  background: transparent;
  color: inherit;
}

.world-building-page.inline-mode .page-header {
  margin-bottom: 1rem;
}

.world-building-page.inline-mode .page-header .page-title {
  font-size: 1.3rem;
  background: none;
  -webkit-text-fill-color: initial;
  color: #1A1A2E;
  background-clip: unset;
}

.world-building-page.inline-mode .page-header .page-subtitle {
  color: #4A4A5A;
}

.world-building-page.inline-mode .page-content {
  flex: 1;
  background: transparent;
}

.world-building-page.inline-mode .search-input {
  background: #fff;
  border-color: #e2e8f0;
  color: #1A1A2E;
}

.world-building-page.inline-mode .search-input::placeholder {
  color: #b8b0a8;
}

.world-building-page.inline-mode .search-input:focus {
  border-color: #f97316;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.08);
}

.world-building-page.inline-mode .filter-select {
  background: #fff;
  border-color: #e2e8f0;
  color: #4A4A5A;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 24 24' fill='none' stroke='%239c9690' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
}

.world-building-page.inline-mode .filter-select:focus {
  border-color: #f97316;
}

.world-building-page.inline-mode .category-card {
  background: #fff;
  border-color: #ece9e3;
  border-left-width: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.world-building-page.inline-mode .category-card::before {
  opacity: 0.5;
}

.world-building-page.inline-mode .category-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-left-width: 5px;
}

.world-building-page.inline-mode .category-name {
  color: #1A1A2E;
}

.world-building-page.inline-mode .category-desc {
  color: #6B6560;
}

.world-building-page.inline-mode .entry-preview {
  background: #faf8f5;
}

.world-building-page.inline-mode .entry-preview:hover {
  background: #f3efe8;
}

.world-building-page.inline-mode .entry-title {
  color: #4A4A5A;
}

.world-building-page.inline-mode .entry-empty {
  color: #b8b0a8;
  border-color: #e2e8f0;
}

.world-building-page.inline-mode .entry-more {
  color: #9c9690;
  cursor: pointer;
  transition: color 0.2s ease;
  border-radius: 6px;
}

.world-building-page.inline-mode .entry-more:hover {
  color: #d97706;
  background: rgba(217, 119, 6, 0.08);
}

.world-building-page.inline-mode .category-footer {
  border-top-color: #f0ece6;
}

.world-building-page.inline-mode .add-entry-btn {
  background: rgba(249, 115, 22, 0.08);
  border-color: rgba(249, 115, 22, 0.2);
  color: #f97316;
}

.world-building-page.inline-mode .add-entry-btn:hover {
  background: rgba(249, 115, 22, 0.15);
}

.world-building-page.inline-mode .category-detail-view {
  background: transparent;
}

.world-building-page.inline-mode .category-settings-list {
  gap: 0.4rem;
}

.world-building-page.inline-mode .setting-list-item {
  background: #fff;
  border-color: #e2e8f0;
}

.world-building-page.inline-mode .setting-list-item:hover {
  background: #faf8f5;
  border-color: #d97706;
  transform: translateX(4px);
}

.world-building-page.inline-mode .setting-name {
  color: #1A1A2E;
}

.world-building-page.inline-mode .setting-status-text {
  color: #9c9690;
}

.world-building-page.inline-mode .empty-category {
  color: #9c9690;
}

.world-building-page.inline-mode .back-btn {
  background: #fff;
  border-color: #e2e8f0;
  color: #4A4A5A;
}

.world-building-page.inline-mode .back-btn:hover {
  background: #faf8f5;
  border-color: #d97706;
  color: #d97706;
}

.top-nav-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 0.5rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid rgba(232, 227, 220, 0.08);
  flex-shrink: 0;
}

.back-to-work-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 1px solid rgba(217, 119, 6, 0.25);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #d97706;
  background: rgba(217, 119, 6, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.back-to-work-btn:hover {
  background: rgba(217, 119, 6, 0.12);
  border-color: rgba(217, 119, 6, 0.4);
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.1);
  transform: translateX(-2px);
}

.nav-separator {
  color: rgba(232, 227, 220, 0.2);
  font-size: 14px;
}

.current-project-name {
  font-size: 13px;
  font-weight: 500;
  color: rgba(232, 227, 220, 0.6);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  flex-shrink: 0;
}

.page-title {
  font-family: var(--font-display);
  font-size: 1.8rem;
  font-weight: 700;
  background: linear-gradient(135deg, #f97316, #fbbf24, #f97316);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 0.3rem 0;
}

.page-subtitle {
  font-size: 0.85rem;
  color: #8a857e;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.6rem;
}

.page-content {
  flex: 1;
  display: flex;
  gap: 1.2rem;
  overflow: hidden;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-bar {
  display: flex;
  gap: 0.8rem;
  margin-bottom: 1.2rem;
  flex-shrink: 0;
}

.search-input-wrap {
  flex: 1;
  position: relative;
}

.search-icon {
  position: absolute;
  left: 0.8rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.9rem;
  opacity: 0.4;
}

.search-input {
  width: 100%;
  padding: 0.6rem 0.8rem 0.6rem 2.4rem;
  border: 1.5px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  color: #e8e3dc;
  font-size: 0.85rem;
  outline: none;
  transition: all 0.2s;
}

.search-input::placeholder { color: rgba(232, 227, 220, 0.35); }

.search-input:focus {
  border-color: #f97316;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.08);
}

.filter-group {
  display: flex;
  gap: 0.5rem;
}

.filter-select {
  padding: 0.5rem 0.8rem;
  border: 1.5px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  color: #d4cec6;
  font-size: 0.8rem;
  outline: none;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 24 24' fill='none' stroke='%239c9690' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  padding-right: 28px;
}

.filter-select:focus {
  border-color: #f97316;
}

.category-view {
  flex: 1;
  overflow-y: auto;
}

.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.25rem;
}

.category-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 1.25rem 1.25rem 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  animation: fadeInUp var(--delay, 0s) ease forwards;
  opacity: 0;
  /* 左侧色条 */
  border-left: 4px solid var(--cat-color, #6366f1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  /* 统一卡片高度 */
  min-height: 280px;
  max-height: 280px;
  display: flex;
  flex-direction: column;
}

.category-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 14px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--cat-color, #6366f1) 6%, transparent), transparent 50%);
  pointer-events: none;
}

.category-card:hover {
  border-color: var(--cat-color, #6366f1);
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.25), 0 0 40px color-mix(in srgb, var(--cat-color, #6366f1) 8%, transparent);
  border-left-width: 5px;
}

.category-card.active {
  border-color: var(--cat-color, #6366f1);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 24px color-mix(in srgb, var(--cat-color, #6366f1) 20%, transparent);
}

.category-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.4rem;
  position: relative;
  z-index: 1;
}

.category-icon {
  font-size: 1.4rem;
}

.category-name {
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 700;
  color: #fff;
}

.category-desc {
  font-size: 0.78rem;
  color: rgba(232, 227, 220, 0.5);
  margin: 0 0 0.8rem 0;
  position: relative;
  z-index: 1;
}

.category-entries {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-bottom: 0.8rem;
  min-height: 76px;
  max-height: 120px;
  overflow-y: auto;
  position: relative;
  z-index: 1;
  flex: 1;
}

.entry-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.45rem 0.6rem;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
  border: 1px solid transparent;
}

.entry-preview:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.06);
}

.entry-title {
  font-size: 0.8rem;
  color: rgba(232, 227, 220, 0.8);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.entry-status {
  font-size: 0.7rem;
  flex-shrink: 0;
}

.entry-more {
  font-size: 0.72rem;
  color: rgba(232, 227, 220, 0.35);
  text-align: center;
  padding: 0.45rem;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s ease;
  border-radius: 6px;
}

.entry-more:hover {
  color: rgba(232, 227, 220, 0.7);
  background: rgba(255, 255, 255, 0.05);
}

.entry-empty {
  font-size: 0.78rem;
  color: rgba(232, 227, 220, 0.25);
  text-align: center;
  padding: 1.2rem 0.5rem;
  border: 1px dashed rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  font-weight: 500;
}

.category-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.7rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  position: relative;
  z-index: 1;
  margin-top: auto;
}

.entry-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  font-size: 0.7rem;
  font-weight: 700;
  color: #fff;
  background: var(--cat-color, #6366f1);
  border-radius: 10px;
  opacity: 0.85;
  transition: opacity 0.2s;
}

.category-card:hover .entry-count {
  opacity: 1;
}

.category-stats {
  display: flex;
  gap: 0.35rem;
}

.stat {
  font-size: 0.65rem;
  padding: 2px 7px;
  border-radius: 5px;
  font-weight: 700;
}

.stat.completed { background: rgba(16, 185, 129, 0.2); color: #6ee7b7; }
.stat.draft { background: rgba(148, 163, 184, 0.2); color: #cbd5e1; }
.stat.needs-work { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }

.category-detail-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.category-settings-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.setting-list-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.setting-list-item:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.15);
  transform: translateX(4px);
}

.setting-status {
  font-size: 0.9rem;
  flex-shrink: 0;
}

.setting-name {
  flex: 1;
  font-size: 0.88rem;
  font-weight: 500;
  color: rgba(232, 227, 220, 0.85);
}

.setting-status-text {
  font-size: 0.72rem;
  color: rgba(232, 227, 220, 0.4);
  flex-shrink: 0;
}

.empty-category {
  text-align: center;
  padding: 2rem;
  color: rgba(232, 227, 220, 0.35);
  font-size: 0.88rem;
}

.detail-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.2rem;
  flex-shrink: 0;
}

.back-btn {
  padding: 0.4rem 0.9rem;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius);
  color: #4A6A8A;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.back-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #2c3e50;
}

.detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.82rem;
  flex-wrap: wrap;
}

.breadcrumb-item {
  color: #5A6A7A;
  cursor: pointer;
  transition: color 0.15s;
}

.breadcrumb-item:hover {
  color: #2c3e50;
  text-decoration: underline;
}

.breadcrumb-item.current {
  color: #2C3E50;
  font-weight: 700;
}

.breadcrumb-sep {
  color: #cbd5e1;
  font-size: 0.75rem;
}

.detail-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #FFFFFF;
  border: 1px solid #eef0f2;
  border-radius: 12px;
  padding: 1.75rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.detail-main {
  flex: 1;
  overflow-y: auto;
}

.detail-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.25rem;
  gap: 1rem;
  border-bottom: 1px solid #eef0f2;
  padding-bottom: 1rem;
}

.detail-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  color: #1A1A2E;
  margin: 0;
  cursor: text;
  line-height: 1.3;
}

.detail-title:hover {
  color: #f97316;
}

.detail-title-input {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  background: #f8fafc;
  border: 1.5px solid #4F46E5;
  border-radius: var(--radius);
  color: #1A1A2E;
  padding: 0.3rem 0.6rem;
  outline: none;
  flex: 1;
}

.status-selector {
  display: flex;
  gap: 0.4rem;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.status-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.35rem 0.75rem;
  border-radius: 20px;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
  color: #64748b;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  white-space: nowrap;
}

.status-btn:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.status-btn.active {
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.detail-body {
  margin-bottom: 1.5rem;
}

.content-editor {
  position: relative;
  min-height: 200px;
  background: #fafbfc;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  padding: 1.2rem;
  transition: border-color 0.2s;
}

.content-editor:hover {
  border-color: #d0d5dc;
}

.content-editor.editing {
  border-color: #4F46E5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.08);
}

.content-display {
  font-size: 1rem;
  line-height: 1.8;
  color: #2D3748;
  white-space: pre-wrap;
}

.content-textarea {
  width: 100%;
  min-height: 180px;
  background: transparent;
  border: none;
  color: #2D3748;
  font-size: 1rem;
  line-height: 1.8;
  font-family: inherit;
  resize: vertical;
  outline: none;
}

.content-placeholder {
  position: absolute;
  top: 1.2rem;
  left: 1.2rem;
  color: #a0aec0;
  font-size: 0.9rem;
  pointer-events: none;
}

.detail-relations {
  margin-bottom: 1.25rem;
  padding-top: 1.25rem;
  border-top: 1px solid #eef0f2;
}

.relations-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: #5A6A7A;
  margin: 0 0 0.8rem 0;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.relations-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.relation-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.75rem;
  background: #f0f4ff;
  border: 1px solid #dce5f5;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.relation-item:hover {
  background: #e4ecff;
  border-color: #b8c9f0;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.08);
}

.relation-icon {
  font-size: 0.8rem;
  flex-shrink: 0;
}

.relation-name {
  font-size: 0.8rem;
  font-weight: 600;
  color: #2D3748;
}

.relation-category {
  font-size: 0.65rem;
  color: #718096;
  background: rgba(255,255,255,0.6);
  padding: 1px 5px;
  border-radius: 4px;
}

.detail-meta {
  display: flex;
  gap: 1.5rem;
  font-size: 0.78rem;
  color: #718096;
  padding-top: 1rem;
  border-top: 1px solid #eef0f2;
}

.detail-meta span {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}

.detail-actions {
  display: flex;
  gap: 0.6rem;
  padding-top: 1.25rem;
  flex-shrink: 0;
  border-top: 1px solid #eef0f2;
  margin-top: 0.25rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.btn-primary {
  background: linear-gradient(135deg, #f97316, #fb923c);
  color: #fff;
  font-weight: 700;
  padding: 0.55rem 1.3rem;
  border: none;
  box-shadow: 0 2px 8px rgba(249, 115, 22, 0.2);
}

.btn-primary:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(249, 115, 22, 0.35);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-outline {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #c4bdb5;
  font-weight: 600;
}

.btn-outline:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
  color: #e8e3dc;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.btn-ghost {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.1);
  color: #8a857e;
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #d4cec6;
}

.btn-danger {
  background: #dc2626;
  color: #fff;
}

.btn-danger:hover {
  background: #b91c1c;
}

.btn-edit {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1.1rem;
  background: #4F46E5;
  color: #fff;
  border: none;
  border-radius: var(--radius);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.btn-edit:hover {
  background: #4338ca;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.3);
  transform: translateY(-1px);
}

.btn-delete {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1.1rem;
  background: transparent;
  color: #DC2626;
  border: 1.5px solid #fca5a5;
  border-radius: var(--radius);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-delete:hover {
  background: #FEF2F2;
  border-color: #DC2626;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.1);
}

.toast {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  padding: 0.8rem 1.2rem;
  border-radius: var(--radius);
  font-size: 0.82rem;
  font-weight: 600;
  z-index: 9999;
  box-shadow: var(--shadow-lg);
}

.toast-success {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.toast-error {
  background: rgba(220, 38, 38, 0.15);
  color: #ef4444;
  border: 1px solid rgba(220, 38, 38, 0.3);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9998;
}

.modal-content {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 1.5rem;
  width: 480px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.modal-confirm {
  width: 380px;
}

.modal-title {
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 1.2rem 0;
}

.modal-body {
  margin-bottom: 1.2rem;
}

.modal-body p {
  color: rgba(232, 227, 220, 0.6);
  margin: 0;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 600;
  color: #8a857e;
  margin-bottom: 0.4rem;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 0.6rem 0.8rem;
  background: rgba(26, 26, 46, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  color: #e8e3dc;
  font-size: 0.85rem;
  outline: none;
  transition: all 0.2s;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: #f97316;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.form-error {
  display: block;
  font-size: 0.72rem;
  color: #ef4444;
  margin-top: 0.3rem;
}

.form-select {
  cursor: pointer;
}

.status-radio-group {
  display: flex;
  gap: 0.5rem;
}

.status-radio {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.4rem 0.8rem;
  background: rgba(26, 26, 46, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  font-size: 0.78rem;
  color: rgba(232, 227, 220, 0.5);
  cursor: pointer;
  transition: all 0.15s ease;
}

.status-radio input {
  display: none;
}

.status-radio.active {
  border-color: #f97316;
  color: #f97316;
}

.relation-hint {
  font-size: 0.85rem;
  color: #8a857e;
  margin-bottom: 1rem;
}

.relation-options {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 300px;
  overflow-y: auto;
}

.relation-option {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 0.8rem;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.15s ease;
}

.relation-option input {
  display: none;
}

.relation-option:hover {
  background: rgba(255, 255, 255, 0.06);
}

.relation-option.selected {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
}

.option-icon {
  font-size: 1rem;
}

.option-name {
  flex: 1;
  font-size: 0.85rem;
  color: #e8e3dc;
}

.option-category {
  font-size: 0.7rem;
  color: #6b6560;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-content,
.modal-fade-leave-to .modal-content {
  transform: scale(0.95) translateY(-10px);
}

.loading-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(26, 26, 46, 0.9);
  z-index: 9997;
  gap: 0.8rem;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #f97316;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-overlay span {
  font-size: 0.82rem;
  color: #8a857e;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ─── 内联模式弹窗背景色统一 ─── */
:deep(.modal-content.modal-inline) {
  background: #ffffff;
  color: #1A1A2E;
}

:deep(.modal-content.modal-inline .modal-title) {
  color: #1A1A2E;
}

:deep(.modal-content.modal-inline .form-label) {
  color: #4A4A5A;
}

:deep(.modal-content.modal-inline .form-input),
:deep(.modal-content.modal-inline .form-select),
:deep(.modal-content.modal-inline .form-textarea) {
  background: #f8f9fa;
  border-color: #e2e8f0;
  color: #1A1A2E;
}

:deep(.modal-content.modal-inline .form-input:focus),
:deep(.modal-content.modal-inline .form-select:focus),
:deep(.modal-content.modal-inline .form-textarea:focus) {
  border-color: #f97316;
}

:deep(.modal-content.modal-inline .form-error) {
  color: #ef4444;
}

:deep(.modal-content.modal-inline .status-radio) {
  background: #f8f9fa;
  border-color: #e2e8f0;
  color: #6B6560;
}

:deep(.modal-content.modal-inline .status-radio.active) {
  border-color: #f97316;
  color: #f97316;
}

:deep(.modal-content.modal-inline .relation-hint) {
  color: #6B6560;
}

:deep(.modal-content.modal-inline .relation-option) {
  background: #f8f9fa;
  border-color: #e2e8f0;
}

:deep(.modal-content.modal-inline .relation-option.selected) {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
}

:deep(.modal-content.modal-inline .option-name) {
  color: #1A1A2E;
}

:deep(.modal-content.modal-inline .option-category) {
  color: #9c9690;
}

:deep(.modal-content.modal-inline .modal-body p) {
  color: #6B6560;
}

:deep(.modal-content.modal-inline .btn-ghost) {
  border-color: #e2e8f0;
  color: #6B6560;
}

:deep(.modal-content.modal-inline .btn-ghost:hover) {
  background: #f1f5f9;
  color: #1A1A2E;
}


@media (max-width: 1024px) {
  .page-content {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .world-building-page {
    padding: 1rem;
  }

  .page-header {
    flex-direction: column;
    gap: 1rem;
  }

  .search-bar {
    flex-direction: column;
  }

  .categories-grid {
    grid-template-columns: 1fr;
  }
}
</style>