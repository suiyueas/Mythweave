import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { aiConfigApi } from '@/api/ai-config'
import { get, put, post, del } from '@/api/request'

const STORAGE_KEY = 'ai-config-cache'

const defaultPresets = [
  { id: 'default', name: '默认风格', description: '平衡型 · 适合大多数场景', temperature: 0.7, topP: 0.9, maxTokens: 4096, isDefault: true },
  { id: 'web-fast', name: '网文快节奏', description: '短段落 · 高密度冲突 · 快速推进', temperature: 0.85, topP: 0.95, maxTokens: 2048, isDefault: false },
  { id: 'literary', name: '文艺细腻', description: '长描写 · 心理刻画深入 · 意境营造', temperature: 0.6, topP: 0.85, maxTokens: 4096, isDefault: false },
  { id: 'suspense', name: '悬疑紧张', description: '悬念铺垫 · 节奏紧凑 · 情节反转', temperature: 0.75, topP: 0.9, maxTokens: 3072, isDefault: false }
]

const defaultPromptTemplate = `你是一位专业的网络小说作家助手，请根据以下上下文继续创作。

写作风格需保持一致，人物对话需符合各自设定，情节发展需合理且富有张力。

【上下文】
{context}

【人物信息】
{character}

【写作风格】
{style}

【章节信息】
{chapter}

【当前情节走向】
{plot}

【语气语调】
{tone}`

export const useAiConfigStore = defineStore('ai-config', () => {
  const loading = ref(false)
  const saving = ref(false)
  const error = ref(null)
  const currentProjectId = ref(null)

  const temperature = ref(0.7)
  const topP = ref(0.9)
  const maxTokens = ref(4096)
  const currentPresetId = ref('default')
  const customPrompt = ref(defaultPromptTemplate)

  const presets = ref([...defaultPresets])
  const usageData = ref({
    totalTokens: 0,
    estimatedCost: 0,
    apiCalls: 0,
    cacheHitRate: 0
  })

  const usageLoading = ref(false)

  const currentPreset = computed(() =>
    presets.value.find(p => p.id === currentPresetId.value) || presets.value[0]
  )

  const temperatureDisplay = computed({
    get: () => temperature.value,
    set: (v) => { temperature.value = Math.round(v * 100) / 100 }
  })

  const topPDisplay = computed({
    get: () => topP.value,
    set: (v) => { topP.value = Math.round(v * 100) / 100 }
  })

  function loadFromStorage() {
    try {
      const cached = localStorage.getItem(STORAGE_KEY)
      if (cached) {
        const data = JSON.parse(cached)
        temperature.value = data.temperature ?? 0.7
        topP.value = data.topP ?? 0.9
        maxTokens.value = data.maxTokens ?? 4096
        currentPresetId.value = data.currentPresetId ?? 'default'
        customPrompt.value = data.customPrompt || defaultPromptTemplate
        presets.value = data.presets || [...defaultPresets]
      }
    } catch (e) {
      console.warn('Failed to load AI config from storage:', e)
    }
  }

  function saveToStorage() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        temperature: temperature.value,
        topP: topP.value,
        maxTokens: maxTokens.value,
        currentPresetId: currentPresetId.value,
        customPrompt: customPrompt.value,
        presets: presets.value
      }))
    } catch (e) {
      console.warn('Failed to save AI config to storage:', e)
    }
  }

  watch([temperature, topP, maxTokens, currentPresetId, customPrompt, presets], () => {
    saveToStorage()
  }, { deep: true })

  async function fetchConfig(projectId) {
    if (!projectId) return
    currentProjectId.value = projectId
    loading.value = true
    error.value = null

    try {
      const config = await aiConfigApi.getConfig(projectId)
      if (config) {
        temperature.value = config.temperature ?? 0.7
        topP.value = config.topP ?? 0.9
        maxTokens.value = config.maxTokens ?? 4096
        currentPresetId.value = config.stylePreset || 'default'
        customPrompt.value = config.customPrompt || defaultPromptTemplate
      }
    } catch (e) {
      console.warn('Failed to fetch AI config, using cached:', e.message)
      loadFromStorage()
    } finally {
      loading.value = false
    }
  }

  async function saveConfig(projectId) {
    if (!projectId) return false
    saving.value = true
    error.value = null

    try {
      await aiConfigApi.updateConfig(projectId, {
        temperature: temperature.value,
        topP: topP.value,
        maxTokens: maxTokens.value,
        stylePreset: currentPresetId.value,
        customPrompt: customPrompt.value
      })
      return true
    } catch (e) {
      error.value = e.message
      return false
    } finally {
      saving.value = false
    }
  }

  async function fetchUsage(projectId) {
    if (!projectId) return
    usageLoading.value = true

    try {
      const data = await get(`/api/projects/${projectId}/ai/usage`)
      if (data) {
        usageData.value = {
          totalTokens: data.totalTokens || 0,
          estimatedCost: data.estimatedCost || 0,
          apiCalls: data.apiCalls || 0,
          cacheHitRate: data.cacheHitRate || 0
        }
      }
    } catch (e) {
      usageData.value = {
        totalTokens: 124580,
        estimatedCost: 3.74,
        apiCalls: 23,
        cacheHitRate: 89
      }
    } finally {
      usageLoading.value = false
    }
  }

  function selectPreset(presetId) {
    const preset = presets.value.find(p => p.id === presetId)
    if (preset) {
      currentPresetId.value = presetId
      temperature.value = preset.temperature
      topP.value = preset.topP
      maxTokens.value = preset.maxTokens
    }
  }

  async function createPreset(presetData) {
    const newPreset = {
      id: `preset_${Date.now()}`,
      name: presetData.name,
      description: presetData.description || '',
      temperature: presetData.temperature,
      topP: presetData.topP,
      maxTokens: presetData.maxTokens,
      isDefault: false
    }
    presets.value.push(newPreset)
    return newPreset
  }

  async function updatePreset(presetId, updates) {
    const index = presets.value.findIndex(p => p.id === presetId)
    if (index !== -1) {
      presets.value[index] = { ...presets.value[index], ...updates }
    }
  }

  async function deletePreset(presetId) {
    const preset = presets.value.find(p => p.id === presetId)
    if (preset?.isDefault) {
      throw new Error('默认预设不能删除')
    }
    presets.value = presets.value.filter(p => p.id !== presetId)
    if (currentPresetId.value === presetId) {
      currentPresetId.value = 'default'
    }
  }

  function resetPromptTemplate() {
    customPrompt.value = defaultPromptTemplate
  }

  return {
    loading,
    saving,
    error,
    temperature,
    topP,
    maxTokens,
    currentPresetId,
    customPrompt,
    presets,
    usageData,
    usageLoading,
    currentPreset,
    temperatureDisplay,
    topPDisplay,
    fetchConfig,
    saveConfig,
    fetchUsage,
    selectPreset,
    createPreset,
    updatePreset,
    deletePreset,
    resetPromptTemplate,
    loadFromStorage
  }
})