<template>
  <div class="ai-cowriter">
    <!-- 参考素材预览 -->
    <div class="context-preview">
      <div class="context-header">
        <span class="context-title">📚 参考素材</span>
        <span class="context-status">已自动关联 {{ contextCount }} 项</span>
      </div>
      <div class="context-tags">
        <span v-if="outlineCount" class="tag tag-outline">📋 大纲：{{ outlineCount }} 项</span>
        <span v-if="worldCount" class="tag tag-world">🌍 世界观：{{ worldCount }} 项</span>
        <span v-if="charCount" class="tag tag-char">👤 人物：{{ charCount }} 位</span>
        <span v-if="plotCount" class="tag tag-plot">🎯 情节：{{ plotCount }} 条</span>
        <span v-if="inspCount" class="tag tag-insp">💡 灵感：{{ inspCount }} 条</span>
      </div>
      <div class="context-detail">
        <details>
          <summary class="detail-summary">查看所有参考素材</summary>
          <div class="detail-content">
            <div v-if="outlineCount" class="section">
              <h4>📋 大纲</h4>
              <p v-for="o in store.outlines.slice(0, 3)" :key="o.id" class="context-line">- {{ o.title }}</p>
            </div>
            <div v-if="worldCount" class="section">
              <h4>🌍 世界观</h4>
              <p v-for="w in store.worldSettings.slice(0, 3)" :key="w.id" class="context-line">{{ w.category }}：{{ w.name }}</p>
            </div>
            <div v-if="charCount" class="section">
              <h4>👤 人物</h4>
              <p v-for="c in store.characters.slice(0, 5)" :key="c.id" class="context-line">{{ c.name }}（{{ c.role }}）</p>
            </div>
            <div v-if="plotCount" class="section">
              <h4>🎯 情节</h4>
              <p v-for="t in store.plotThreads.slice(0, 3)" :key="t.id" class="context-line">{{ t.name }} · 进度 {{ t.progress }}%</p>
            </div>
            <div v-if="inspCount" class="section">
              <h4>💡 灵感</h4>
              <p v-for="i in store.inspirations.slice(0, 3)" :key="i.id" class="context-line">{{ (i.content || '').slice(0, 50) }}</p>
            </div>
          </div>
        </details>
      </div>
    </div>

    <!-- 额外指令 -->
    <div class="direction-area">
      <label class="direction-label">📝 额外指令（可选）</label>
      <textarea
        v-model="direction"
        class="direction-input"
        placeholder="例如：请重点描写主角的内心挣扎，并暗示下一章的冲突..."
        rows="4"
      />
    </div>

    <!-- 字数控制 -->
    <div class="word-count-control">
      <label class="wc-label">目标字数</label>
      <div class="word-count-options">
        <button
          v-for="count in wordOptions"
          :key="count"
          class="word-option"
          :class="{ active: targetWords === count }"
          @click="targetWords = count"
        >{{ count.toLocaleString() }}</button>
      </div>
    </div>

    <!-- 生成按钮 -->
    <div class="actions">
      <button class="btn-cancel" :disabled="!generating" @click="handleCancel">
        {{ generating ? '⏹ 取消中...' : '取消' }}
      </button>
      <button class="btn-generate" :disabled="generating" @click="generate">
        <span v-if="generating" class="spinner"></span>
        <span v-else>✨ 生成章节</span>
      </button>
    </div>

    <!-- 章节不匹配确认对话框 -->
    <div v-if="showMismatchDialog" class="dialog-overlay" @click.self="handleMismatch('cancel')">
      <div class="dialog-content">
        <h3>⚠️ 章节不匹配</h3>
        <p>当前正在编辑 <strong>「{{ currentChapterName }}」</strong></p>
        <p>AI 将生成 <strong>「{{ targetChapterName }}」</strong> 的内容</p>
        <p style="color:#94a3b8;font-size:13px;margin-top:8px;">建议切换到正确章节后生成，避免内容错乱</p>
        <div class="dialog-actions">
          <button class="btn btn-outline" @click="handleMismatch('cancel')">取消</button>
          <button class="btn btn-primary" @click="handleMismatch('switch')">🔄 切换并生成</button>
          <button class="btn btn-outline" @click="handleMismatch('continue')">继续生成</button>
        </div>
      </div>
    </div>

    <!-- 生成过程中切换章节警告弹窗 -->
    <div v-if="showSwitchWarning" class="dialog-overlay">
      <div class="dialog-content warning-dialog">
        <h3>⚠️ 生成过程中切换了章节</h3>
        <p>AI 正在为 <strong>「{{ targetChapterName }}」</strong> 生成内容</p>
        <p>您当前切换到了 <strong>「{{ currentChapterName }}」</strong></p>
        <p style="color:#ef4444;font-size:13px;margin-top:8px;">
          ⚡ AI 已暂停，请选择继续或取消
        </p>
        <div class="dialog-actions">
          <button class="btn btn-outline" @click="handleWarningAction('cancel')">❌ 取消生成</button>
          <button class="btn btn-outline" @click="handleWarningAction('continue')">▶️ 继续生成</button>
          <button class="btn btn-primary" @click="handleWarningAction('switch')">🔄 切换并生成</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useNovelStore } from '@/stores/novel'

const props = defineProps({
  projectId: { type: [Number, String], required: true },
  chapterIndex: { type: Number, default: 0 },
  currentContent: { type: String, default: '' }
})

const emit = defineEmits(['close', 'generated'])

const store = useNovelStore()
const direction = ref('')
const targetWords = ref(2000)
const generating = ref(false)
const generatedContent = ref('')  // 实时累积生成内容
const wordOptions = [1000, 1500, 2000, 3000]

// ─── 生成状态管理 ───
const generatingChapterId = ref(null)    // 正在生成的章节ID（锁定）
const generatingChapterIndex = ref(0)    // 正在生成的章节索引（1-based）
const isPaused = ref(false)              // 是否暂停
const showSwitchWarning = ref(false)      // 切换章节警告弹窗
let warningResolve = null                // 警告弹窗Promise resolver
let pauseResolve = null                  // 暂停Promise resolver

// 当前正在编辑的章节（使用 store.currentChapter，带兜底查找）
const currentChapter = computed(() => {
  if (store.currentChapter) return store.currentChapter
  // 兜底：通过 currentChapterId 从 chapters 列表查找
  if (store.currentChapterId) {
    return store.chapters.find(c => c.id === store.currentChapterId) || null
  }
  return null
})

// 当前章节在列表中的位置（0-based index）
const currentChapterListIndex = computed(() => {
  if (!currentChapter.value) return -1
  return store.chapters.findIndex(c => c.id === currentChapter.value.id)
})

// 当前章节的展示名称（如「第9章 · 翠绿深渊低语」）
const currentChapterName = computed(() => {
  if (!currentChapter.value) return '未选择章节'
  const idx = currentChapterListIndex.value
  const title = currentChapter.value.title || '未命名'
  return idx >= 0 ? `第${idx + 1}章 · ${title}` : title
})

// 目标生成章节（从 props 获取，1-based）
const targetChapterIndex = computed(() => Number(props.chapterIndex) || 0)

// 目标章节的展示名称
const targetChapterName = computed(() => {
  const idx = targetChapterIndex.value
  if (idx <= 0) return '当前章节'
  // 从 chapters 列表查找对应章节
  const ch = store.chapters[idx - 1] // props.chapterIndex 是 1-based
  const title = ch?.title || '未命名'
  return `第${idx}章 · ${title}`
})

// 章节不匹配对话框状态
const showMismatchDialog = ref(false)
let mismatchResolve = null

// ✅ 只在 props 变化时更新（已改为 computed，无需 watch）

// ─── 内容格式化：确保段落间有空行 ───
function formatGeneratedContent(text) {
  if (!text) return text
  // 1. 将连续多个空行合并为两个换行（一个空行）
  let result = text.replace(/\n{3,}/g, '\n\n')
  // 2. 确保对话「」前后有空行（如果前面没有空行）
  result = result.replace(/([^\n])(「)/g, '$1\n\n$2')
  // 3. 确保对话「」后面有空行（如果后面没有空行且不是紧跟文字）
  result = result.replace(/(」)([^\n」])/g, '$1\n\n$2')
  // 4. 确保「---」分隔线前后有空行
  result = result.replace(/([^\n])(---)/g, '$1\n\n$2')
  result = result.replace(/(---)([^\n])/g, '$1\n\n$2')
  // 5. 去除开头的空行
  result = result.replace(/^\n+/, '')
  return result
}

// 各项素材计数
const outlineCount = computed(() => (store.outlines || []).length)
const worldCount = computed(() => (store.worldSettings || []).length)
const charCount = computed(() => (store.characters || []).length)
const plotCount = computed(() => (store.plotThreads || []).length)
const inspCount = computed(() => (store.inspirations || []).length)

const contextCount = computed(() => {
  let n = 0
  if (outlineCount.value) n++
  if (worldCount.value) n++
  if (charCount.value) n++
  if (plotCount.value) n++
  if (inspCount.value) n++
  return n
})

// 章节不匹配确认对话框
function showMismatchConfirm() {
  return new Promise((resolve) => {
    showMismatchDialog.value = true
    mismatchResolve = resolve
  })
}

function handleMismatch(action) {
  showMismatchDialog.value = false
  if (mismatchResolve) {
    mismatchResolve(action)
    mismatchResolve = null
  }
}

// ✅ 监听章节切换，检测生成过程中是否切换了章节
watch(() => store.currentChapterId, (newId, oldId) => {
  // 如果正在生成，且切换了不同章节
  if (generating.value && generatingChapterId.value && newId !== generatingChapterId.value) {
    console.log('⚠️ [生成中切换章节] 从', oldId, '切换到', newId, '，目标章节是', generatingChapterId.value)
    // 显示警告弹窗
    showSwitchWarning.value = true
    // 触发暂停
    triggerPause()
    // 处理警告（异步等待用户选择）
    handleSwitchDuringGeneration()
  }
})

// ─── 警告弹窗处理 ───
function showWarningDialog() {
  return new Promise((resolve) => {
    warningResolve = resolve
  })
}

function handleWarningAction(action) {
  showSwitchWarning.value = false
  isPaused.value = false
  if (warningResolve) {
    warningResolve(action)
    warningResolve = null
  }
}

// ─── 暂停/恢复机制 ───
function triggerPause() {
  isPaused.value = true
}

function resumeGeneration() {
  isPaused.value = false
  if (pauseResolve) {
    pauseResolve()
    pauseResolve = null
  }
}

async function waitForResume() {
  if (!isPaused.value) return
  return new Promise((resolve) => {
    pauseResolve = resolve
  })
}

async function stopGeneration() {
  generating.value = false
  generatingChapterId.value = null
  generatingChapterIndex.value = 0
  isPaused.value = false
  await store.cancelGeneration()
}

function handleCancel() {
  if (generating.value) {
    stopGeneration()
  } else {
    emit('close')
  }
}

async function generate() {
  // ✅ 兜底检查：确保当前章节存在
  if (!store.currentChapterId) {
    alert('⚠️ 请先打开一个章节')
    return
  }

  const currentIdx = currentChapterListIndex.value  // 0-based
  const targetIdx = targetChapterIndex.value         // 1-based (from props)

  console.log('🔍 [章节拦截] 当前列表索引:', currentIdx, '目标章节号:', targetIdx, 'props:', props.chapterIndex)

  // ✅ 只有当两者都有有效值且不一致时才触发拦截
  if (currentIdx >= 0 && targetIdx > 0 && (currentIdx + 1) !== targetIdx) {
    console.log('⚠️ 章节不匹配，触发拦截对话框')
    const action = await showMismatchConfirm()
    if (action === 'cancel') {
      return
    } else if (action === 'switch') {
      // ✅ 切换到目标章节（targetIdx 是 1-based，转为 0-based index）
      const targetChapter = store.chapters[targetIdx - 1]
      if (targetChapter) {
        await store.selectChapter(targetChapter)
        await nextTick()
      } else {
        alert('⚠️ 目标章节不存在')
        return
      }
    }
    // 如果选择 'continue'，直接继续在当前章节生成
  }
  console.log('✅ 章节匹配或用户确认，开始生成')

  // ✅ 锁定目标章节
  const targetChapter = store.chapters.find(c => c.sortOrder === targetIdx)
  generatingChapterId.value = targetChapter?.id || store.currentChapterId
  generatingChapterIndex.value = targetIdx > 0 ? targetIdx : (currentIdx + 1)

  generating.value = true
  generatedContent.value = ''  // 重置累积内容
  isPaused.value = false

  try {
    // 记录流式开始前的编辑器内容
    const originalContent = store.editorContent || ''
    await store.generateChapterWithContext({
      projectId: props.projectId,
      chapterIndex: props.chapterIndex,
      currentContent: props.currentContent,
      direction: direction.value,
      targetWords: targetWords.value,
      lockChapterId: generatingChapterId.value,
      onToken: async (token) => {
        // 如果暂停了，等待恢复
        if (isPaused.value) {
          await waitForResume()
        }
        if (token != null && token !== 'null' && token !== 'undefined') {
          generatedContent.value += token
          store.updateEditorContent(originalContent + '\n\n' + formatGeneratedContent(generatedContent.value))
        }
      }
    })
    // 流式完成：最终格式化并设置编辑器内容
    store.updateEditorContent(originalContent + '\n\n' + formatGeneratedContent(generatedContent.value))
  } catch (e) {
    // 如果是用户主动取消，不显示错误
    if (e.name === 'AbortError' || e.message === '生成已取消') {
      console.log('✅ 生成已取消')
      return
    }
    console.error('生成失败：', e)
    alert('生成失败，请重试')
  } finally {
    generating.value = false
    generatingChapterId.value = null
    generatingChapterIndex.value = 0
    isPaused.value = false
  }
}

// ─── 处理生成中切换章节的警告 ───
async function handleSwitchDuringGeneration() {
  const action = await showWarningDialog()
  if (action === 'cancel') {
    // 取消生成
    await stopGeneration()
    return
  } else if (action === 'switch') {
    // 切换到新章节，继续生成
    generatingChapterId.value = store.currentChapterId
    const ch = store.chapters.find(c => c.id === store.currentChapterId)
    generatingChapterIndex.value = ch?.sortOrder || 0
    resumeGeneration()
  } else if (action === 'continue') {
    // 继续生成（不切换章节）
    // 切回目标章节
    const targetCh = store.chapters.find(c => c.sortOrder === generatingChapterIndex.value)
    if (targetCh && targetCh.id !== store.currentChapterId) {
      await store.selectChapter(targetCh)
      await nextTick()
    }
    generatingChapterId.value = targetCh?.id || store.currentChapterId
    resumeGeneration()
  }
}
</script>

<style scoped>
.ai-cowriter {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.context-preview {
  border: 1px solid #e8e3dc;
  border-radius: 10px;
  overflow: hidden;
}

.context-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #faf8f5;
  border-bottom: 1px solid #e8e3dc;
}

.context-title { font-size: 12px; font-weight: 700; color: #6b6560; }
.context-status { font-size: 10px; color: #059669; font-weight: 500; }

.context-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px 12px;
}

.tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}
.tag-outline { background: #fef3c7; color: #92400e; }
.tag-world { background: #dbeafe; color: #1e40af; }
.tag-char { background: #ede9fe; color: #5b21b6; }
.tag-plot { background: #d1fae5; color: #065f46; }
.tag-insp { background: #ffedd5; color: #9a3412; }

.context-detail { padding: 0 12px 8px; }

.detail-summary {
  font-size: 11px;
  color: #d97706;
  cursor: pointer;
  padding: 4px 0;
}

.detail-content {
  padding: 8px 0;
}

.section { margin-bottom: 8px; }
.section h4 { font-size: 11px; font-weight: 600; color: #6b6560; margin-bottom: 4px; }

.context-line {
  font-size: 10px;
  color: #9c9690;
  margin: 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.direction-area { display: flex; flex-direction: column; gap: 4px; }
.direction-label { font-size: 11px; font-weight: 600; color: #6b6560; }

.direction-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e8e3dc;
  border-radius: 8px;
  font-size: 12px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  background: #fafbfc;
  transition: border-color 0.2s;
}
.direction-input:focus { border-color: #d97706; }

.word-count-control { display: flex; align-items: center; gap: 8px; }
.wc-label { font-size: 11px; font-weight: 600; color: #6b6560; white-space: nowrap; }

.word-count-options { display: flex; gap: 4px; }

.word-option {
  padding: 3px 10px;
  border: 1px solid #e8e3dc;
  border-radius: 6px;
  font-size: 11px;
  background: #fff;
  color: #6b6560;
  cursor: pointer;
  transition: all 0.15s;
}
.word-option:hover { border-color: #d97706; color: #d97706; }
.word-option.active { background: #d97706; color: #fff; border-color: #d97706; }

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.btn-cancel {
  padding: 6px 16px;
  border: 1px solid #e8e3dc;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  background: #fff;
  color: #6b6560;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-cancel:hover { background: #faf8f5; }

.btn-generate {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 20px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  background: #d97706;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(217,119,6,0.2);
}
.btn-generate:hover:not(:disabled) { background: #b45309; transform: translateY(-1px); }
.btn-generate:disabled { opacity: 0.55; cursor: not-allowed; }

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.dialog-content {
  background: #fff;
  border-radius: 16px;
  padding: 28px 32px;
  max-width: 420px;
  width: 90%;
  box-shadow: 0 24px 80px rgba(0,0,0,0.2);
}

.dialog-content h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
}

.dialog-content p {
  margin: 4px 0;
  font-size: 14px;
  color: #1a1a2e;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.dialog-actions .btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  font-size: 13px;
}

.dialog-actions .btn-outline {
  border: 1px solid #e8e3dc;
  background: #fff;
  color: #6b6560;
}

.dialog-actions .btn-outline:hover {
  background: #faf8f5;
}

.dialog-actions .btn-primary {
  border: none;
  background: #d97706;
  color: #fff;
}

.dialog-actions .btn-primary:hover {
  background: #b45309;
}

.warning-dialog {
  border-top: 4px solid #ef4444;
}
</style>