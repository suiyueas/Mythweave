<template>
  <div class="bootstrap-wizard">
    <!-- 顶部标题 -->
    <div class="wizard-header">
      <router-link to="/my-works" class="back-link">← 返回我的作品</router-link>
      <h1>🚀 AI 先导创作</h1>
      <p class="subtitle">让 AI 为你生成完整创作蓝图，再开始写作</p>
    </div>

    <!-- 进度指示器 -->
    <div class="progress-indicators">
      <div class="progress-track"></div>
      <div
        v-for="(stepItem, index) in stepList"
        :key="stepItem.key"
        class="progress-dot"
        :class="{
          active: currentStepIndex === index,
          done: stepStatus[stepItem.key] === 'completed' || stepStatus[stepItem.key] === 'skipped',
          failed: stepStatus[stepItem.key] === 'failed'
        }"
        @click="jumpToStep(index)"
      >
        <span class="dot-number">
          <template v-if="stepStatus[stepItem.key] === 'completed'">✓</template>
          <template v-else-if="stepStatus[stepItem.key] === 'skipped'">−</template>
          <template v-else>{{ index + 1 }}</template>
        </span>
        <span class="dot-label">{{ stepItem.label }}</span>
      </div>
    </div>

    <!-- 步骤 -2: 新建作品 -->
    <div v-if="currentStepIndex === -2" class="step-container">
      <div class="step-header">
        <h2>📝 新建作品</h2>
        <p class="step-desc">填写基本信息，AI 将为你生成完整的创作蓝图</p>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label>作品名称 *</label>
          <input v-model="createForm.title" placeholder="如：星辰剑诀" maxlength="30" />
        </div>
        <div class="form-group">
          <label>作品类型 *</label>
          <select v-model="createForm.genre">
            <option value="">选择类型</option>
            <option v-for="g in genres" :key="g" :value="g">{{ g }}</option>
          </select>
        </div>
        <div class="form-group full-width">
          <label>一句话灵感（核心创意）*</label>
          <div class="desc-row">
            <textarea v-model="createForm.inspiration" rows="2"
              placeholder="如：灵气复苏三千年后，一位少年在垃圾堆里捡到了一把会说话的断剑..."
              maxlength="200"></textarea>
            <button type="button" class="btn-ai-inline" @click="aiGenerateInspiration" :disabled="inspireLoading">
              <span v-if="inspireLoading" class="spinner-sm"></span>
              <span v-else>💡</span>
            </button>
          </div>
          <span class="char-count">{{ createForm.inspiration.length }}/200</span>
        </div>
        <div class="form-group full-width">
          <label>作品简介（可选）</label>
          <div class="desc-row">
            <textarea v-model="createForm.description" rows="2"
              placeholder="简要描述故事背景和核心看点..." maxlength="200"></textarea>
            <button type="button" class="btn-ai-inline" @click="aiGenerateDesc" :disabled="descLoading">
              <span v-if="descLoading" class="spinner-sm"></span>
              <span v-else>✨</span>
            </button>
          </div>
        </div>
        <div class="form-group">
          <label>风格偏好</label>
          <div class="style-tags">
            <button v-for="s in styles" :key="s"
              :class="['style-tag', { active: createForm.style === s }]"
              @click="createForm.style = s">{{ s }}</button>
          </div>
        </div>
        <div class="form-group">
          <label>目标章节数</label>
          <input v-model.number="createForm.targetChapters" type="number" min="10" max="500" />
        </div>
        <div class="form-group">
          <label>目标字数</label>
          <input v-model.number="createForm.targetWordCount" type="number" min="10000" max="10000000" placeholder="如：2000000" />
        </div>
        <div class="form-group">
          <label>起始时间</label>
          <input v-model="createForm.startingTime" type="date" />
        </div>
        <div class="form-group">
          <label>预定完本时间</label>
          <input v-model="createForm.plannedCompletionDate" type="date" />
        </div>
      </div>
      <p v-if="createError" class="error-msg-inline">{{ createError }}</p>
      <div class="step-actions step-actions-between">
        <router-link to="/my-works" class="btn-secondary">← 返回</router-link>
        <button class="btn-primary" :disabled="!canCreate || creating" @click="createProject">
          <span v-if="creating" class="spinner"></span>
          <span v-else>✨</span>
          {{ creating ? '创建中…' : '创建作品并开始' }}
        </button>
      </div>
    </div>

    <!-- 步骤 1: 世界观 -->
    <div v-else-if="currentStepIndex === 0" class="step-container">
      <StepWorld
        ref="stepWorldRef"
        :project-id="projectId"
        :params="params"
        :generated-data="generatedData.world"
        :world-settings="worldSettingsList"
        @generated="onStepGenerated('world', $event)"
        @skipped="onStepSkipped('world')"
        @next="nextStep"
      />
    </div>

    <!-- 步骤 2: 人物 -->
    <div v-else-if="currentStepIndex === 1" class="step-container">
      <StepCharacters
        ref="stepCharactersRef"
        :project-id="projectId"
        :params="params"
        :world="generatedData.world"
        :generated-data="generatedData.characters"
        @generated="onStepGenerated('characters', $event)"
        @skipped="onStepSkipped('characters')"
        @next="nextStep"
        @prev="prevStep"
      />
    </div>

    <!-- 步骤 3: 大纲 -->
    <div v-else-if="currentStepIndex === 2" class="step-container">
      <StepOutline
        ref="stepOutlineRef"
        :project-id="projectId"
        :params="params"
        :world="generatedData.world"
        :characters="generatedData.characters"
        :generated-data="generatedData.outline"
        @generated="onStepGenerated('outline', $event)"
        @skipped="onStepSkipped('outline')"
        @next="nextStep"
        @prev="prevStep"
      />
    </div>

    <!-- 步骤 4: 情节引擎 -->
    <div v-else-if="currentStepIndex === 3" class="step-container">
      <StepPlot
        ref="stepPlotRef"
        :project-id="projectId"
        :params="params"
        :outline="generatedData.outline"
        :characters="generatedData.characters"
        :generated-data="generatedData.plot"
        @generated="onStepGenerated('plot', $event)"
        @skipped="onStepSkipped('plot')"
        @next="nextStep"
        @prev="prevStep"
      />
    </div>

    <!-- 步骤 5: 灵感素材 -->
    <div v-else-if="currentStepIndex === 4" class="step-container">
      <StepInspirations
        ref="stepInspirationsRef"
        :project-id="projectId"
        :params="params"
        :world="generatedData.world"
        :characters="generatedData.characters"
        :outline="generatedData.outline"
        :plot="generatedData.plot"
        :generated-data="generatedData.inspirations"
        @generated="onStepGenerated('inspirations', $event)"
        @skipped="onStepSkipped('inspirations')"
        @complete="finishWizard"
        @prev="prevStep"
      />
    </div>

    <!-- 底部 -->
    <div class="wizard-footer">
      <button v-if="currentStepIndex >= 0" class="btn-skip-all" @click="skipAll">
        ⏭️ 跳过全部，直接开始写作
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { setupApi } from '@/api/setup'
import { aiApi } from '@/api'
import { useNovelStore } from '@/stores/novel'

import StepWorld from './steps/StepWorld.vue'
import StepCharacters from './steps/StepCharacters.vue'
import StepOutline from './steps/StepOutline.vue'
import StepPlot from './steps/StepPlot.vue'
import StepInspirations from './steps/StepInspirations.vue'

const router = useRouter()
const route = useRoute()
const store = useNovelStore()

// ─── 作品ID：优先从路由获取，其次通过新建作品动态设置 ───
const localProjectId = ref(route.params.projectId || null)
const projectId = computed(() => localProjectId.value || route.params.projectId)

// ─── 步骤配置 ───
const stepList = [
  { key: 'world', label: '世界观' },
  { key: 'characters', label: '人物群像' },
  { key: 'outline', label: '大纲结构' },
  { key: 'plot', label: '情节引擎' },
  { key: 'inspirations', label: '灵感素材' }
]

// ─── 表单参数 ───
const genres = ['玄幻', '科幻', '悬疑', '言情', '都市', '历史', '奇幻', '武侠', '青春', '浪漫', '恐怖', '惊悚', '冒险', '战争', '谍战', '军事', '悬疑推理', '科幻末世', '科幻星际', '奇幻异世', '奇幻魔法', '玄幻修仙', '玄幻异火', '玄幻剑道', '仙侠', '轻小说', '同人', '短篇小说', '诗歌', '散文']
const styles = ['诗意', '平实', '热血', '悬疑', '温情']
const params = reactive({
  title: '',
  genre: '',
  inspiration: '',
  style: '诗意',
  targetChapters: 30
})

// ─── 新建作品表单 ───
const today = new Date().toISOString().slice(0, 10)
const createForm = reactive({
  title: '',
  genre: '',
  inspiration: '',
  description: '',
  style: '诗意',
  targetChapters: 30,
  targetWordCount: null,
  startingTime: today,
  plannedCompletionDate: ''
})
const creating = ref(false)
const createError = ref('')

const canCreate = computed(() =>
  createForm.title.trim() && createForm.genre && createForm.inspiration.trim()
)

// ─── AI 加载状态 ───
const inspireLoading = ref(false)
const descLoading = ref(false)

// ─── AI 生成灵感 ───
async function aiGenerateInspiration() {
  if (inspireLoading.value) return
  inspireLoading.value = true
  try {
    const genre = createForm.genre || '奇幻'
    const title = createForm.title.trim() || '未命名作品'
    const prompt = `你是一位创意写作专家。请根据以下信息，生成一句话核心创意（30-80字），用于小说创作灵感：\n\n作品名称：《${title}》\n作品类型：${genre}\n\n要求：一句话说清核心冲突或独特设定，带有悬念或吸引力，与「${genre}」类型的经典要素相关。直接输出创意内容，不要任何前缀后缀。`
    const result = await aiApi.chat(0, prompt)
    const text = typeof result === 'string' ? result : (result?.content || result?.text || '')
    if (text.trim()) {
      createForm.inspiration = text.trim().replace(/^["「《]|["」》]$/g, '').slice(0, 200)
    }
  } catch (e) {
    console.warn('AI 灵感生成失败：', e.message)
  } finally {
    inspireLoading.value = false
  }
}

// ─── AI 生成简介 ───
async function aiGenerateDesc() {
  if (descLoading.value) return
  descLoading.value = true
  try {
    const genre = createForm.genre || '奇幻'
    const title = createForm.title.trim() || '未命名作品'
    const inspiration = createForm.inspiration.trim() || ''
    const prompt = `你是一位小说营销编辑。请根据以下信息，生成一段吸引人的作品简介（50-150字）：\n\n作品名称：《${title}》\n作品类型：${genre}\n核心灵感：${inspiration}\n\n要求：要有悬念感，突出核心冲突，吸引读者。直接输出简介内容，不要前缀后缀。`
    const result = await aiApi.chat(0, prompt)
    const text = typeof result === 'string' ? result : (result?.content || result?.text || '')
    if (text.trim()) {
      createForm.description = text.trim().replace(/^["「《]|["」》]$/g, '').slice(0, 200)
    }
  } catch (e) {
    console.warn('AI 简介生成失败：', e.message)
  } finally {
    descLoading.value = false
  }
}

// ─── 状态 ───
const currentStepIndex = ref(-2) // -2 = 新建作品
const generatedData = reactive({
  world: null,
  characters: null,
  outline: null,
  plot: null,
  inspirations: null
})

// 从数据库加载的 world_setting 记录数组（动态模块，直接传给 StepWorld 渲染）
const worldSettingsList = ref([])

const stepStatus = reactive({
  world: 'pending',
  characters: 'pending',
  outline: 'pending',
  plot: 'pending',
  inspirations: 'pending'
})

// ─── 子组件引用 ───
const stepWorldRef = ref(null)
const stepCharactersRef = ref(null)
const stepOutlineRef = ref(null)
const stepPlotRef = ref(null)
const stepInspirationsRef = ref(null)

// ─── 导航方法 ───

async function createProject() {
  if (!canCreate.value || creating.value) return
  creating.value = true
  createError.value = ''
  try {
    const data = {
      title: createForm.title.trim(),
      genre: createForm.genre,
      description: createForm.description || '',
      targetWordCount: createForm.targetWordCount ?? null,
      startingTime: createForm.startingTime || '',
      plannedCompletionDate: createForm.plannedCompletionDate || null
    }
    const result = await store.createProject(data)
    const newId = result?.id || result?.data?.id
    if (!newId) throw new Error('创建作品失败：未获取到作品ID')
    localProjectId.value = String(newId)
    // 预填 AI 参数
    params.title = createForm.title.trim()
    params.genre = createForm.genre
    params.inspiration = createForm.inspiration.trim()
    params.style = createForm.style
    params.targetChapters = createForm.targetChapters
    currentStepIndex.value = 0
  } catch (e) {
    createError.value = e.message || '创建失败，请稍后重试'
  } finally {
    creating.value = false
  }
}

function nextStep() {
  if (currentStepIndex.value < stepList.length - 1) {
    currentStepIndex.value++
  }
}

function prevStep() {
  if (currentStepIndex.value > 0) {
    currentStepIndex.value--
  }
}

function jumpToStep(index) {
  if (index < currentStepIndex.value) {
    currentStepIndex.value = index
  }
}

// ─── 数据回调 ───

function onStepGenerated(stepKey, data) {
  generatedData[stepKey] = data
  stepStatus[stepKey] = 'completed'
  // 生成完成后立即刷新 Store 对应模块
  const pid = projectId.value
  if (pid) {
    const refreshMap = {
      world: () => store.refreshWorld(pid),
      characters: () => store.refreshCharacters(pid),
      outline: () => store.refreshOutlines(pid),
      plot: () => store.refreshPlot(pid),
      inspirations: () => store.refreshInspirations(pid)
    }
    const fn = refreshMap[stepKey]
    if (fn) fn().catch(e => console.warn('刷新数据失败：', e))
  }
}

function onStepSkipped(stepKey) {
  stepStatus[stepKey] = 'skipped'
}

// ─── 跳过 / 完成 ───

function skipAll() {
  if (confirm('跳过所有 AI 生成步骤，直接进入写作？')) {
    finishWizard()
  }
}

function finishWizard() {
  const pid = projectId.value
  if (!pid) {
    router.push('/my-works')
    return
  }
  // 最后刷新所有模块，确保 Store 数据最新
  store.refreshAll(pid).finally(() => {
    store.selectProject({ id: pid, title: params.title })
    router.push(`/my-works/${pid}?tool=aiWrite`)
  })
}

// ─── 加载已有设定 ───
onMounted(async () => {
  const pid = projectId.value
  // 无作品ID → 显示新建作品界面
  if (!pid) {
    currentStepIndex.value = -2
    return
  }

  try {
    const setup = await setupApi.getSetup(projectId.value)
    if (setup) {
      // 检查各模块是否已有数据
      const hasWorld = !!(setup.worldSettings && setup.worldSettings.length > 0)
      const hasCharacters = !!(setup.characters && setup.characters.length > 0)
      const hasOutline = !!(setup.outlines && setup.outlines.length > 0)
      const hasPlot = !!(setup.plotThreads && setup.plotThreads.length > 0)
      const hasInspirations = !!(setup.inspirations && setup.inspirations.length > 0)
  
      if (hasWorld) stepStatus.world = 'completed'
      if (hasCharacters) stepStatus.characters = 'completed'
      if (hasOutline) stepStatus.outline = 'completed'
      if (hasPlot) stepStatus.plot = 'completed'
      if (hasInspirations) stepStatus.inspirations = 'completed'
  
      // 将数据库中的 world_setting 记录数组直接传给 StepWorld 动态渲染
      if (hasWorld) {
        worldSettingsList.value = setup.worldSettings
      }
  
      // 加载项目信息到表单
      if (setup.project) {
        params.title = setup.project.title || ''
        params.genre = setup.project.genre || ''
      }
  
      // 跳转到第一个未完成步骤
      const keys = ['world', 'characters', 'outline', 'plot', 'inspirations']
      const firstPendingIdx = keys.findIndex(k => stepStatus[k] === 'pending')
      if (firstPendingIdx === -1) {
        // 全部完成，跳到编辑器
        router.push(`/my-works/${projectId.value}?tool=aiWrite`)
      } else {
        currentStepIndex.value = firstPendingIdx
      }
    } else {
      // 无已有数据，从参数输入开始
      currentStepIndex.value = -1
    }
  } catch (error) {
    console.warn('加载已有设定失败：', error)
    currentStepIndex.value = -1
  }
})


</script>

<style scoped>
.bootstrap-wizard {
  max-width: 820px;
  margin: 0 auto;
  padding: 32px 24px 40px;
  min-height: 100vh;
  background: #faf8f5;
}

.wizard-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
}

.wizard-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 15px;
  color: #94a3b8;
  margin: 0;
}

.back-link {
  position: absolute;
  left: 0;
  top: 4px;
  font-size: 14px;
  color: #6b6560;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s;
}
.back-link:hover { color: #6366f1; }

/* ─── 进度点 ─── */
.progress-indicators {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
  padding: 0 12px;
  position: relative;
}

.progress-track {
  position: absolute;
  top: 14px;
  left: 38px;
  right: 38px;
  height: 2px;
  background: #e8e3dc;
  z-index: 0;
}

.progress-dot {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  z-index: 1;
  opacity: 0.4;
  transition: all 0.3s;
}

.progress-dot.active {
  opacity: 1;
}

.progress-dot.done {
  opacity: 1;
}

.progress-dot.failed {
  opacity: 1;
}

.progress-dot.failed .dot-number {
  background: #ef4444;
  color: #fff;
}

.dot-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e8e3dc;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  transition: all 0.3s;
  margin-bottom: 6px;
}

.progress-dot.active .dot-number {
  background: #6366f1;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.2);
}

.progress-dot.done .dot-number {
  background: #10b981;
  color: #fff;
}

.dot-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
  white-space: nowrap;
}

.progress-dot.active .dot-label {
  color: #1a1a2e;
  font-weight: 700;
}

.progress-dot.done .dot-label {
  color: #10b981;
}

/* ─── 步骤容器 ─── */
.step-container {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e8e3dc;
  padding: 28px 32px;
  min-height: 360px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

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

/* ─── 参数输入表单 ─── */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}
.form-group.full-width { grid-column: 1 / -1; }
.form-group { display: flex; flex-direction: column; gap: 5px; position: relative; }
.form-group label { font-size: 13px; font-weight: 600; color: #334155; }
.form-group input, .form-group select, .form-group textarea {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e2e8f0; border-radius: 10px;
  font-size: 14px; background: #fafbfc; outline: none; font-family: inherit;
  transition: border-color 0.2s; box-sizing: border-box;
}
.form-group input:focus, .form-group select:focus, .form-group textarea:focus {
  border-color: #818cf8; background: #fff;
}
.form-group textarea { resize: vertical; min-height: 70px; }
.char-count { position: absolute; right: 8px; bottom: 8px; font-size: 11px; color: #94a3b8; }

.style-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.style-tag {
  padding: 5px 14px; border: 1.5px solid #e2e8f0; border-radius: 8px;
  font-size: 13px; cursor: pointer; color: #6b6560; background: #fff; transition: all 0.15s;
}
.style-tag:hover { border-color: #818cf8; color: #818cf8; }
.style-tag.active { background: #eef2ff; border-color: #6366f1; color: #4338ca; font-weight: 600; }

.step-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.step-actions-between {
  justify-content: space-between;
  align-items: center;
}

.btn-primary {
  padding: 14px 48px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.3);
}
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4); }
.btn-primary:active:not(:disabled) { transform: translateY(0); box-shadow: 0 2px 12px rgba(99, 102, 241, 0.3); }
.btn-primary:focus { outline: none; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

/* ─── 错误提示 ─── */
.error-msg-inline {
  color: #be123c;
  font-size: 13px;
  margin: -8px 0 12px 0;
}

/* ─── AI 内联按钮 & 描述行 ─── */
.desc-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.desc-row textarea {
  flex: 1;
}

.btn-ai-inline {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1.5px solid #e2e8f0;
  background: linear-gradient(135deg, #fafbfc, #f1f5f9);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.2s;
  margin-top: 1px;
}
.btn-ai-inline:hover:not(:disabled) {
  border-color: #818cf8;
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}
.btn-ai-inline:disabled {
  opacity: 0.5;
  cursor: wait;
}

.spinner-sm {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(99, 102, 241, 0.25);
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ─── 底部 ─── */
.wizard-footer {
  text-align: center;
  margin-top: 24px;
}

.btn-skip-all {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 16px;
  transition: color 0.2s;
}
.btn-skip-all:hover { color: #6b6560; }

/* ─── 响应式 ─── */
@media (max-width: 640px) {
  .bootstrap-wizard { padding: 16px; }
  .progress-indicators { padding: 0; }
  .progress-track { left: 20px; right: 20px; }
  .dot-label { font-size: 9px; }
  .step-container { padding: 20px 16px; }
  .wizard-header h1 { font-size: 22px; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>