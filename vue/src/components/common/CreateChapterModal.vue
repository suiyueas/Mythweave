<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="close">
      <div class="modal-content">
        <div class="modal-header">
          <span class="modal-title">📝 新建章节</span>
          <button class="btn-close" @click="close">✕</button>
        </div>

        <div class="tab-bar">
          <button class="tab-btn" :class="{ active: activeTab === 'manual' }" @click="activeTab = 'manual'">
            ✍️ 手动创建
          </button>
          <button class="tab-btn" :class="{ active: activeTab === 'ai' }" @click="activeTab = 'ai'">
            ✨ AI 生成标题
          </button>
        </div>

        <div v-if="activeTab === 'manual'" class="tab-body">
          <div class="form-group">
            <label class="form-label">章节序号</label>
            <div class="form-static">第 {{ nextIndex }} 章</div>
          </div>
          <div class="form-group">
            <label class="form-label">章节标题</label>
            <input v-model="chapterTitle" class="form-input" placeholder="请输入章节标题..." maxlength="50" />
          </div>
          <div class="empty-hint">
            <span>📖</span> 创建空白章节后，可在编辑器中使用 AI 扩写生成内容
          </div>
        </div>

        <div v-else class="tab-body">
          <div class="form-group">
            <label class="form-label">章节序号</label>
            <div class="form-static">第 {{ nextIndex }} 章</div>
          </div>

          <!-- 已有标题参考 -->
          <div v-if="existingTitles?.length" class="ref-titles">
            <div class="ref-label">📚 已有标题（风格参考）</div>
            <div class="ref-flow">
              <span v-for="(t, i) in existingTitles" :key="i" class="ref-badge">
                {{ i + 1 }}. {{ t }}
              </span>
            </div>
          </div>

          <!-- 上下文参考素材 -->
          <div class="ref-titles" v-if="contextTags.length">
            <div class="ref-label">📚 上下文参考素材</div>
            <div class="ref-flow">
              <span v-for="tag in contextTags" :key="tag" class="ref-badge" style="font-size:11px">{{ tag }}</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">章节标题</label>
            <div class="input-row">
              <input v-model="chapterTitle" class="form-input flex-1" placeholder="AI 将参考已有风格生成标题..." maxlength="50" />
              <button class="btn-ai-sm" @click="handleGenerateTitle" :disabled="aiTitleLoading">
                <span v-if="aiTitleLoading" class="spinner"></span>
                <span v-else>✨</span>
                {{ aiTitleLoading ? '分析中…' : 'AI 生成标题' }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">剧情方向 <span class="label-hint">（建议填写，让标题更精准）</span></label>
            <textarea v-model="aiDirection" class="form-textarea" placeholder="如：主角突破金丹期、迎来第一次大战……" rows="2" />
          </div>

          <!-- 智能哨兵建议 -->
          <div class="sentinel-section" v-if="activeTab === 'ai'">
            <div class="sentinel-header">
              <span class="sentinel-title">🔍 智能哨兵建议</span>
              <span v-if="alertsLoading" class="sentinel-loading">加载中...</span>
              <span v-else-if="!sentinelAlerts.length" class="sentinel-empty">暂无哨兵建议，继续创作吧 ✨</span>
            </div>
            <div v-if="sentinelAlerts.length" class="sentinel-alerts">
              <span
                v-for="alert in sentinelAlerts"
                :key="alert.id"
                class="sentinel-tag"
                :class="{ selected: selectedSentinelHints.includes(alert.id) }"
                :style="{ borderColor: getAlertColor(alert.severity), color: getAlertColor(alert.severity) }"
                @click="applySentinelHint(alert)"
                :title="'点击填入：' + (alert.description || alert.title)"
              >
                {{ getAlertIcon(alert.type) }} {{ alert.title }}
              </span>
            </div>
            <div v-if="selectedSentinelHints.length" class="sentinel-hint-selected">
              已选择 {{ selectedSentinelHints.length }} 条建议将融入标题生成
            </div>
          </div>

          <!-- 风格微调 -->
          <div class="form-group">
            <label class="form-label">标题基调</label>
            <div class="style-tags">
              <button v-for="s in tones" :key="s" class="style-tag" :class="{ active: selectedTone === s }" @click="selectedTone = s">
                {{ s }}
              </button>
            </div>
          </div>

          <div class="empty-hint" v-if="!aiTitleLoading && chapterTitle">
            <span>✨</span> 标题已生成，点击"创建章节"即可
          </div>

          <!-- 反馈消息 -->
          <div v-if="feedbackMessage" class="feedback-message" :class="feedbackType">
            {{ feedbackMessage }}
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="close">取消</button>
          <button class="btn-create" @click="createChapter" :disabled="!chapterTitle.trim()">
            🚀 创建章节
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { aiApi, sentinelApi } from '@/api'
import { useNovelStore } from '@/stores/novel'
import { requireVip } from '@/services/vipService'

const store = useNovelStore()

const props = defineProps({
  projectId: { type: [Number, String], required: true },
  chapterCount: { type: Number, default: 0 },
  existingTitles: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'created'])

const activeTab = ref('manual')
const chapterTitle = ref('')
const aiDirection = ref('')
const selectedTone = ref('诗意意象')
const aiTitleLoading = ref(false)
const sentinelAlerts = ref([])
const alertsLoading = ref(false)
const feedbackMessage = ref('')
const feedbackType = ref('success')

const tones = ['诗意意象', '精炼凝练', '悬疑暗示', '热血激昂']
const nextIndex = computed(() => props.chapterCount + 1)

const contextTags = computed(() => {
  const tags = []
  if (store.outlines.length) tags.push(`📋 大纲 ${store.outlines.length} 项`)
  if (store.worldSettings.length) tags.push(`🌍 世界观 ${store.worldSettings.length} 项`)
  if (store.characters.length) tags.push(`👤 人物 ${store.characters.length} 位`)
  if (store.plotThreads.length) tags.push(`🎯 情节 ${store.plotThreads.length} 条`)
  return tags
})

const selectedSentinelHints = ref([])

async function loadSentinelAlerts() {
  try {
    alertsLoading.value = true
    const res = await sentinelApi.listAlerts(props.projectId, 'all', 'unresolved', 5)
    if (res?.list) {
      sentinelAlerts.value = res.list.filter(a => a.severity !== 'info')
    }
  } catch (e) {
    console.warn('加载哨兵告警失败：', e.message)
  } finally {
    alertsLoading.value = false
  }
}

function applySentinelHint(alert) {
  const hint = `⚠️ ${alert.title}`
  if (alert.description) {
    aiDirection.value = aiDirection.value ? `${aiDirection.value}\n${hint}：${alert.description}` : `${hint}：${alert.description}`
  } else {
    aiDirection.value = aiDirection.value ? `${aiDirection.value}\n${hint}` : hint
  }
  if (!selectedSentinelHints.value.includes(alert.id)) {
    selectedSentinelHints.value.push(alert.id)
  }
}

function getAlertIcon(type) {
  const icons = {
    foreshadowing: '🔮',
    logic: '🧩',
    character: '👤',
    rhythm: '📊',
    normal: '✨'
  }
  return icons[type] || '⚠️'
}

function getAlertColor(severity) {
  const colors = {
    high: '#dc2626',
    medium: '#f59e0b',
    low: '#3b82f6',
    info: '#8b5cf6'
  }
  return colors[severity] || '#6b6560'
}

async function handleGenerateTitle() {
  if (aiTitleLoading.value) return
  if (!requireVip('章节标题生成')) return
  aiTitleLoading.value = true
  feedbackMessage.value = ''
  try {
    const selectedHints = sentinelAlerts.value
      .filter(a => selectedSentinelHints.value.includes(a.id))
      .map(a => `${a.title}${a.description ? '：' + a.description : ''}`)

    const title = await aiApi.generateTitle(props.projectId, {
      chapterIndex: nextIndex.value,
      direction: aiDirection.value || '',
      existingTitles: props.existingTitles || [],
      tone: selectedTone.value,
      sentinelHints: selectedHints,
      context: {
        outline: store.outlines,
        world: store.worldSettings,
        characters: store.characters,
        plotThreads: store.plotThreads
      }
    })
    if (title && title.trim()) {
      chapterTitle.value = title.trim()
      showFeedback('✨ 标题已生成', 'success')
    } else {
      chapterTitle.value = `第${nextIndex.value}章`
      showFeedback('⚠️ 使用默认标题，可自行修改', 'warning')
    }
  } catch (e) {
    console.error('AI 标题生成失败：', e)
    chapterTitle.value = `第${nextIndex.value}章`
    showFeedback(`⚠️ 生成失败，使用默认标题：${e.message || ''}`, 'warning')
  } finally {
    aiTitleLoading.value = false
  }
}

function showFeedback(msg, type) {
  feedbackMessage.value = msg
  feedbackType.value = type
  setTimeout(() => {
    feedbackMessage.value = ''
  }, 3000)
}

function createChapter() {
  if (!chapterTitle.value.trim()) return
  emit('created', { title: chapterTitle.value.trim(), content: '', status: 'draft' })
  close()
}

function close() { emit('close') }

onMounted(() => {
  loadSentinelAlerts()
})

watch(activeTab, (tab) => {
  if (tab === 'ai' && !chapterTitle.value.trim()) handleGenerateTitle()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.35); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0; transform: scale(0.96); } to { opacity: 1; transform: scale(1); } }

.modal-content {
  background: #fff; border-radius: 16px; width: 100%; max-width: 500px;
  max-height: 90vh; overflow-y: auto; padding: 24px 28px 20px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
}

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.modal-title { font-size: 20px; font-weight: 700; color: #1a1815; }
.btn-close { background: none; border: none; font-size: 20px; color: #9c9690; cursor: pointer; padding: 2px 6px; }

.tab-bar { display: flex; gap: 4px; background: #f3efe8; border-radius: 10px; padding: 4px; margin-bottom: 20px; }
.tab-btn {
  flex: 1; padding: 8px 12px; border: none; border-radius: 8px;
  font-size: 13px; font-weight: 600; cursor: pointer;
  background: transparent; color: #9c9690; transition: all 0.2s;
}
.tab-btn.active { background: #fff; color: #1a1815; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.tab-btn:hover:not(.active) { color: #6b6560; }

.tab-body { padding: 4px 0; }

.form-group { margin-bottom: 14px; }
.form-label { display: block; font-size: 13px; font-weight: 600; color: #6b6560; margin-bottom: 5px; }
.label-hint { font-weight: 400; color: #9c9690; }

.form-static { padding: 9px 12px; background: #faf8f5; border-radius: 8px; font-size: 14px; color: #6b6560; font-weight: 500; }

.form-input {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  font-size: 14px; outline: none; background: #fafbfc; transition: border-color 0.2s;
}
.form-input:focus { border-color: #d97706; background: #fff; }

.form-textarea {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  font-size: 13px; outline: none; resize: vertical; min-height: 56px;
  background: #fafbfc; font-family: inherit; transition: border-color 0.2s;
}
.form-textarea:focus { border-color: #d97706; background: #fff; }

.input-row { display: flex; gap: 8px; align-items: center; }
.input-row .form-input { flex: 1; }

.ref-titles { margin-bottom: 14px; padding: 10px 12px; background: #faf8f5; border-radius: 10px; }
.ref-label { font-size: 12px; font-weight: 600; color: #9c9690; margin-bottom: 6px; }
.ref-flow { display: flex; flex-wrap: wrap; gap: 6px; }
.ref-badge {
  font-size: 12px; color: #6b6560; background: #fff; border: 1px solid #e8e3dc;
  border-radius: 6px; padding: 3px 8px; white-space: nowrap;
}

.style-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.style-tag {
  padding: 5px 12px; border: 1.5px solid #e8e3dc; border-radius: 7px;
  font-size: 12px; font-weight: 500; cursor: pointer; color: #6b6560;
  background: #fff; transition: all 0.15s;
}
.style-tag:hover { border-color: #d97706; color: #d97706; }
.style-tag.active { border-color: #d97706; background: #fffbeb; color: #92400e; font-weight: 600; }

.btn-ai-sm {
  flex-shrink: 0; padding: 9px 14px; border: 1.5px solid #d97706; border-radius: 8px;
  background: #fffbeb; color: #92400e; font-size: 13px; font-weight: 600;
  cursor: pointer; display: flex; align-items: center; gap: 4px;
  transition: all 0.2s; white-space: nowrap;
}
.btn-ai-sm:hover:not(:disabled) { background: #fef3c7; }
.btn-ai-sm:disabled { opacity: 0.6; cursor: wait; }

.spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid #d97706; border-top-color: transparent; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty-hint { text-align: center; padding: 20px 0; color: #9c9690; font-size: 13px; display: flex; align-items: center; justify-content: center; gap: 6px; }

.sentinel-section {
  margin-bottom: 14px;
  padding: 10px 12px;
  background: linear-gradient(135deg, #fef9e7 0%, #fdf6f0 100%);
  border-radius: 10px;
  border: 1px solid #f3e8dc;
}

.sentinel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.sentinel-title {
  font-size: 12px;
  font-weight: 600;
  color: #92400e;
}

.sentinel-loading {
  font-size: 11px;
  color: #9c9690;
}

.sentinel-empty {
  font-size: 11px;
  color: #9c9690;
  font-style: italic;
}

.sentinel-alerts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sentinel-tag {
  font-size: 11px;
  padding: 4px 10px;
  border: 1.5px solid;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
  font-weight: 500;
}
.sentinel-tag:hover {
  opacity: 0.8;
  transform: scale(1.02);
}
.sentinel-tag.selected {
  background: #fef3c7;
}

.sentinel-hint-selected {
  margin-top: 6px;
  font-size: 11px;
  color: #92400e;
  font-weight: 500;
}

.feedback-message {
  text-align: center;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  margin-top: 8px;
  animation: fadeIn 0.2s ease;
}
.feedback-message.success {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
}
.feedback-message.error {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}
.feedback-message.warning {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fcd34d;
}

.modal-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #f3efe8; }
.btn-cancel { padding: 8px 20px; border: 1.5px solid #e8e3dc; border-radius: 8px; background: #fff; color: #6b6560; font-size: 13px; font-weight: 600; cursor: pointer; }
.btn-create { padding: 8px 24px; border: none; border-radius: 8px; background: #d97706; color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-create:hover:not(:disabled) { background: #b45309; }
.btn-create:disabled { opacity: 0.45; cursor: not-allowed; }
</style>