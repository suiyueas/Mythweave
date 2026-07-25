<template>
  <div class="ai-config-page">
    <div class="section-header">
      <div>
        <h2 class="section-title">AI 策略配置中心</h2>
        <p class="section-subtitle">DeepSeek v4 Pro · 实时掌控模型参数与写作风格</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-outline btn-sm" @click="refreshUsage" :disabled="usageLoading">
          <span :class="{ 'spin': usageLoading }">🔄</span> 刷新用量
        </button>
        <button class="btn btn-primary btn-sm" @click="saveAllConfig" :disabled="saving">
          <span v-if="saving" class="loading-dot"></span>
          <span v-else>💾</span> 保存配置
        </button>
      </div>
    </div>

    <div class="config-layout">
      <div class="config-left">
        <div class="card params-card">
          <div class="card-header">
            <h3 class="card-title">🎚️ 模型参数</h3>
            <span class="card-badge">实时调整</span>
          </div>

          <div class="param-group">
            <div class="param-header">
              <span class="param-label">温度 (Temperature)</span>
              <div class="param-value-display">
                <input
                  type="number"
                  v-model.number="temperatureDisplay"
                  min="0"
                  max="1"
                  step="0.05"
                  class="param-input"
                />
                <span class="param-unit">/ 1.0</span>
              </div>
            </div>
            <div class="slider-container">
              <input
                type="range"
                v-model.number="temperatureDisplay"
                min="0"
                max="1"
                step="0.05"
                class="param-slider slider-temperature"
              />
              <div class="slider-labels">
                <span>保守</span>
                <span>平衡</span>
                <span>创意</span>
              </div>
            </div>
          </div>

          <div class="param-group">
            <div class="param-header">
              <span class="param-label">Top-P</span>
              <div class="param-value-display">
                <input
                  type="number"
                  v-model.number="topPDisplay"
                  min="0.1"
                  max="1"
                  step="0.05"
                  class="param-input"
                />
                <span class="param-unit">/ 1.0</span>
              </div>
            </div>
            <div class="slider-container">
              <input
                type="range"
                v-model.number="topPDisplay"
                min="0.1"
                max="1"
                step="0.05"
                class="param-slider slider-topp"
              />
              <div class="slider-labels">
                <span>精确</span>
                <span>多样</span>
              </div>
            </div>
          </div>

          <div class="param-group">
            <div class="param-header">
              <span class="param-label">Max Tokens</span>
              <div class="param-value-display">
                <input
                  type="number"
                  v-model.number="maxTokens"
                  min="256"
                  max="8192"
                  step="256"
                  class="param-input param-input-lg"
                />
                <span class="param-unit">tokens</span>
              </div>
            </div>
            <div class="quick-buttons">
              <button
                v-for="val in [512, 1024, 2048, 4096]"
                :key="val"
                class="quick-btn"
                :class="{ active: maxTokens === val }"
                @click="maxTokens = val"
              >
                {{ val }}
              </button>
            </div>
          </div>
        </div>

        <div class="card usage-card">
          <div class="card-header">
            <h3 class="card-title">📊 Token 用量统计</h3>
            <span class="card-badge badge-emerald">实时</span>
          </div>
          <div class="usage-grid">
            <div class="usage-stat usage-stat-primary">
              <div class="usage-value">{{ formatNumber(usageData.totalTokens) }}</div>
              <div class="usage-label">总 Token</div>
            </div>
            <div class="usage-stat">
              <div class="usage-value usage-value-cost">¥{{ usageData.estimatedCost.toFixed(2) }}</div>
              <div class="usage-label">预估费用</div>
            </div>
            <div class="usage-stat">
              <div class="usage-value">{{ usageData.apiCalls }}</div>
              <div class="usage-label">API 调用</div>
            </div>
            <div class="usage-stat usage-stat-ring">
              <div class="ring-progress">
                <svg viewBox="0 0 36 36" class="ring-svg">
                  <path
                    class="ring-bg"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                  <path
                    class="ring-fill"
                    :stroke-dasharray="`${usageData.cacheHitRate}, 100`"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                </svg>
                <div class="ring-text">{{ usageData.cacheHitRate }}%</div>
              </div>
              <div class="usage-label">缓存命中</div>
            </div>
          </div>
        </div>
      </div>

      <div class="config-right">
        <div class="card presets-card">
          <div class="card-header">
            <h3 class="card-title">🎨 写作风格预设</h3>
            <button class="btn btn-sm btn-accent" @click="showPresetDialog = true">
              + 创建新预设
            </button>
          </div>
          <div class="presets-grid">
            <div
              v-for="preset in presets"
              :key="preset.id"
              class="preset-card"
              :class="{ active: currentPresetId === preset.id }"
              @click="selectPreset(preset.id)"
            >
              <div class="preset-header">
                <span class="preset-name">{{ preset.name }}</span>
                <div class="preset-actions" v-if="!preset.isDefault">
                  <button class="preset-menu-btn" @click.stop="openPresetMenu(preset, $event)">···</button>
                  <div v-if="activePresetMenu === preset.id" class="preset-menu">
                    <button @click.stop="editPreset(preset)">编辑</button>
                    <button @click.stop="confirmDeletePreset(preset)" class="danger">删除</button>
                  </div>
                </div>
              </div>
              <div class="preset-desc">{{ preset.description }}</div>
              <div class="preset-meta">
                <span class="preset-param">T: {{ preset.temperature }}</span>
                <span class="preset-param">P: {{ preset.topP }}</span>
                <span class="preset-param">M: {{ preset.maxTokens }}</span>
              </div>
              <div v-if="currentPresetId === preset.id" class="preset-active-tag">
                <span class="tag tag-amber">当前使用</span>
              </div>
            </div>
          </div>
        </div>

        <div class="card prompt-card">
          <div class="card-header">
            <h3 class="card-title">📝 自定义 Prompt 模板</h3>
            <div class="prompt-actions">
              <button class="btn btn-sm btn-ghost" @click="resetTemplate">恢复默认</button>
              <button class="btn btn-sm btn-accent" @click="saveTemplate" :disabled="savingTemplate">
                <span v-if="savingTemplate" class="loading-dot"></span>
                <span v-else>✓</span> 保存模板
              </button>
            </div>
          </div>
          <div class="prompt-editor">
            <textarea
              v-model="customPrompt"
              class="prompt-textarea"
              rows="10"
              placeholder="输入自定义 Prompt 模板..."
            ></textarea>
          </div>
          <div class="prompt-placeholders">
            <span class="placeholders-label">可用占位符：</span>
            <span
              v-for="p in placeholders"
              :key="p.key"
              class="prompt-var"
              :title="p.desc"
              @click="insertPlaceholder(p.key)"
            >
              {{ p.key }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="toast-fade">
        <div v-if="toast.show" class="toast" :class="`toast-${toast.type}`">
          <span class="toast-icon">{{ toast.type === 'success' ? '✓' : '✕' }}</span>
          <span class="toast-message">{{ toast.message }}</span>
        </div>
      </Transition>

      <Transition name="modal-fade">
        <div v-if="showPresetDialog" class="modal-overlay" @click.self="closePresetDialog">
          <div class="modal-content">
            <h3 class="modal-title">{{ editingPreset ? '编辑预设' : '创建新预设' }}</h3>
            <div class="modal-body">
              <div class="form-group">
                <label class="form-label">预设名称</label>
                <input v-model="presetForm.name" class="form-input" placeholder="如：悬疑紧张风格" />
              </div>
              <div class="form-group">
                <label class="form-label">简短描述</label>
                <input v-model="presetForm.description" class="form-input" placeholder="如：短段落 · 高密度冲突" />
              </div>
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">温度</label>
                  <input v-model.number="presetForm.temperature" type="number" min="0" max="1" step="0.05" class="form-input" />
                </div>
                <div class="form-group">
                  <label class="form-label">Top-P</label>
                  <input v-model.number="presetForm.topP" type="number" min="0.1" max="1" step="0.05" class="form-input" />
                </div>
                <div class="form-group">
                  <label class="form-label">MaxTokens</label>
                  <input v-model.number="presetForm.maxTokens" type="number" min="256" max="8192" step="256" class="form-input" />
                </div>
              </div>
            </div>
            <div class="modal-actions">
              <button class="btn btn-ghost" @click="closePresetDialog">取消</button>
              <button class="btn btn-primary" @click="savePresetForm">{{ editingPreset ? '保存修改' : '创建预设' }}</button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="modal-fade">
        <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
          <div class="modal-content modal-confirm">
            <h3 class="modal-title">⚠️ 删除确认</h3>
            <div class="modal-body">
              <p>确定要删除预设「{{ presetToDelete?.name }}」吗？此操作不可恢复。</p>
            </div>
            <div class="modal-actions">
              <button class="btn btn-ghost" @click="showDeleteConfirm = false">取消</button>
              <button class="btn btn-danger" @click="doDeletePreset">确认删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <span>加载配置中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useAiConfigStore } from '@/stores/ai-config'
import { useNovelStore } from '@/stores/novel'

const store = useAiConfigStore()
const novelStore = useNovelStore()

const loading = computed(() => store.loading)
const saving = computed(() => store.saving)
const temperatureDisplay = computed({
  get: () => store.temperature,
  set: (v) => { store.temperature = Math.round(v * 100) / 100 }
})
const topPDisplay = computed({
  get: () => store.topP,
  set: (v) => { store.topP = Math.round(v * 100) / 100 }
})
const maxTokens = computed({
  get: () => store.maxTokens,
  set: (v) => { store.maxTokens = Math.max(256, Math.min(8192, v)) }
})
const currentPresetId = computed(() => store.currentPresetId)
const presets = computed(() => store.presets)
const customPrompt = computed({
  get: () => store.customPrompt,
  set: (v) => { store.customPrompt = v }
})
const usageData = computed(() => store.usageData)
const usageLoading = computed(() => store.usageLoading)

const toast = reactive({ show: false, message: '', type: 'success' })
const showToast = (message, type = 'success') => {
  toast.message = message
  toast.type = type
  toast.show = true
  setTimeout(() => { toast.show = false }, 2500)
}

const savingTemplate = ref(false)
const showPresetDialog = ref(false)
const editingPreset = ref(null)
const presetForm = reactive({ name: '', description: '', temperature: 0.7, topP: 0.9, maxTokens: 4096 })
const activePresetMenu = ref(null)
const showDeleteConfirm = ref(false)
const presetToDelete = ref(null)

const placeholders = [
  { key: '{context}', desc: '上下文信息' },
  { key: '{character}', desc: '人物信息' },
  { key: '{style}', desc: '写作风格' },
  { key: '{chapter}', desc: '章节信息' },
  { key: '{plot}', desc: '当前情节走向' },
  { key: '{tone}', desc: '语气语调' }
]

const formatNumber = (num) => {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

const selectPreset = (id) => {
  store.selectPreset(id)
  showToast('已切换到：' + store.currentPreset.name)
}

const refreshUsage = async () => {
  const projectId = novelStore.currentProjectId
  if (projectId) {
    await store.fetchUsage(projectId)
    showToast('用量数据已刷新')
  }
}

const saveAllConfig = async () => {
  const projectId = novelStore.currentProjectId
  if (!projectId) {
    showToast('请先选择作品', 'error')
    return
  }
  const success = await store.saveConfig(projectId)
  if (success) {
    showToast('配置已保存')
  } else {
    showToast(store.error || '保存失败', 'error')
  }
}

const saveTemplate = async () => {
  savingTemplate.value = true
  const projectId = novelStore.currentProjectId
  if (projectId) {
    const success = await store.saveConfig(projectId)
    if (success) {
      showToast('模板已保存')
    } else {
      showToast('模板保存失败', 'error')
    }
  } else {
    showToast('模板已缓存（请先选择作品）')
  }
  savingTemplate.value = false
}

const resetTemplate = () => {
  store.resetPromptTemplate()
  showToast('已恢复默认模板')
}

const insertPlaceholder = (key) => {
  store.customPrompt += key
}

const openPresetMenu = (preset, event) => {
  event.stopPropagation()
  activePresetMenu.value = activePresetMenu.value === preset.id ? null : preset.id
}

const closePresetMenu = () => {
  activePresetMenu.value = null
}

const editPreset = (preset) => {
  editingPreset.value = preset
  presetForm.name = preset.name
  presetForm.description = preset.description
  presetForm.temperature = preset.temperature
  presetForm.topP = preset.topP
  presetForm.maxTokens = preset.maxTokens
  showPresetDialog.value = true
  activePresetMenu.value = null
}

const confirmDeletePreset = (preset) => {
  presetToDelete.value = preset
  showDeleteConfirm.value = true
  activePresetMenu.value = null
}

const doDeletePreset = async () => {
  if (presetToDelete.value) {
    await store.deletePreset(presetToDelete.value.id)
    showToast('预设已删除')
    showDeleteConfirm.value = false
    presetToDelete.value = null
  }
}

const closePresetDialog = () => {
  showPresetDialog.value = false
  editingPreset.value = null
  presetForm.name = ''
  presetForm.description = ''
  presetForm.temperature = 0.7
  presetForm.topP = 0.9
  presetForm.maxTokens = 4096
}

const savePresetForm = async () => {
  if (!presetForm.name.trim()) {
    showToast('请输入预设名称', 'error')
    return
  }
  if (editingPreset.value) {
    await store.updatePreset(editingPreset.value.id, { ...presetForm })
    showToast('预设已更新')
  } else {
    await store.createPreset({ ...presetForm })
    showToast('新预设已创建')
  }
  closePresetDialog()
}

onMounted(async () => {
  store.loadFromStorage()
  const projectId = novelStore.currentProjectId
  if (projectId) {
    await store.fetchConfig(projectId)
    await store.fetchUsage(projectId)
  }
  document.addEventListener('click', closePresetMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closePresetMenu)
})
</script>

<style scoped>
.ai-config-page {
  animation: fadeSlideIn 0.4s ease;
  padding: 1.5rem;
  max-width: 1400px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.config-layout {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 1.2rem;
  align-items: start;
}

.config-left,
.config-right {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 1.2rem;
  transition: all 0.2s ease;
}

.card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.card-title {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.card-badge {
  font-size: 0.62rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  background: var(--accent-glow);
  color: var(--accent);
}

.badge-emerald {
  background: var(--emerald-subtle);
  color: var(--emerald);
}

.param-group {
  margin-bottom: 1.2rem;
}

.param-group:last-child {
  margin-bottom: 0;
}

.param-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.param-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text);
}

.param-value-display {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.param-input {
  width: 60px;
  padding: 0.3rem 0.5rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--accent);
  text-align: center;
  background: var(--bg);
}

.param-input:focus {
  outline: none;
  border-color: var(--accent);
}

.param-input-lg {
  width: 80px;
}

.param-unit {
  font-size: 0.7rem;
  color: var(--text-muted);
}

.slider-container {
  position: relative;
}

.param-slider {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: var(--border);
  outline: none;
  -webkit-appearance: none;
  appearance: none;
  cursor: pointer;
}

.param-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--accent);
  cursor: pointer;
  box-shadow: 0 2px 8px var(--accent-glow);
  transition: transform 0.15s ease;
}

.param-slider::-webkit-slider-thumb:hover {
  transform: scale(1.15);
}

.slider-temperature {
  background: linear-gradient(to right, #3b82f6 0%, var(--accent) 50%, #ef4444 100%);
}

.slider-topp {
  background: linear-gradient(to right, var(--border) 0%, var(--accent) 100%);
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 0.3rem;
  font-size: 0.6rem;
  color: var(--text-muted);
}

.quick-buttons {
  display: flex;
  gap: 0.4rem;
  margin-top: 0.5rem;
}

.quick-btn {
  padding: 0.3rem 0.7rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.quick-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-glow);
}

.quick-btn.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.usage-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.8rem;
}

.usage-stat {
  text-align: center;
  padding: 0.8rem;
  background: #faf8f5;
  border-radius: var(--radius);
}

.usage-stat-primary {
  grid-column: 1 / -1;
  background: linear-gradient(135deg, var(--accent-glow) 0%, transparent 100%);
}

.usage-value {
  font-family: var(--font-display);
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--accent);
}

.usage-value-cost {
  color: var(--teal);
}

.usage-label {
  font-size: 0.65rem;
  color: var(--text-muted);
  margin-top: 0.2rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.usage-stat-ring {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: transparent;
  padding: 0.5rem;
}

.ring-progress {
  position: relative;
  width: 60px;
  height: 60px;
}

.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: var(--border);
  stroke-width: 3;
}

.ring-fill {
  fill: none;
  stroke: var(--emerald);
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dasharray 0.6s ease;
}

.ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--emerald);
}

.presets-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.8rem;
}

.preset-card {
  position: relative;
  padding: 0.9rem;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--card);
}

.preset-card:hover {
  border-color: var(--border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.preset-card.active {
  border-color: var(--accent);
  background: linear-gradient(135deg, var(--accent-glow) 0%, transparent 100%);
  box-shadow: 0 0 20px var(--accent-glow);
}

.preset-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.3rem;
}

.preset-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text);
}

.preset-desc {
  font-size: 0.68rem;
  color: var(--text-muted);
  margin-bottom: 0.5rem;
  line-height: 1.4;
}

.preset-meta {
  display: flex;
  gap: 0.5rem;
}

.preset-param {
  font-size: 0.6rem;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg);
  padding: 2px 6px;
  border-radius: 4px;
}

.preset-active-tag {
  position: absolute;
  top: -8px;
  right: 8px;
}

.preset-menu-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
  color: var(--text-muted);
  padding: 0 0.3rem;
}

.preset-menu-btn:hover {
  color: var(--text);
}

.preset-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  z-index: 100;
  min-width: 80px;
  overflow: hidden;
}

.preset-menu button {
  display: block;
  width: 100%;
  padding: 0.5rem 0.8rem;
  text-align: left;
  border: none;
  background: transparent;
  font-size: 0.75rem;
  color: var(--text);
  cursor: pointer;
}

.preset-menu button:hover {
  background: var(--accent-glow);
}

.preset-menu button.danger {
  color: var(--rose);
}

.prompt-actions {
  display: flex;
  gap: 0.4rem;
}

.prompt-editor {
  margin-bottom: 0.8rem;
}

.prompt-textarea {
  width: 100%;
  min-height: 180px;
  padding: 0.8rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-family: var(--font-mono);
  font-size: 0.75rem;
  line-height: 1.6;
  background: #faf8f5;
  color: var(--text);
  resize: vertical;
  outline: none;
  transition: border-color 0.15s ease;
}

.prompt-textarea:focus {
  border-color: var(--accent);
  background: var(--card);
}

.prompt-placeholders {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
}

.placeholders-label {
  font-size: 0.68rem;
  color: var(--text-muted);
  font-weight: 600;
}

.prompt-var {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 0.65rem;
  font-weight: 600;
  font-family: var(--font-mono);
  background: var(--accent-glow);
  color: var(--accent);
  cursor: pointer;
  transition: all 0.15s ease;
}

.prompt-var:hover {
  background: var(--accent);
  color: #fff;
}

.btn-accent {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.btn-accent:hover {
  background: #c26704;
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-secondary);
}

.btn-ghost:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.btn-danger {
  background: var(--rose);
  color: #fff;
  border-color: var(--rose);
}

.btn-danger:hover {
  background: #9f1239;
}

.loading-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 0.8s ease-in-out infinite;
}

.spin {
  display: inline-block;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

.toast {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.8rem 1.2rem;
  border-radius: var(--radius);
  font-size: 0.82rem;
  font-weight: 600;
  box-shadow: var(--shadow-lg);
  z-index: 9999;
}

.toast-success {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.toast-error {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.toast-icon {
  font-size: 1rem;
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
  background: rgba(26, 24, 21, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9998;
}

.modal-content {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  width: 420px;
  max-width: 90vw;
  box-shadow: var(--shadow-lg);
}

.modal-confirm {
  width: 360px;
}

.modal-title {
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 1rem;
  color: var(--text);
}

.modal-body {
  margin-bottom: 1.2rem;
}

.modal-body p {
  font-size: 0.85rem;
  color: var(--text-secondary);
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
}

.form-group {
  margin-bottom: 0.8rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 0.3rem;
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.7rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 0.82rem;
  background: var(--bg);
  color: var(--text);
  outline: none;
}

.form-input:focus {
  border-color: var(--accent);
}

.form-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.6rem;
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
  background: rgba(250, 247, 242, 0.9);
  z-index: 9997;
  gap: 0.8rem;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-overlay span {
  font-size: 0.82rem;
  color: var(--text-muted);
}

@media (max-width: 1024px) {
  .config-layout {
    grid-template-columns: 1fr;
  }

  .presets-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .ai-config-page {
    padding: 1rem;
  }

  .usage-grid {
    grid-template-columns: 1fr 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>