<template>
  <div class="step-world">
    <div class="step-header">
      <h2>🌍 世界观构建</h2>
      <p class="step-desc">AI 将根据你的故事灵感，构建完整的世界设定</p>
    </div>

    <!-- 未生成状态 -->
    <div v-if="status === 'pending'" class="step-body pending">
      <div class="input-area">
        <div class="form-group">
          <label>额外方向（可选）</label>
          <textarea
            v-model="direction"
            class="form-textarea"
            placeholder="比如：希望世界偏向东方仙侠风格，力量体系以血脉传承为主..."
            rows="2"
          />
        </div>
        <button class="btn-generate" @click="generate" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <span v-else>✨</span>
          {{ loading ? '生成中…' : '生成世界观' }}
        </button>
      </div>
    </div>

    <!-- 生成中状态 -->
    <div v-else-if="status === 'generating'" class="step-body generating">
      <div class="generating-status">
        <span class="pulse-dot"></span>
        <span>AI 正在构建你的世界...</span>
      </div>
      <div class="generating-log">
        <div class="log-item">
          <span class="log-icon">🧭</span>
          <span class="log-text">构建地理版图与历史年表</span>
        </div>
        <div class="log-item" style="animation-delay: 2s">
          <span class="log-icon">⚡</span>
          <span class="log-text">设定力量体系与特殊规则</span>
        </div>
        <div class="log-item" style="animation-delay: 4s">
          <span class="log-icon">🏰</span>
          <span class="log-text">生成势力分布与核心冲突</span>
        </div>
      </div>
    </div>

    <!-- 已完成状态 -->
    <div v-else-if="status === 'completed'" class="step-body completed">
      <div class="result-summary">
        <span class="badge success">✅ 已生成</span>
      </div>

      <!-- 动态渲染：每个 world_setting 记录渲染为一个模块 -->
      <div v-if="modules.length" class="world-preview">
        <div
          v-for="mod in modules"
          :key="mod.id || mod.name || mod.category"
          class="preview-section"
          :class="{ highlight: (mod.category || mod.name) === '核心规则' }"
        >
          <div class="section-header">
            <span class="section-icon">{{ moduleIcon(mod) }}</span>
            <span class="section-title">{{ mod.name || mod.category }}</span>
            <span v-if="moduleCount(mod)" class="section-count">{{ moduleCount(mod) }}</span>
          </div>
          <div class="section-content">
            <!-- JSON 数组内容 → 列表 -->
            <template v-if="isArrayContent(mod.content)">
              <div v-for="(sub, idx) in parseContent(mod.content)" :key="idx" class="sub-item">
                <template v-if="sub && typeof sub === 'object'">
                  <div class="sub-title" v-if="sub.name || sub.title">{{ sub.name || sub.title }}</div>
                  <div class="sub-desc" v-if="sub.description || sub.content">{{ sub.description || sub.content }}</div>
                  <div class="sub-goal" v-if="sub.goal">目标：{{ sub.goal }}</div>
                </template>
                <div class="sub-desc" v-else>{{ sub }}</div>
              </div>
            </template>
            <!-- JSON 对象内容 → 键值对 -->
            <template v-else-if="isObjectContent(mod.content)">
              <div v-for="(val, key) in parseContent(mod.content)" :key="key" class="kv-item">
                <strong>{{ key }}：</strong><span>{{ val }}</span>
              </div>
            </template>
            <!-- 纯文本内容 → 直接显示 -->
            <div v-else>{{ mod.content }}</div>
          </div>
        </div>
      </div>

      <div class="step-actions">
        <button class="btn-secondary" @click="status = 'pending'; direction = ''">✏️ 重新生成</button>
        <button class="btn-secondary" @click="regenerate">🔄 换个方向</button>
        <button class="btn-primary" @click="$emit('next')">✅ 继续 →</button>
      </div>
    </div>

    <!-- 失败状态 -->
    <div v-else-if="status === 'failed'" class="step-body failed">
      <p class="error-msg">❌ 生成失败：{{ errorMsg }}</p>
      <div class="step-actions">
        <button class="btn-secondary" @click="$emit('skipped'); $emit('next')">⏭️ 跳过</button>
        <button class="btn-primary" @click="generate">🔄 重试</button>
      </div>
    </div>

    <!-- 跳过状态 -->
    <div v-else-if="status === 'skipped'" class="step-body skipped">
      <div class="result-summary">
        <span class="badge muted">⏭️ 已跳过</span>
      </div>
      <div class="step-actions">
        <button class="btn-secondary" @click="status = 'pending'">✏️ 现在生成</button>
        <button class="btn-primary" @click="$emit('next')">✅ 继续 →</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { setupApi } from '@/api/setup'

const props = defineProps({
  projectId: { type: [Number, String], required: true },
  params: { type: Object, required: true },
  generatedData: { type: Object, default: null },
  // 动态模块：从数据库加载的 world_setting 记录数组
  worldSettings: { type: Array, default: () => [] }
})

const emit = defineEmits(['generated', 'skipped', 'next'])

// ─── 状态 ───
const status = ref(props.generatedData || props.worldSettings?.length ? 'completed' : 'pending')
const loading = ref(false)
const direction = ref('')
const errorMsg = ref('')
const localPreview = ref(null)

// ─── 字段名 → 中文显示名（用于把生成接口返回的 parsed 对象转成模块） ───
const WORLD_FIELD_NAMES = {
  era: '时代背景',
  geography: '地理版图',
  history: '历史年表',
  powerSystem: '力量体系',
  magicSystem: '力量体系',
  factions: '政治势力',
  factionList: '政治势力',
  politics: '政治势力',
  uniqueRules: '核心规则',
  culture: '文化社会',
  technology: '科技文明',
  races: '种族设定',
  gods: '信仰神明',
  ecology: '生态环境'
}

// ─── 模块图标映射 ───
const MODULE_ICONS = {
  '时代背景': '⏳',
  '地理版图': '🗺️',
  '历史年表': '📜',
  '力量体系': '⚡',
  '政治势力': '🏰',
  '核心规则': '🔥',
  '文化社会': '🏛️',
  '科技文明': '🔧',
  '种族设定': '🧬',
  '信仰神明': '🙏',
  '生态环境': '🌿'
}

// ─── 把 parsed 对象（era/geography/...）转为模块数组 [{ name, category, content }] ───
function parsedToModules(obj) {
  if (!obj || typeof obj !== 'object') return []
  return Object.entries(obj)
    .filter(([k, v]) => v !== null && v !== undefined && v !== '' && !(Array.isArray(v) && v.length === 0) && k !== 'factionCount')
    .map(([k, v]) => {
      const displayName = WORLD_FIELD_NAMES[k] || k
      return {
        id: k,
        name: displayName,
        category: displayName,
        content: typeof v === 'string' ? v : JSON.stringify(v)
      }
    })
}

// ─── 动态模块列表（优先级：本地生成 > generatedData > DB worldSettings） ───
const modules = computed(() => {
  if (localPreview.value && typeof localPreview.value === 'object') {
    return parsedToModules(localPreview.value)
  }
  if (props.generatedData?.parsed) {
    return parsedToModules(props.generatedData.parsed)
  }
  if (props.worldSettings && props.worldSettings.length) {
    return props.worldSettings
  }
  return []
})

// ─── 内容解析：支持 JSON 数组/对象/纯文本三种形态 ───
function parseContent(content) {
  if (typeof content !== 'string') return content
  try { return JSON.parse(content) } catch { return content }
}

function isArrayContent(content) {
  return Array.isArray(parseContent(content))
}

function isObjectContent(content) {
  const p = parseContent(content)
  return p && typeof p === 'object' && !Array.isArray(p)
}

// 数组内容时显示数量徽标（如 "3 项"）
function moduleCount(mod) {
  const p = parseContent(mod.content)
  if (Array.isArray(p) && p.length) return `${p.length} 项`
  return ''
}

function moduleIcon(mod) {
  return MODULE_ICONS[mod.category || mod.name] || '📌'
}

// ─── 生成 ───
async function generate() {
  loading.value = true
  status.value = 'generating'
  errorMsg.value = ''

  try {
    const data = await setupApi.generateWorld(props.projectId, {
      title: props.params.title,
      genre: props.params.genre,
      inspiration: props.params.inspiration,
      style: props.params.style,
      direction: direction.value
    })
    if (data?.parsed) localPreview.value = data.parsed
    emit('generated', data)
    status.value = 'completed'
  } catch (e) {
    status.value = 'failed'
    errorMsg.value = e.message || 'AI 生成失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

// ─── 换个方向重试 ───
async function regenerate() {
  direction.value = '请换一个不同角度重新生成'
  await generate()
}
</script>

<style scoped>
.step-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px 0;
}

.step-desc {
  font-size: 13px;
  color: #94a3b8;
  margin: 0 0 20px 0;
}

.step-body {
  min-height: 200px;
}

/* ─── pending ─── */
.input-area {
  max-width: 520px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 16px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  background: #fafbfc;
  outline: none;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.form-textarea:focus {
  border-color: #818cf8;
  background: #fff;
}

.btn-generate {
  padding: 12px 32px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.3);
}
.btn-generate:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4);
}
.btn-generate:disabled {
  opacity: 0.5;
  cursor: wait;
}

.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* ─── generating ─── */
.generating-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #4f46e5;
  margin-bottom: 24px;
}

.pulse-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #6366f1;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}

.generating-log {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 8px;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #6b6560;
  opacity: 0;
  animation: fadeIn 0.5s ease forwards;
}

@keyframes fadeIn {
  to { opacity: 1; }
}

.log-icon {
  font-size: 18px;
}

/* ─── completed ─── */
.result-summary {
  margin-bottom: 16px;
}

.badge {
  display: inline-block;
  padding: 5px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
}

.badge.success {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.badge.muted {
  background: #f8fafc;
  color: #94a3b8;
  border: 1px solid #e8e3dc;
}

/* ─── 世界观预览 ─── */
.world-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.preview-section {
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #f0ece6;
}

.preview-section.highlight {
  background: #fef2f2;
  border-color: #fecaca;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.section-icon {
  font-size: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
}

.section-count {
  font-size: 12px;
  color: #6366f1;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 6px;
  margin-left: auto;
}

.section-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.7;
}

/* ─── 动态子项（数组内容列表） ─── */
.sub-item {
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8e3dc;
  margin-bottom: 8px;
}
.sub-item:last-child {
  margin-bottom: 0;
}

.sub-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.sub-desc {
  font-size: 13px;
  color: #6b6560;
  line-height: 1.5;
  margin-bottom: 4px;
}

.sub-goal {
  font-size: 12px;
  color: #6366f1;
  font-style: italic;
}

/* ─── 对象内容键值对 ─── */
.kv-item {
  font-size: 13px;
  color: #6b6560;
  line-height: 1.6;
  margin-bottom: 6px;
}
.kv-item:last-child {
  margin-bottom: 0;
}
.kv-item strong {
  color: #334155;
}

/* ─── 按钮组 ─── */
.step-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.btn-primary {
  padding: 10px 28px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.2);
}
.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 18px rgba(99, 102, 241, 0.3);
}
.btn-primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.2);
}
.btn-primary:focus {
  outline: none;
}

.btn-secondary {
  padding: 10px 20px;
  background: #f5f2ed;
  border: 1px solid #e8e3dc;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #6b6560;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover {
  background: #e8e3dc;
}
.btn-secondary:active {
  background: #ddd8d0;
}

/* ─── failed ─── */
.error-msg {
  text-align: center;
  color: #be123c;
  font-size: 14px;
  margin-bottom: 8px;
}

/* ─── skipped ─── */
.skipped .step-actions {
  justify-content: flex-start;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>