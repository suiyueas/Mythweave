<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div class="modal-overlay" @click.self="$emit('close')">
        <div class="modal-content">
          <div class="modal-header">
            <h2>{{ mode === 'create' ? '✏️ 新建章节' : '📝 编辑章节' }}</h2>
            <button class="btn-close" @click="$emit('close')">✕</button>
          </div>

          <!-- AI 辅助 -->
          <div class="ai-hint">
            <span class="icon">✨</span>
            <span class="text">让 AI 帮你生成章节内容</span>
            <button class="btn-ai" @click="generateAI">AI 生成</button>
          </div>

          <div class="modal-body">
            <div class="form-group">
              <label>章节标题 <span class="hint">（必填）</span></label>
              <input v-model="form.title" placeholder="如：第4章·星辰坠落" class="form-input" />
            </div>

            <div class="form-group">
              <label>正文内容 <span class="hint">（选填）</span></label>
              <textarea
                v-model="form.content"
                placeholder="在此输入章节正文..."
                rows="6"
                class="form-textarea"
              ></textarea>
              <span class="char-count">{{ (form.content || '').length }} 字</span>
            </div>

            <div class="form-group">
              <label>目标字数</label>
              <input v-model.number="form.targetWordCount" type="number" placeholder="如：3000" class="form-input" />
            </div>

            <div class="form-group">
              <label>状态</label>
              <select v-model="form.status" class="form-select">
                <option value="draft">草稿</option>
                <option value="writing">写作中</option>
                <option value="published">已发布</option>
                <option value="completed">已完成</option>
              </select>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-cancel" @click="$emit('close')">取消</button>
            <button class="btn-confirm" @click="submit" :disabled="!form.title || saving">
              <span v-if="saving" class="spinner"></span>
              <span v-else>{{ mode === 'create' ? '🚀 创建' : '💾 保存' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { useNovelStore } from '@/stores/novel'

const props = defineProps({
  mode: { type: String, default: 'create' },
  chapter: { type: Object, default: null },
  projectId: { type: [Number, String], required: true }
})

const emit = defineEmits(['close', 'saved'])
const store = useNovelStore()
const saving = ref(false)

const form = reactive({
  title: '',
  content: '',
  targetWordCount: null,
  status: 'draft'
})

// 编辑时回显
watch(() => props.chapter, (ch) => {
  if (ch && props.mode === 'edit') {
    form.title = ch.title || ''
    form.content = ch.content || ''
    form.targetWordCount = ch.targetWordCount || null
    form.status = ch.status || 'draft'
  } else {
    form.title = ''
    form.content = ''
    form.targetWordCount = null
    form.status = 'draft'
  }
}, { immediate: true })

async function submit() {
  if (!form.title.trim() || saving.value) return
  saving.value = true

  const data = {
    title: form.title.trim(),
    content: form.content?.trim() || '',
    targetWordCount: form.targetWordCount,
    status: form.status
  }

  try {
    if (props.mode === 'create') {
      await store.createChapter(props.projectId, data)
    } else {
      await store.updateChapter(props.projectId, { ...data, id: props.chapter.id })
    }
    emit('saved')
  } catch (error) {
    console.error('保存失败：', error)
    alert('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

function generateAI() {
  // TODO: 接入 AI 接口生成内容
  const aiContent = '（AI 生成的示例内容，实际应接入 AI 接口）\n\n夜色如墨，笼罩着整个星辰帝国。林越从黑暗中醒来，发现自己躺在冰冷的地面上，四周是残破的墙壁和散落的瓦砾。'
  form.content = aiContent
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.35); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}

.modal-content {
  background: #fff; border-radius: 16px; width: 100%; max-width: 560px;
  max-height: 90vh; overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
  animation: dialogIn 0.2s ease;
}

@keyframes dialogIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px 0;
}

.modal-header h2 {
  font-size: 18px; font-weight: 700; color: #1a1815; margin: 0;
}

.btn-close {
  background: none; border: none; font-size: 18px; color: #9c9690;
  cursor: pointer; padding: 2px 6px; line-height: 1;
}
.btn-close:hover { color: #6b6560; }

.modal-body {
  padding: 16px 24px 0;
}

.ai-hint {
  display: flex; align-items: center; gap: 8px;
  margin: 16px 24px 0;
  padding: 10px 14px;
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  border-radius: 10px;
  border: 1px solid #fde68a;
}

.ai-hint .icon { font-size: 16px; }
.ai-hint .text { flex: 1; font-size: 12px; color: #92400e; }

.btn-ai {
  padding: 4px 12px; font-size: 11px; font-weight: 600;
  background: #d97706; color: #fff; border: none; border-radius: 6px;
  cursor: pointer; transition: background 0.2s;
}
.btn-ai:hover { background: #b45309; }

.form-group { margin-bottom: 14px; }
.form-group.flex-1 { flex: 1; }

.form-group label {
  display: block; font-size: 13px; font-weight: 600; color: #6b6560; margin-bottom: 5px;
}
.form-group label .hint {
  font-weight: 400; color: #a8a4a0; font-size: 11px;
}

.form-input {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  font-size: 14px; outline: none; background: #fafbfc; transition: border-color 0.2s;
  box-sizing: border-box;
}
.form-input:focus { border-color: #d97706; background: #fff; }
.form-input[type="number"] { -moz-appearance: textfield; appearance: textfield; }
.form-input[type="number"]::-webkit-outer-spin-button,
.form-input[type="number"]::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }

.form-textarea {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  font-size: 13px; outline: none; resize: vertical; min-height: 120px;
  background: #fafbfc; font-family: inherit; transition: border-color 0.2s;
  box-sizing: border-box; line-height: 1.5;
}
.form-textarea:focus { border-color: #d97706; background: #fff; }

.char-count {
  text-align: right; font-size: 11px; color: #a8a4a0;
  margin-top: 4px; display: block;
}

.form-row {
  display: flex; gap: 12px;
}

.form-select {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  font-size: 14px; outline: none; background: #fafbfc; transition: border-color 0.2s;
  box-sizing: border-box; cursor: pointer; color: #1a1815;
}
.form-select:focus { border-color: #d97706; background: #fff; }

.modal-footer {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 16px 24px 20px; border-top: 1px solid #f3efe8; margin-top: 4px;
}

.btn-cancel {
  padding: 8px 20px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  background: #fff; color: #6b6560; font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel:hover { background: #f5f2ed; }

.btn-confirm {
  padding: 8px 24px; border: none; border-radius: 8px;
  background: #d97706; color: #fff; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; gap: 4px;
}
.btn-confirm:hover:not(:disabled) { background: #b45309; }
.btn-confirm:disabled { opacity: 0.45; cursor: not-allowed; }

.spinner {
  display: inline-block; width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.4); border-top-color: #fff;
  border-radius: 50%; animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 过渡动画 */
.dialog-fade-enter-active { transition: all 0.25s ease-out; }
.dialog-fade-leave-active { transition: all 0.2s ease-in; }
.dialog-fade-enter-from { opacity: 0; }
.dialog-fade-leave-to { opacity: 0; }
</style>
