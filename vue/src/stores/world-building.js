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

  const categories = ref([
    { id: 'geography', name: '地理版图', icon: '🌍', description: '山川河流、城市疆域', color: '#2A9D8F' },
    { id: 'history', name: '历史年表', icon: '📜', description: '重大事件、传奇时代', color: '#E9C46A' },
    { id: 'culture', name: '文化社会', icon: '🏛️', description: '民俗风情、社会结构', color: '#4A9EFF' },
    { id: 'magic', name: '力量体系', icon: '🔮', description: '魔法、武道、异能', color: '#9B59B6' },
    { id: 'technology', name: '科技文明', icon: '⚙️', description: '器械、机关、奇术', color: '#5D6D7E' },
    { id: 'races', name: '种族设定', icon: '👥', description: '种族特征、文化传承', color: '#E76F51' },
    { id: 'religion', name: '信仰神明', icon: '🕊️', description: '宗教派系、神祇传说', color: '#F4A261' },
    { id: 'politics', name: '政治势力', icon: '⚖️', description: '王国派系、势力纷争', color: '#3D5A80' }
  ])

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
      result = result.filter(s => s.category === selectedCategoryId.value)
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
      grouped[cat.id] = settings.value
        .filter(s => s.category === cat.id)
        .sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0))
    }
    return grouped
  })

  const categoryStats = computed(() => {
    const stats = {}
    for (const cat of categories.value) {
      const catSettings = settings.value.filter(s => s.category === cat.id)
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