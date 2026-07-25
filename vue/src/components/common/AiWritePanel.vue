<template>
  <Teleport to="body">
    <Transition name="panel-slide">
      <div v-if="visible" class="panel-overlay" @click.self="close">
        <div class="panel-container">
          <div class="panel-header">
            <span class="panel-title">✨ AI 章节内容生成</span>
            <button class="btn-close" @click="close">✕</button>
          </div>

          <div class="panel-body">
            <div class="chapter-info">
              <span class="ci-index">第 {{ chapterIndex }} 章</span>
              <span class="ci-title">{{ chapterTitle || '未命名章节' }}</span>
            </div>

            <div class="form-group">
              <label class="form-label">写作方向 <span class="label-hint">（描述期望的内容走向）</span></label>
              <textarea v-model="direction" class="form-textarea"
                placeholder="如：主角突破金丹期，引来天劫，在众人帮助下渡劫成功，实力大增……" rows="3" />
            </div>

            <button class="btn-generate" @click="handleGenerate" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              <span v-else>✨</span>
              {{ loading ? 'AI 创作中…' : '开始生成' }}
            </button>

            <div v-if="content" class="preview-area">
              <div class="preview-header">
                <span class="preview-label">📄 生成预览</span>
                <span class="preview-count">约 {{ wordCount }} 字</span>
              </div>
              <div class="preview-body">
                <template v-for="(block, i) in parsedBlocks" :key="i">
                  <hr v-if="block.type === 'divider'" class="scene-divider" />
                  <p v-else-if="block.type === 'dialogue'" class="dialog-block">{{ block.text }}</p>
                  <p v-else-if="block.type === 'short'" class="short-block">{{ block.text }}</p>
                  <p v-else class="text-block">{{ block.text }}</p>
                </template>
              </div>
              <div class="preview-actions">
                <button class="btn-adopt" @click="handleAdopt" :disabled="adopting">
                  {{ adopting ? '采纳中…' : '✅ 采纳到编辑器' }}
                </button>
                <button class="btn-retry" @click="handleGenerate" :disabled="loading">🔄 重新生成</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { aiApi } from '@/api'

const props = defineProps({
  visible: { type: Boolean, default: false },
  projectId: { type: [Number, String], default: '' },
  chapterId: { type: [Number, String], default: '' },
  chapterTitle: { type: String, default: '' },
  chapterIndex: { type: Number, default: 1 }
})

const emit = defineEmits(['close', 'adopt'])

const direction = ref('')
const content = ref('')
const loading = ref(false)
const adopting = ref(false)

const wordCount = computed(() => content.value.replace(/\s/g, '').length)

// 解析内容为结构化块
const parsedBlocks = computed(() => {
  if (!content.value) return []
  const lines = content.value.split('\n')
  const blocks = []

  for (const line of lines) {
    const t = line.trim()
    if (!t) continue

    // 场景分隔线
    if (t === '---' || t === '***') {
      blocks.push({ type: 'divider' })
      continue
    }

    // 对话（以「开头或包含「」）
    if (/^[「]/.test(t) || (t.includes('「') && t.includes('」') && t.length < 100)) {
      blocks.push({ type: 'dialogue', text: t })
      continue
    }

    // 短句（<= 30 字）作为强调段
    if (t.length <= 30 && t.endsWith('。') === false && t.endsWith('！') === false && t.endsWith('？') === false) {
      blocks.push({ type: 'short', text: t })
      continue
    }

    blocks.push({ type: 'paragraph', text: t })
  }
  return blocks
})

async function handleGenerate() {
  if (loading.value) return
  loading.value = true
  content.value = ''

  try {
    await aiApi.generateContentStream(
      props.projectId,
      { chapterIndex: props.chapterIndex, title: props.chapterTitle, direction: direction.value || '延续故事主线，推动情节发展' },
      (token) => { if (token != null && token !== 'null' && token !== 'undefined') content.value += token },
      () => { loading.value = false },
      (e) => { console.error('AI 生成失败：', e); loading.value = false }
    )
  } catch (e) {
    console.error('AI 生成失败：', e)
    loading.value = false
  }
}

function handleAdopt() {
  if (!content.value) return
  adopting.value = true
  emit('adopt', content.value)
  setTimeout(() => { adopting.value = false }, 300)
}

function close() { emit('close') }

watch(() => props.visible, (v) => {
  if (v) { direction.value = ''; content.value = '' }
})
</script>

<style scoped>
.panel-overlay {
  position: fixed; inset: 0; z-index: 900;
  background: rgba(0,0,0,0.2);
  display: flex; justify-content: flex-end;
}
.panel-container {
  width: 460px; max-width: 90vw; height: 100vh;
  background: #fff; box-shadow: -8px 0 40px rgba(0,0,0,0.12);
  display: flex; flex-direction: column; overflow: hidden;
}
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid #f3efe8; flex-shrink: 0;
}
.panel-title { font-size: 16px; font-weight: 700; color: #1a1815; }
.btn-close { background: none; border: none; font-size: 18px; color: #9c9690; cursor: pointer; }

.panel-body { flex: 1; overflow-y: auto; padding: 20px; }

.chapter-info {
  display: flex; align-items: center; gap: 8px; margin-bottom: 18px;
  padding: 10px 14px; background: #faf8f5; border-radius: 10px;
}
.ci-index { font-size: 12px; font-weight: 700; color: #d97706; flex-shrink: 0; }
.ci-title { font-size: 14px; font-weight: 600; color: #6b6560; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: 12px; font-weight: 600; color: #6b6560; margin-bottom: 6px; }
.label-hint { font-weight: 400; color: #9c9690; }

.form-textarea {
  width: 100%; padding: 10px 14px; border: 1.5px solid #e8e3dc; border-radius: 10px;
  font-size: 13px; outline: none; resize: vertical; min-height: 72px;
  background: #fafbfc; font-family: inherit; transition: border-color 0.2s;
}
.form-textarea:focus { border-color: #d97706; background: #fff; }

.btn-generate {
  width: 100%; padding: 11px 0; border: 1.5px solid #d97706; border-radius: 10px;
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  color: #92400e; font-size: 14px; font-weight: 600;
  cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 6px;
  transition: all 0.2s; margin-bottom: 18px;
}
.btn-generate:hover:not(:disabled) { background: #fef3c7; box-shadow: 0 2px 8px rgba(217,119,6,0.15); }
.btn-generate:disabled { opacity: 0.6; cursor: wait; }

.spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid #d97706; border-top-color: transparent; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.preview-area {
  border: 1.5px solid #e8e3dc; border-radius: 10px;
  overflow: hidden; animation: slideUp 0.3s ease;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(8px); } }
.preview-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px; background: #faf8f5; border-bottom: 1px solid #f3efe8;
}
.preview-label { font-size: 12px; font-weight: 600; color: #6b6560; }
.preview-count { font-size: 11px; color: #9c9690; }
.preview-body { padding: 16px 18px; max-height: 420px; overflow-y: auto; }

/* ── 段落块 ── */
.text-block {
  font-size: 13px; color: #3a3a4a; line-height: 2;
  margin: 0 0 1em 0; text-indent: 2em; text-align: justify;
}

/* ── 短句/动作强调 ── */
.short-block {
  font-size: 13px; color: #5a5a72; line-height: 1.7;
  margin: 0.8em 0; text-indent: 0; font-weight: 500;
}

/* ── 对话 ── */
.dialog-block {
  font-size: 13px; color: #4a4a6a; line-height: 1.7;
  margin: 0.6em 0; padding-left: 2em; text-indent: 0;
}

/* ── 场景分隔线 ── */
.scene-divider {
  border: none; border-top: 2px dashed #e2d9cc;
  margin: 1.5em auto; width: 50%; opacity: 0.6;
}

.preview-actions { display: flex; gap: 8px; padding: 10px 12px; border-top: 1px solid #f3efe8; }
.btn-adopt, .btn-retry {
  flex: 1; padding: 8px 0; border-radius: 7px; font-size: 12px; font-weight: 600; cursor: pointer;
  border: 1.5px solid transparent; transition: all 0.2s; text-align: center;
}
.btn-adopt { background: #0d9488; color: #fff; border-color: #0d9488; }
.btn-adopt:hover:not(:disabled) { background: #0f766e; }
.btn-adopt:disabled { opacity: 0.6; cursor: wait; }
.btn-retry { background: #fff; color: #6b6560; border-color: #e8e3dc; }
.btn-retry:hover { border-color: #d97706; color: #d97706; }

.panel-slide-enter-active { transition: all 0.3s ease; }
.panel-slide-leave-active { transition: all 0.25s ease; }
.panel-slide-enter-from .panel-container { transform: translateX(100%); }
.panel-slide-leave-from .panel-container { transform: translateX(0); }
.panel-slide-leave-to .panel-container { transform: translateX(100%); }
</style>