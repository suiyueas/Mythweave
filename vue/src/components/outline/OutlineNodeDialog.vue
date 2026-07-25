<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="dialog-overlay" @click.self="handleClose" @keydown.esc="handleClose">
        <div class="dialog-content" @keydown.enter="handleSave" @keydown.esc="handleClose">
          <!-- 头部 -->
          <div class="dialog-header">
            <span class="dialog-title">{{ isEdit ? '✏️ 编辑大纲节点' : '📋 新建大纲节点' }}</span>
            <button class="btn-close" @click="handleClose" type="button">✕</button>
          </div>

          <!-- 表单 -->
          <div class="dialog-body">
            <!-- 标题 -->
            <div class="form-group">
              <label class="form-label">
                标题 <span class="required">*</span>
                <span v-if="errors.title" class="error-text">{{ errors.title }}</span>
              </label>
              <input
                ref="titleInput"
                v-model="form.title"
                class="form-input"
                :class="{ 'input-error': errors.title }"
                placeholder="请输入节点标题..."
                maxlength="200"
                @input="clearError('title')"
              />
            </div>

            <!-- 描述 -->
            <div class="form-group">
              <label class="form-label">
                描述
                <span v-if="errors.description" class="error-text">{{ errors.description }}</span>
              </label>
              <textarea
                v-model="form.description"
                class="form-textarea"
                :class="{ 'input-error': errors.description }"
                placeholder="请输入节点描述，如：本章核心冲突、关键事件或幕主题概括..."
                rows="3"
                maxlength="2000"
                @input="clearError('description')"
              />
              <div class="char-counter">
                <span :class="{ warn: form.description.length > 1800 }">
                  {{ form.description.length }}
                </span>
                / 2000
              </div>
            </div>

            <!-- 第一行：所属幕 + 类型 -->
            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">所属幕</label>
                <select v-model="form.act" class="form-select">
                  <option v-for="a in actOptions" :key="a.key" :value="a.key">
                    {{ a.icon }} {{ a.label }}
                  </option>
                </select>
              </div>
              <div class="form-group flex-1">
                <label class="form-label">类型</label>
                <select v-model="form.type" class="form-select">
                  <option value="">请选择类型</option>
                  <option value="volume">📚 卷</option>
                  <option value="chapter">📄 章</option>
                  <option value="scene">🎬 场景</option>
                </select>
              </div>
            </div>

            <!-- 第二行：字数预估 + 状态 -->
            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">字数预估（可选）</label>
                <input
                  v-model.number="form.estimatedWords"
                  class="form-input"
                  type="number"
                  placeholder="如：3000"
                  min="0"
                />
              </div>
              <div class="form-group flex-1">
                <label class="form-label">状态</label>
                <select v-model="form.nodeStatus" class="form-select">
                  <option value="draft">📝 草稿</option>
                  <option value="pending">🔧 待修改</option>
                  <option value="completed">✅ 已完成</option>
                </select>
              </div>
            </div>

            <!-- 第三行：关联章节 -->
            <div class="form-group">
              <label class="form-label">关联章节（可选）</label>
              <select v-model="form.chapterId" class="form-select">
                <option :value="null">无关联章节</option>
                <option
                  v-for="ch in chapterOptions"
                  :key="ch.id"
                  :value="ch.id"
                >
                  {{ ch.label }}
                </option>
              </select>
              <div v-if="chapterOptions.length === 0" class="form-hint">当前作品暂无章节</div>
            </div>

            <!-- 核心情节点 -->
            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.isKeyEvent" class="checkbox-input" />
                <span class="checkbox-custom"></span>
                <span class="checkbox-text">标记为关键情节点 ⭐</span>
              </label>
              <div class="checkbox-hint">关键情节点将在列表中高亮显示</div>
            </div>
          </div>

          <!-- 底部 -->
          <div class="dialog-footer">
            <button class="btn-cancel" @click="handleClose" :disabled="saving" type="button">取消</button>
            <button
              class="btn-save"
              @click="handleSave"
              :disabled="!canSave || saving"
              type="button"
            >
              <span v-if="saving" class="spinner"></span>
              <span v-else>{{ isEdit ? '💾 保存修改' : '✅ 确认新增' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useNovelStore } from '@/stores/novel'
import { outlineApi } from '@/api/outline'

const store = useNovelStore()

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  projectId: { type: [Number, String], required: true },
  editNode: { type: Object, default: null },
  defaultAct: { type: String, default: null }
})

const emit = defineEmits(['update:modelValue', 'saved'])

// ─── 常量 ───
const actOptions = [
  { key: 'first_act', icon: '📖', label: '第一幕：建置' },
  { key: 'second_act', icon: '⚔️', label: '第二幕：对抗' },
  { key: 'third_act', icon: '🏆', label: '第三幕：解决' }
]

// ─── 状态 ───
const visible = computed(() => props.modelValue)
const isEdit = computed(() => !!props.editNode)
const saving = ref(false)
const titleInput = ref(null)

const form = ref(createEmptyForm())
const errors = ref({ title: '', description: '' })

// ─── 计算属性 ───
const chapterOptions = computed(() => {
  return (store.chapters || []).map(ch => ({
    id: ch.id,
    label: `第${ch.sortOrder || ch.chapterNumber || '?'}章${ch.title ? '：' + ch.title : ''}`
  }))
})

const canSave = computed(() => {
  return form.value.title.trim().length > 0 &&
    form.value.description.trim().length >= 10 &&
    !saving.value
})

// ─── 智能推荐所属幕 ───
function suggestAct() {
  if (props.defaultAct) return props.defaultAct
  const outlines = store.outlines || []
  const counts = actOptions.map(a => ({
    key: a.key,
    count: outlines.filter(o => (o.act || 'first_act') === a.key).length
  }))
  counts.sort((a, b) => a.count - b.count)
  return counts[0]?.key || 'first_act'
}

// ─── 表单 ───
function createEmptyForm() {
  return {
    title: '',
    description: '',
    estimatedWords: null,
    type: '',
    act: 'first_act',
    nodeStatus: 'draft',
    chapterId: null,
    isKeyEvent: false
  }
}

function resetForm() {
  form.value = createEmptyForm()
  errors.value = { title: '', description: '' }
}

// 编辑时回显
watch(
  () => props.editNode,
  (node) => {
    if (node) {
      form.value = {
        title: node.title || '',
        description: node.description || '',
        estimatedWords: node.estimatedWords || null,
        type: node.type || '',
        act: node.act || 'first_act',
        nodeStatus: node.nodeStatus || 'draft',
        chapterId: node.chapterId || null,
        isKeyEvent: node.isKeyEvent || false
      }
    } else {
      form.value = createEmptyForm()
      form.value.act = suggestAct()
    }
  },
  { immediate: true }
)

// 打开时重置 + 自动聚焦
watch(
  () => props.modelValue,
  async (val) => {
    if (val) {
      if (!props.editNode) {
        form.value = createEmptyForm()
        form.value.act = suggestAct()
      }
      errors.value = { title: '', description: '' }
      await nextTick()
      titleInput.value?.focus()
    }
  }
)

// ─── 验证 ───
function validate() {
  let valid = true
  errors.value = { title: '', description: '' }

  if (!form.value.title.trim()) {
    errors.value.title = '标题不能为空'
    valid = false
  }

  if (form.value.description.trim() && form.value.description.trim().length < 10) {
    errors.value.description = '描述至少 10 个字'
    valid = false
  }

  return valid
}

function clearError(field) {
  if (errors.value[field]) {
    errors.value[field] = ''
  }
}

// ─── 提交 ───
async function handleSave() {
  if (!validate() || saving.value) return

  saving.value = true
  try {
    const data = {
      title: form.value.title.trim(),
      description: form.value.description.trim(),
      estimatedWords: form.value.estimatedWords || null,
      type: form.value.type || null,
      act: form.value.act,
      nodeStatus: form.value.nodeStatus,
      chapterId: form.value.chapterId || null,
      isKeyEvent: form.value.isKeyEvent || false
    }

    if (isEdit.value && props.editNode?.id) {
      await outlineApi.update(props.projectId, props.editNode.id, data)
    } else {
      await outlineApi.create(props.projectId, data)
    }

    emit('saved')
    close()
  } catch (e) {
    console.error('保存大纲节点失败：', e.message)
    alert('保存失败：' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

function handleClose() {
  if (saving.value) return
  close()
}

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.35); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}

.dialog-content {
  background: #fff; border-radius: 16px; width: 100%; max-width: 560px;
  max-height: 90vh; overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
  animation: dialogIn 0.2s ease;
}

@keyframes dialogIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.dialog-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px 0;
}

.dialog-title {
  font-size: 18px; font-weight: 700; color: #1a1815;
}

.btn-close {
  background: none; border: none; font-size: 18px; color: #9c9690;
  cursor: pointer; padding: 2px 6px; line-height: 1;
}
.btn-close:hover { color: #6b6560; }

.dialog-body {
  padding: 16px 24px 0;
}

.form-group { margin-bottom: 14px; }
.form-group.flex-1 { flex: 1; }

.form-label {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #6b6560; margin-bottom: 5px;
}
.form-label .required { color: #be123c; }

.error-text {
  font-size: 11px; font-weight: 400; color: #dc2626;
}

.form-input {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  font-size: 14px; outline: none; background: #fafbfc; transition: border-color 0.2s;
  box-sizing: border-box;
}
.form-input:focus { border-color: #d97706; background: #fff; }
.form-input.input-error { border-color: #dc2626; background: #fef2f2; }
.form-input[type="number"] { -moz-appearance: textfield; appearance: textfield; }
.form-input[type="number"]::-webkit-outer-spin-button,
.form-input[type="number"]::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }

.form-textarea {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  font-size: 13px; outline: none; resize: vertical; min-height: 72px;
  background: #fafbfc; font-family: inherit; transition: border-color 0.2s;
  box-sizing: border-box; line-height: 1.5;
}
.form-textarea:focus { border-color: #d97706; background: #fff; }
.form-textarea.input-error { border-color: #dc2626; background: #fef2f2; }

.char-counter {
  text-align: right; font-size: 11px; color: #a8a4a0;
  margin-top: 4px; padding-right: 2px;
}
.char-counter .warn { color: #d97706; font-weight: 600; }

.form-row {
  display: flex; gap: 12px;
}

.form-select {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  font-size: 14px; outline: none; background: #fafbfc; transition: border-color 0.2s;
  box-sizing: border-box; cursor: pointer; color: #1a1815;
}
.form-select:focus { border-color: #d97706; background: #fff; }

.form-hint {
  font-size: 11px; color: #a8a4a0; margin-top: 4px;
}

/* ─── 复选框 ─── */
.checkbox-group {
  padding: 10px 0;
}

.checkbox-label {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; user-select: none;
}

.checkbox-input {
  display: none;
}

.checkbox-custom {
  width: 18px; height: 18px;
  border: 2px solid #d4cec6;
  border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.checkbox-input:checked + .checkbox-custom {
  background: #d97706;
  border-color: #d97706;
}

.checkbox-input:checked + .checkbox-custom::after {
  content: '✓';
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.checkbox-text {
  font-size: 14px; font-weight: 600; color: #1a1815;
}

.checkbox-hint {
  font-size: 11px; color: #a8a4a0;
  margin-top: 4px; margin-left: 26px;
}

/* ─── 底部 ─── */
.dialog-footer {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 16px 24px 20px; border-top: 1px solid #f3efe8; margin-top: 4px;
}

.btn-cancel {
  padding: 8px 20px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  background: #fff; color: #6b6560; font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel:hover:not(:disabled) { background: #f5f2ed; }
.btn-cancel:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-save {
  padding: 8px 24px; border: none; border-radius: 8px;
  background: #d97706; color: #fff; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; gap: 4px;
}
.btn-save:hover:not(:disabled) { background: #b45309; }
.btn-save:disabled { opacity: 0.45; cursor: not-allowed; }

.spinner {
  display: inline-block; width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.4); border-top-color: #fff;
  border-radius: 50%; animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ─── 过渡动画 ─── */
.dialog-fade-enter-active { transition: all 0.25s ease-out; }
.dialog-fade-leave-active { transition: all 0.2s ease-in; }
.dialog-fade-enter-from { opacity: 0; }
.dialog-fade-leave-to { opacity: 0; }
</style>
