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

      <div v-if="preview" class="result-preview">
        <div class="preview-item" v-if="preview.era">
          <span class="preview-label">时代背景</span>
          <span class="preview-value">{{ preview.era }}</span>
        </div>
        <div class="preview-item" v-if="preview.powerSystem">
          <span class="preview-label">力量体系</span>
          <span class="preview-value">{{ preview.powerSystem }}</span>
        </div>
        <div class="preview-item" v-if="preview.factionCount">
          <span class="preview-label">势力数量</span>
          <span class="preview-value">{{ preview.factionCount }} 个势力</span>
        </div>
        <div class="preview-item" v-if="preview.uniqueRules">
          <span class="preview-label">核心矛盾</span>
          <span class="preview-value">{{ preview.uniqueRules }}</span>
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
  generatedData: { type: Object, default: null }
})

const emit = defineEmits(['generated', 'skipped', 'next'])

// ─── 状态 ───
const status = ref(props.generatedData ? 'completed' : 'pending')
const loading = ref(false)
const direction = ref('')
const errorMsg = ref('')

// ─── 预览数据 ───
const preview = computed(() => {
  if (props.generatedData?.parsed) return props.generatedData.parsed
  return null
})

// ─── 生成 ───
async function generate() {
  loading.value = true
  status.value = 'generating'
  errorMsg.value = ''

  try {
    const res = await setupApi.generateWorld(props.projectId, {
      title: props.params.title,
      genre: props.params.genre,
      inspiration: props.params.inspiration,
      style: props.params.style,
      direction: direction.value
    })
    const data = res?.data || res
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
.step-world { }

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

.result-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #f0ece6;
}

.preview-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.preview-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
}

.preview-value {
  font-size: 14px;
  color: #334155;
  line-height: 1.5;
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
