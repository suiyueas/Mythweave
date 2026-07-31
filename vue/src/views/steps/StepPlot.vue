<template>
  <div class="step-plot">
    <div class="step-header">
      <h2>🔗 情节引擎</h2>
      <p class="step-desc">AI 将设计情节主线、支线与伏笔系统</p>
    </div>

    <div v-if="status === 'pending'" class="step-body pending">
      <div class="input-area">
        <div class="form-group">
          <label>额外方向（可选）</label>
          <textarea v-model="direction" class="form-textarea"
            placeholder="比如：增加悬疑元素，主线围绕一个千古秘密展开..."
            rows="2" />
        </div>
        <button class="btn-generate" @click="generate" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <span v-else>✨</span>
          {{ loading ? '生成中…' : '生成情节引擎' }}
        </button>
        <button class="btn-skip-step" @click="skip">⏭️ 跳过此步</button>
      </div>
    </div>

    <div v-else-if="status === 'generating'" class="step-body generating">
      <div class="generating-status">
        <span class="pulse-dot"></span>
        <span>AI 正在编织情节网络...</span>
      </div>
      <div class="generating-log">
        <div class="log-item"><span class="log-icon">🧵</span><span>设计主线情节与关键节点</span></div>
        <div class="log-item" style="animation-delay:2s"><span class="log-icon">🔀</span><span>构建支线情节</span></div>
        <div class="log-item" style="animation-delay:4s"><span class="log-icon">🪝</span><span>埋设伏笔与回收点</span></div>
      </div>
    </div>

    <div v-else-if="status === 'completed'" class="step-body completed">
      <div class="result-summary">
        <span class="badge success">✅ {{ preview.threadCount || '?' }} 条线，{{ preview.foreshadowingCount || '?' }} 个伏笔</span>
      </div>
      <div class="plot-preview" v-if="preview.mainThreadTitle">
        <div class="preview-item">
          <span class="preview-label">主线</span>
          <span class="preview-value">{{ preview.mainThreadTitle }}</span>
        </div>
      </div>
      <div class="step-actions">
        <button class="btn-secondary" @click="status = 'pending'; direction = ''">✏️ 重新生成</button>
        <button class="btn-secondary" @click="regenerate">🔄 换个方向</button>
        <button class="btn-secondary" @click="$emit('prev')">← 上一步</button>
        <button class="btn-primary" @click="$emit('next')">✅ 继续 →</button>
      </div>
    </div>

    <div v-else-if="status === 'failed'" class="step-body failed">
      <p class="error-msg">❌ 生成失败：{{ errorMsg }}</p>
      <div class="step-actions">
        <button class="btn-secondary" @click="skip">⏭️ 跳过</button>
        <button class="btn-primary" @click="generate">🔄 重试</button>
      </div>
    </div>

    <div v-else-if="status === 'skipped'" class="step-body skipped">
      <div class="result-summary"><span class="badge muted">⏭️ 已跳过</span></div>
      <div class="step-actions">
        <button class="btn-secondary" @click="status = 'pending'">✏️ 现在生成</button>
        <button class="btn-secondary" @click="$emit('prev')">← 上一步</button>
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
  outline: { type: Object, default: null },
  characters: { type: Object, default: null },
  generatedData: { type: Object, default: null }
})

const emit = defineEmits(['generated', 'skipped', 'next', 'prev'])

const status = ref(props.generatedData ? 'completed' : 'pending')
const loading = ref(false)
const direction = ref('')
const errorMsg = ref('')
const localPreview = ref(null)

const preview = computed(() => {
  if (localPreview.value) return localPreview.value
  return props.generatedData?.parsed || {}
})

async function generate() {
  loading.value = true; status.value = 'generating'; errorMsg.value = ''
  try {
    const res = await setupApi.generatePlot(props.projectId, {
      title: props.params.title, genre: props.params.genre,
      outlineRaw: props.outline?.rawText || '',
      charactersRaw: props.characters?.rawText || '',
      style: props.params.style, direction: direction.value
    })
    const data = res?.data || res
    if (data?.parsed) localPreview.value = data.parsed
    emit('generated', data)
    status.value = 'completed'
  } catch (e) {
    status.value = 'failed'; errorMsg.value = e.message || 'AI 生成失败'
  } finally { loading.value = false }
}

async function regenerate() { direction.value = '请换一个不同角度重新生成情节'; await generate() }
function skip() { emit('skipped'); emit('next') }
</script>

<style scoped>
.step-plot { }
.step-header h2 { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px 0; }
.step-desc { font-size: 13px; color: #94a3b8; margin: 0 0 20px 0; }
.step-body { min-height: 200px; }
.input-area { max-width: 520px; }
.form-group { display: flex; flex-direction: column; gap: 5px; margin-bottom: 16px; }
.form-group label { font-size: 13px; font-weight: 600; color: #334155; }
.form-textarea { width: 100%; padding: 10px 12px; border: 1.5px solid #e2e8f0; border-radius: 10px; font-size: 14px; background: #fafbfc; outline: none; font-family: inherit; resize: vertical; box-sizing: border-box; transition: border-color 0.2s; }
.form-textarea:focus { border-color: #818cf8; background: #fff; }
.btn-generate { padding: 12px 32px; background: linear-gradient(135deg, #6366f1, #4f46e5); border: none; border-radius: 12px; color: #fff; font-size: 15px; font-weight: 700; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; transition: all 0.2s; box-shadow: 0 2px 12px rgba(99,102,241,0.3); }
.btn-generate:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 20px rgba(99,102,241,0.4); }
.btn-generate:disabled { opacity: 0.5; cursor: wait; }
.btn-skip-step { display: block; margin-top: 10px; background: none; border: none; color: #94a3b8; font-size: 13px; cursor: pointer; padding: 4px 0; }
.btn-skip-step:hover { color: #6b6560; }
.spinner { display: inline-block; width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; }
.generating-status { display: flex; align-items: center; gap: 10px; font-size: 15px; font-weight: 600; color: #4f46e5; margin-bottom: 24px; }
.pulse-dot { width: 12px; height: 12px; border-radius: 50%; background: #6366f1; animation: pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%,100%{opacity:1;transform:scale(1)}50%{opacity:.4;transform:scale(.8)} }
.generating-log { display: flex; flex-direction: column; gap: 12px; padding: 0 8px; }
.log-item { display: flex; align-items: center; gap: 10px; font-size: 14px; color: #6b6560; opacity: 0; animation: fadeIn 0.5s ease forwards; }
@keyframes fadeIn { to{opacity:1} }
.log-icon { font-size: 18px; }
.result-summary { margin-bottom: 16px; }
.badge { display: inline-block; padding: 5px 14px; border-radius: 8px; font-size: 13px; font-weight: 600; }
.badge.success { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }
.badge.muted { background: #f8fafc; color: #94a3b8; border: 1px solid #e8e3dc; }
.plot-preview { margin-bottom: 20px; padding: 16px; background: #f8fafc; border-radius: 12px; border: 1px solid #f0ece6; }
.preview-item { display: flex; flex-direction: column; gap: 2px; }
.preview-label { font-size: 11px; font-weight: 600; color: #94a3b8; text-transform: uppercase; }
.preview-value { font-size: 14px; color: #334155; }
.step-actions { display: flex; gap: 10px; margin-top: 20px; flex-wrap: wrap; }
.btn-primary { padding: 10px 28px; background: linear-gradient(135deg, #6366f1, #4f46e5); border: none; border-radius: 10px; color: #fff; font-size: 14px; font-weight: 700; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 10px rgba(99,102,241,0.2); }
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 4px 18px rgba(99,102,241,0.3); }
.btn-primary:active { transform: translateY(0); box-shadow: 0 2px 10px rgba(99,102,241,0.2); }
.btn-primary:focus { outline: none; }
.btn-secondary { padding: 10px 20px; background: #f5f2ed; border: 1px solid #e8e3dc; border-radius: 10px; font-size: 14px; font-weight: 600; color: #6b6560; cursor: pointer; transition: all 0.2s; }
.btn-secondary:hover { background: #e8e3dc; }
.btn-secondary:active { background: #ddd8d0; }
.error-msg { text-align: center; color: #be123c; font-size: 14px; margin-bottom: 8px; }
@keyframes spin { to{transform:rotate(360deg)} }
</style>