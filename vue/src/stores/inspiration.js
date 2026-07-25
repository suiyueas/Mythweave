import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { inspirationApi } from '@/api/inspiration'

const STORAGE_PREFIX = 'novelcraft_inspirations_'
const FILTER_KEY = 'novelcraft_inspiration_filter'

// 灵感类型标签的中文映射
const TYPE_LABELS = {
  dialogue: '对白灵感',
  scene: '场景描写',
  detail: '细节设定',
  reference: '参考资料'
}

const TYPE_LIST = ['dialogue', 'scene', 'detail', 'reference']

function generateId() {
  return 'insp_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}

function loadFromStorage(projectId) {
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + projectId)
    if (raw) {
      const data = JSON.parse(raw)
      // 恢复日期字符串为 Date 对象
      return (data || []).map(item => ({
        ...item,
        createTime: item.createTime ? new Date(item.createTime) : new Date(),
        updateTime: item.updateTime ? new Date(item.updateTime) : new Date(),
        usedTime: item.usedTime ? new Date(item.usedTime) : null
      }))
    }
  } catch (e) {
    console.warn('加载灵感本地存储失败:', e)
  }
  return []
}

function saveToStorage(projectId, items) {
  try {
    localStorage.setItem(STORAGE_PREFIX + projectId, JSON.stringify(items))
  } catch (e) {
    console.warn('保存灵感本地存储失败:', e)
  }
}

function loadFilterState() {
  try {
    const raw = localStorage.getItem(FILTER_KEY)
    if (raw) return JSON.parse(raw)
  } catch (e) { /* ignore */ }
  return { filterType: 'all', sortBy: 'time-desc', searchQuery: '' }
}

function saveFilterState(state) {
  try {
    localStorage.setItem(FILTER_KEY, JSON.stringify(state))
  } catch (e) { /* ignore */ }
}

export const useInspirationStore = defineStore('inspiration', () => {
  // ─── State ───
  const inspirations = ref([])
  const backupInspirations = ref([]) // 用于 API 同步备份
  const loading = ref(false)
  const aiGenerating = ref(false)
  const aiResults = ref([])

  // 筛选/排序/搜索状态（持久化）
  const filterType = ref('all')
  const sortBy = ref('time-desc')
  const searchQuery = ref('')

  // 当前项目ID
  let currentProjectId = null

  // ─── Getters ───
  const typeCounts = computed(() => {
    const counts = { all: inspirations.value.length }
    TYPE_LIST.forEach(t => {
      counts[t] = inspirations.value.filter(i => i.type === t).length
    })
    return counts
  })

  const filteredInspirations = computed(() => {
    let list = [...inspirations.value]

    // 类型筛选
    if (filterType.value !== 'all') {
      list = list.filter(i => i.type === filterType.value)
    }

    // 搜索
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.trim().toLowerCase()
      list = list.filter(i =>
        (i.content && i.content.toLowerCase().includes(q)) ||
        (i.tags && i.tags.toLowerCase().includes(q)) ||
        (i.chapterName && i.chapterName.toLowerCase().includes(q))
      )
    }

    // 高亮置顶
    const highlighted = list.filter(i => i.isHighlight)
    const normal = list.filter(i => !i.isHighlight)

    // 排序
    const sortField = sortBy.value.startsWith('time') ? 'createTime'
      : sortBy.value === 'chapter' ? 'chapterId'
      : sortBy.value === 'used' ? 'isUsed'
      : 'createTime'
    const isDesc = sortBy.value === 'time-desc' || sortBy.value === 'time-asc' ? sortBy.value === 'time-desc'
      : sortBy.value === 'chapter' ? true
      : false

    const sorter = (a, b) => {
      let cmp = 0
      if (sortField === 'createTime') {
        cmp = new Date(b.createTime || 0) - new Date(a.createTime || 0)
        if (!isDesc) cmp = -cmp
      } else if (sortField === 'isUsed') {
        cmp = (a.isUsed ? 1 : 0) - (b.isUsed ? 1 : 0)
      } else if (sortField === 'chapterId') {
        cmp = (b.chapterId || '') > (a.chapterId || '') ? 1 : -1
      }
      return cmp
    }

    highlighted.sort(sorter)
    normal.sort(sorter)

    return [...highlighted, ...normal]
  })

  // ─── Actions ───
  function init(projectId, chapters) {
    currentProjectId = projectId
    // 加载筛选状态
    const saved = loadFilterState()
    filterType.value = saved.filterType || 'all'
    sortBy.value = saved.sortBy || 'time-desc'
    searchQuery.value = saved.searchQuery || ''

    // 优先从 API 加载最新数据，失败时降级到本地缓存
    loadFromApi(projectId, chapters)
  }

  async function loadFromApi(projectId, chapters) {
    if (!projectId) return
    loading.value = true
    try {
      const data = await inspirationApi.list(projectId)
      // API 返回空数组时也更新本地数据（空数组表示确实没有数据）
      const localMap = new Map()
      inspirations.value.forEach(i => localMap.set(i.id, i))

      const apiItems = (data || []).map(item => {
        const local = localMap.get(String(item.id))
        return {
          id: String(item.id),
          projectId: item.projectId,
          type: item.type || 'dialogue',
          content: item.content || '',
          tags: item.tags || '',
          chapterId: item.chapterId || (local ? local.chapterId : null),
          source: item.source || local?.source || 'manual',
          isHighlight: item.isHighlight != null ? item.isHighlight : (local ? local.isHighlight : false),
          isUsed: item.isUsed != null ? item.isUsed : (local ? local.isUsed : false),
          usedTime: item.usedTime || local?.usedTime || null,
          chapterName: local?.chapterName || '',
          createTime: item.createTime ? new Date(item.createTime) : new Date(),
          updateTime: item.updateTime ? new Date(item.updateTime) : new Date()
        }
      })

      inspirations.value = apiItems
      backupInspirations.value = [...apiItems]
      saveToStorage(projectId, apiItems)
    } catch (e) {
      console.warn('从API加载灵感失败，使用本地缓存:', e.message)
      // API 失败时从本地缓存加载
      inspirations.value = loadFromStorage(projectId)
      backupInspirations.value = [...inspirations.value]
    } finally {
      loading.value = false
    }
  }

  function persist() {
    if (currentProjectId) {
      saveToStorage(currentProjectId, inspirations.value)
    }
  }

  function createInspiration(data) {
    const now = new Date()
    const item = {
      id: generateId(),
      type: data.type || 'dialogue',
      content: data.content || '',
      tags: data.tags || '',
      chapterId: data.chapterId || null,
      chapterName: data.chapterName || '',
      source: data.source || 'manual',
      isHighlight: data.isHighlight || false,
      isUsed: false,
      usedTime: null,
      createTime: now,
      updateTime: now
    }
    inspirations.value.unshift(item)
    persist()
    return item
  }

  function updateInspiration(id, data) {
    const idx = inspirations.value.findIndex(i => i.id === id)
    if (idx === -1) return null
    const item = inspirations.value[idx]
    Object.assign(item, {
      ...data,
      updateTime: new Date()
    })
    inspirations.value[idx] = { ...item }
    persist()
    return inspirations.value[idx]
  }

  function deleteInspiration(id) {
    inspirations.value = inspirations.value.filter(i => i.id !== id)
    persist()
  }

  function toggleUsed(id) {
    const item = inspirations.value.find(i => i.id === id)
    if (!item) return
    item.isUsed = !item.isUsed
    item.usedTime = item.isUsed ? new Date() : null
    item.updateTime = new Date()
    persist()
  }

  function setFilter(type) {
    filterType.value = type
    saveFilterState({ filterType: type, sortBy: sortBy.value, searchQuery: searchQuery.value })
  }

  function setSortBy(sort) {
    sortBy.value = sort
    saveFilterState({ filterType: filterType.value, sortBy: sort, searchQuery: searchQuery.value })
  }

  function setSearchQuery(query) {
    searchQuery.value = query
    saveFilterState({ filterType: filterType.value, sortBy: sortBy.value, searchQuery: query })
  }

  async function generateAI(keywords) {
    if (!keywords.trim()) return []
    aiGenerating.value = true
    aiResults.value = []
    try {
      // 尝试调后端 API
      const res = await inspirationApi.aiGenerate(currentProjectId, keywords)
      if (res && res.length > 0) {
        aiResults.value = res
        return res
      }
    } catch (e) {
      console.warn('AI 生成接口调用失败，使用本地降级:', e.message)
    }

    // 本地降级生成
    const fallback = generateLocalFallback(keywords)
    aiResults.value = fallback
    return fallback
  }

  function acceptAiResult(item) {
    return createInspiration({
      type: item.type,
      content: item.content,
      source: 'ai',
      tags: 'AI生成'
    })
  }

  function generateLocalFallback(keywords) {
    const kw = keywords.split(/\s+/).filter(Boolean)
    const k0 = kw[0] || '故事'
    const k1 = kw[1] || '冒险'
    const k2 = kw[2] || '命运'

    const templates = [
      {
        type: 'dialogue',
        content: `「你以为${k0}只是${k1}？不，它背后隐藏着比${k2}更深的秘密。」他低声说道，眼中闪烁着危险的光芒。`,
        source: 'ai'
      },
      {
        type: 'scene',
        content: `${k0}的余晖洒在${k1}的废墟上，空气中弥漫着${k2}的气息。远处传来钟声，沉重而悠远，仿佛在宣告什么。`,
        source: 'ai'
      },
      {
        type: 'detail',
        content: `他的指尖划过${k0}的纹理，那上面刻着古老的符文——每一个符号都在诉说着与${k1}有关的${k2}预言。`,
        source: 'ai'
      }
    ]
    return templates.map((t, i) => ({
      id: 'ai_' + Date.now() + '_' + i,
      ...t
    }))
  }

  return {
    // State
    inspirations, loading, aiGenerating, aiResults,
    filterType, sortBy, searchQuery,
    // Getters
    typeCounts, filteredInspirations,
    // Actions
    init, loadFromApi, createInspiration, updateInspiration, deleteInspiration,
    toggleUsed, setFilter, setSortBy, setSearchQuery, generateAI, acceptAiResult, persist
  }
})