import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { worldApi } from '@/api/world'

export const STATUS_CONFIG = {
  draft: { label: '草稿', color: '#92400E', bg: '#FEF3C7', icon: '○' },
  completed: { label: '已完成', color: '#065F46', bg: '#D1FAE5', icon: '●' },
  needs_work: { label: '待完善', color: '#991B1B', bg: '#FEE2E2', icon: '◐' }
}

export const useWorldBuildingStore = defineStore('world-building', () => {
  const loading = ref(false)
  const saving = ref(false)
  const error = ref(null)
  const currentProjectId = ref(null)

  // 预设分类（用户手动新建设定时使用，存储值用英文）
  const presetCategories = ref([
    { id: 'geography', name: '地理版图', icon: '🌍', description: '山川河流、城市疆域', color: '#2A9D8F' },
    { id: 'history', name: '历史年表', icon: '📜', description: '重大事件、传奇时代', color: '#E9C46A' },
    { id: 'culture', name: '文化社会', icon: '🏛️', description: '民俗风情、社会结构', color: '#4A9EFF' },
    { id: 'magic', name: '力量体系', icon: '🔮', description: '魔法、武道、异能', color: '#9B59B6' },
    { id: 'technology', name: '科技文明', icon: '⚙️', description: '器械、机关、奇术', color: '#5D6D7E' },
    { id: 'races', name: '种族设定', icon: '👥', description: '种族特征、文化传承', color: '#E76F51' },
    { id: 'religion', name: '信仰神明', icon: '🕊️', description: '宗教派系、神祇传说', color: '#F4A261' },
    { id: 'politics', name: '政治势力', icon: '⚖️', description: '王国派系、势力纷争', color: '#3D5A80' }
  ])

  // 动态分类元信息（AI 生成的常见中文分类 → 图标/颜色）
  const DYNAMIC_CATEGORY_META = {
    '时代背景': { icon: '⏳', color: '#8B5CF6' },
    '地理版图': { icon: '🗺️', color: '#2A9D8F' },
    '历史年表': { icon: '📜', color: '#E9C46A' },
    '力量体系': { icon: '⚡', color: '#9B59B6' },
    '政治势力': { icon: '🏰', color: '#3D5A80' },
    '核心规则': { icon: '🔥', color: '#E76F51' },
    '文化社会': { icon: '🏛️', color: '#4A9EFF' },
    '科技文明': { icon: '🔧', color: '#5D6D7E' },
    '种族设定': { icon: '🧬', color: '#E76F51' },
    '信仰神明': { icon: '🙏', color: '#F4A261' },
    '生态环境': { icon: '🌿', color: '#14B8A6' },
    '经济商业': { icon: '💰', color: '#10B981' },
    '其他': { icon: '📌', color: '#6366F1' },
    'uniqueRules': { icon: '🔥', color: '#E76F51' },
    'core': { icon: '🔥', color: '#E76F51' },
    'magic': { icon: '⚡', color: '#9B59B6' },
    'other': { icon: '📌', color: '#6366F1' }
  }

  // 英文ID到中文名称的映射
  const CATEGORY_ID_TO_NAME = {
    'era': '时代背景',
    'geography': '地理版图',
    'history': '历史年表',
    'powerSystem': '力量体系',
    'magicSystem': '力量体系',
    'magic': '力量体系',
    'factions': '政治势力',
    'factionList': '政治势力',
    'politics': '政治势力',
    'uniqueRules': '核心规则',
    'core': '核心规则',
    'culture': '文化社会',
    'technology': '科技文明',
    'races': '种族设定',
    'religion': '信仰神明',
    'gods': '信仰神明',
    'ecology': '生态环境',
    'economy': '经济商业',
    'other': '其他'
  }

  // 分类归一化：英文 ID → 中文显示名；已是中文原样返回（老数据兼容）
  function normalizeCategory(cat) {
    if (!cat) return ''
    return CATEGORY_ID_TO_NAME[cat] || cat
  }

  // 分类匹配：数据分类与卡片分类统一转中文后比较，兼容中英文混杂的历史数据
  function categoryMatches(dataCategory, catId, catName) {
    const dataCn = normalizeCategory(dataCategory)
    return dataCn === catId || dataCn === catName || dataCn === normalizeCategory(catId)
  }

  // 动态分类：预设分类 + 数据中实际出现的其他分类（AI 生成的中文分类自动生成卡片）
  // 去重判断统一转中文后比较：数据 category='era' 与 '时代背景' 视为同一分类，
  // 命中预设（如 id='geography' 与 name='地理版图'）时不重复创建卡片
  const categories = computed(() => {
    const merged = [...presetCategories.value]
    const knownCns = new Set(merged.map(c => normalizeCategory(c.id)))
    for (const s of settings.value) {
      const cat = s.category
      if (!cat) continue
      const chineseName = normalizeCategory(cat)
      if (knownCns.has(chineseName)) continue
      knownCns.add(chineseName)
      const meta = DYNAMIC_CATEGORY_META[cat] || DYNAMIC_CATEGORY_META[chineseName] || {}
      merged.push({
        id: chineseName,
        name: chineseName,
        icon: meta.icon || '📌',
        description: 'AI 生成模块',
        color: meta.color || '#6366F1'
      })
    }
    return merged
  })

  const settings = ref([])
  const selectedCategoryId = ref(null)
  const selectedSettingId = ref(null)
  const searchQuery = ref('')
  const statusFilter = ref('all')
  const sortBy = ref('updatedAt')

  const selectedCategory = computed(() =>
    categories.value.find(c => c.id === selectedCategoryId.value)
  )

  const selectedSetting = computed(() =>
    settings.value.find(s => s.id === selectedSettingId.value)
  )

  const filteredSettings = computed(() => {
    let result = settings.value

    if (selectedCategoryId.value) {
      // 兼容中英文分类：统一转中文后匹配，历史英文 ID 数据也能正确归类
      const cat = categories.value.find(c => c.id === selectedCategoryId.value)
      const catId = cat?.id || selectedCategoryId.value
      const catName = cat?.name || selectedCategoryId.value
      result = result.filter(s => categoryMatches(s.category, catId, catName))
    }

    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase()
      result = result.filter(s =>
        s.name.toLowerCase().includes(q) ||
        (s.content && s.content.toLowerCase().includes(q))
      )
    }

    if (statusFilter.value !== 'all') {
      result = result.filter(s => s.status === statusFilter.value)
    }

    result = [...result].sort((a, b) => {
      if (sortBy.value === 'updatedAt') {
        return new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0)
      } else if (sortBy.value === 'createdAt') {
        return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
      } else if (sortBy.value === 'name') {
        return a.name.localeCompare(b.name)
      }
      return 0
    })

    return result
  })

  const settingsByCategory = computed(() => {
    const grouped = {}
    for (const cat of categories.value) {
      // 兼容中英文分类：统一转中文后匹配（如 id='geography' 也能匹配 category='地理版图'、'era' 匹配 '时代背景'）
      grouped[cat.id] = settings.value
        .filter(s => categoryMatches(s.category, cat.id, cat.name))
        .sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0))
    }
    return grouped
  })

  const categoryStats = computed(() => {
    const stats = {}
    for (const cat of categories.value) {
      // 兼容中英文分类：统一转中文后匹配
      const catSettings = settings.value.filter(s => categoryMatches(s.category, cat.id, cat.name))
      stats[cat.id] = {
        total: catSettings.length,
        completed: catSettings.filter(s => s.status === 'completed').length,
        draft: catSettings.filter(s => s.status === 'draft').length,
        needsWork: catSettings.filter(s => s.status === 'needs_work').length
      }
    }
    return stats
  })

  const associations = computed(() => {
    const links = []
    for (const s of settings.value) {
      if (s.relatedSettings && Array.isArray(s.relatedSettings)) {
        for (const relatedId of s.relatedSettings) {
          const related = settings.value.find(r => r.id === relatedId)
          if (related) {
            links.push({
              source: s.id,
              target: relatedId,
              sourceName: s.name,
              targetName: related.name,
              sourceCategory: s.category,
              targetCategory: related.category
            })
          }
        }
      }
    }
    return links
  })



  async function fetchSettings(projectId) {
    if (!projectId) return
    currentProjectId.value = projectId
    loading.value = true
    error.value = null

    try {
      const data = await worldApi.listSettings(projectId)
      if (data && Array.isArray(data)) {
        settings.value = data.map(s => ({
          ...s,
          status: s.status || 'draft',
          relatedSettings: s.relatedSettings || []
        }))
      }
    } catch (e) {
      console.warn('Failed to fetch world settings:', e.message)
      settings.value = []
    } finally {
      loading.value = false
    }
  }

  async function createSetting(settingData) {
    if (!currentProjectId.value) return null
    saving.value = true

    try {
      const newSetting = await worldApi.createSetting(currentProjectId.value, {
        ...settingData,
        status: settingData.status || 'draft'
      })
      if (newSetting) {
        settings.value.push({
          ...newSetting,
          status: newSetting.status || 'draft',
          relatedSettings: newSetting.relatedSettings || []
        })
      }
      return newSetting
    } catch (e) {
      error.value = e.message
      return null
    } finally {
      saving.value = false
    }
  }

  async function updateSetting(settingId, updates) {
    if (!currentProjectId.value) return false
    saving.value = true

    try {
      const updated = await worldApi.updateSetting(currentProjectId.value, settingId, updates)
      if (updated) {
        const index = settings.value.findIndex(s => s.id === settingId)
        if (index !== -1) {
          settings.value[index] = {
            ...settings.value[index],
            ...updated,
            relatedSettings: updated.relatedSettings || settings.value[index].relatedSettings || []
          }
        }
      }
      return true
    } catch (e) {
      error.value = e.message
      return false
    } finally {
      saving.value = false
    }
  }

  async function deleteSetting(settingId) {
    if (!currentProjectId.value) return false

    try {
      await worldApi.deleteSetting(currentProjectId.value, settingId)
      settings.value = settings.value.filter(s => s.id !== settingId)
      if (selectedSettingId.value === settingId) {
        selectedSettingId.value = null
      }
      return true
    } catch (e) {
      error.value = e.message
      return false
    }
  }

  function selectCategory(categoryId) {
    selectedCategoryId.value = categoryId
    selectedSettingId.value = null
  }

  function selectSetting(settingId) {
    selectedSettingId.value = settingId
  }

  function clearSelection() {
    selectedCategoryId.value = null
    selectedSettingId.value = null
  }

  function getRelatedSettings(setting) {
    if (!setting || !setting.relatedSettings || !Array.isArray(setting.relatedSettings)) {
      return []
    }
    return settings.value.filter(s => setting.relatedSettings.includes(s.id))
  }

  return {
    loading,
    saving,
    error,
    categories,
    settings,
    selectedCategoryId,
    selectedSettingId,
    searchQuery,
    statusFilter,
    sortBy,
    selectedCategory,
    selectedSetting,
    filteredSettings,
    settingsByCategory,
    categoryStats,
    associations,
    fetchSettings,
    createSetting,
    updateSetting,
    deleteSetting,
    selectCategory,
    selectSetting,
    clearSelection,
    getRelatedSettings,
  }
})