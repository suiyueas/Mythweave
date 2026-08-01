import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { projectApi, chapterApi, characterApi, exportApi, contextApi, sentinelApi } from '@/api'
import { worldApi } from '@/api/world'
import { outlineApi } from '@/api/outline'
import { plotApi } from '@/api/plot'
import { inspirationApi } from '@/api/inspiration'
import { aiApi } from '@/api/ai'
import { dashboardApi } from '@/api/dashboard'

// 判断是否为本地临时 ID（前端生成，未同步到后端）
function isLocalId(id) {
  if (!id) return true
  if (typeof id === 'string' && id.startsWith('temp_')) return true
  if (typeof id === 'object' && id._local) return true
  return false
}

export const useNovelStore = defineStore('novel', () => {
  const projects = ref([])
  const currentProject = ref(null)
  const chapters = ref([])
  const characters = ref([])
  const worldSettings = ref([])
  const outlines = ref([])
  const plotThreads = ref([])
  const inspirations = ref([])
  const loading = ref(false)
  const error = ref(null)

  // ─── 各模块加载状态 ───
  const loadingModules = ref({
    world: false,
    characters: false,
    outline: false,
    plot: false,
    inspirations: false,
    chapters: false
  })

  // ─── 各模块最后更新时间 ───
  const lastUpdated = ref({
    world: null,
    characters: null,
    outline: null,
    plot: null,
    inspirations: null,
    chapters: null
  })

  // ─── 编辑器状态 ───
  const currentChapterId = ref(null)
  const editorContent = ref('')
  const chapterTitle = ref('')
  const isSaving = ref(false)
  const lastSavedAt = ref(null)
  const targetWords = ref(3000)
  const aiSuggestLoading = ref(false)
  const aiExpandLoading = ref(false)

  // ─── AI 生成控制器 ───
  let abortController = null

  // ─── 仪表盘状态 ───
  const dashboardStats = ref({
    todayWords: 0,
    weekWords: 0,
    totalWords: 0,
    writingDuration: 0,
    chapterCount: 0,
    targetWords: 0,
    bestHours: '--',
    avgSpeed: 0
  })
  const heatmapData = ref([])
  const weeklyTrend = ref([])
  const recentActivities = ref([])

  // ─── 导出状态 ───
  const exportHistory = ref([])
  const exportFormats = ref([])
  const exporting = ref(false)
  const exportProgress = ref(0)
  const exportStatusText = ref('')

  // ─── 上下文引擎状态 ───
  const contextStats = ref({ characterVectors: 0, worldEntries: 0, outlineNodes: 0, indexSize: 0, lastFullIndexTime: null })
  const contextConfig = ref({ autoIndex: true, windowSize: 512, topK: 10, hybridWeights: { semantic: 0.7, bm25: 0.3 } })
  const searching = ref(false)
  const searchResults = ref([])
  const contextLoading = ref(false)
  const contextRebuilding = ref(false)
  const contextRebuildProgress = ref(0)
  const contextRebuildTaskId = ref(null)
  const contextHealthCheck = ref(null)
  const contextSizeTrend = ref([])
  const contextOperationStatus = ref(null)
  let contextPollTimer = null

  // ─── 通知 & 哨兵状态 ───
  const notifications = ref([])
  const unreadCount = ref(0)
  const notifLoading = ref(false)
  const sentinelStats = ref({ total: 0, pending: 0, resolved: 0, foreshadowing: 0, logic: 0, character: 0, rhythm: 0, critical: 0, warning: 0 })
  const sentinelAlerts = ref([])
  const sentinelFilter = ref({ type: 'all', status: 'all' })
  const scanning = ref(false)
  const scanningTaskId = ref(null)
  const scanProgress = ref(null)
  const sentinelLogs = ref([])
  const alertDetail = ref(null)
  let scanPollTimer = null

  const sentinelCriticalCount = computed(() => {
    return (sentinelAlerts.value || []).filter(a => a.severity === 'critical' && !a.resolved).length
  })

  const currentProjectId = computed(() => currentProject.value?.id)

  const totalWordCount = computed(() =>
    chapters.value.reduce((sum, ch) => sum + (ch.wordCount || 0), 0)
  )

  const completedChapters = computed(() =>
    chapters.value.filter(ch => ch.status === 'completed' || ch.status === '完成').length
  )

  // ─── 仪表盘计算属性 ───
  const todayWords = computed(() => dashboardStats.value.todayWords || 0)
  const weekWords = computed(() => dashboardStats.value.weekWords || 0)
  const writingDuration = computed(() => dashboardStats.value.writingDuration || 0)
  const totalDashboardChapterCount = computed(() =>
    dashboardStats.value.chapterCount || chapters.value.length
  )
  const targetWordCount = computed(() => dashboardStats.value.targetWords || 0)
  const bestHours = computed(() => dashboardStats.value.bestHours || '--')
  const avgSpeed = computed(() => dashboardStats.value.avgSpeed || 0)
  const progress = computed(() => {
    const target = targetWordCount.value
    return target > 0 ? Math.round(totalWordCount.value / target * 100) : 0
  })

  // ─── 仪表盘数据加载 ───
  async function loadDashboard(projectId) {
    if (!projectId) return
    console.log('📊 开始加载仪表盘数据，项目ID:', projectId)
    try {
      const stats = await dashboardApi.getStats(projectId)
      if (stats) {
        dashboardStats.value = {
          todayWords: stats.todayWords || 0,
          weekWords: stats.weekWords || 0,
          totalWords: stats.totalWords || 0,
          writingDuration: stats.writingDuration || 0,
          chapterCount: stats.chapterCount || 0,
          targetWords: stats.targetWords || 0,
          bestHours: stats.bestHours || '--',
          avgSpeed: stats.avgSpeed || 0
        }
        console.log('📊 仪表盘统计已更新:', dashboardStats.value)
      }
    } catch (e) {
      console.warn('仪表盘统计加载失败：', e.message)
    }

    try {
      const heatmap = await dashboardApi.getHeatmap(projectId)
      heatmapData.value = heatmap || []
      console.log('📅 热力图数据已更新:', heatmapData.value.length, '条')
    } catch (e) {
      console.warn('热力图数据加载失败：', e.message)
    }

    try {
      const activities = await dashboardApi.getRecentActivities(projectId)
      recentActivities.value = activities || []
      console.log('🕐 最近活动已更新:', recentActivities.value.length, '条')
    } catch (e) {
      console.warn('最近活动加载失败：', e.message)
    }

    try {
      const trend = await dashboardApi.getWeeklyTrend(projectId)
      weeklyTrend.value = trend || []
      console.log('📈 本周趋势已更新:', weeklyTrend.value.length, '条')
    } catch (e) {
      console.warn('本周趋势加载失败：', e.message)
    }
  }

  /** 轻量刷新最近活动（策略页“最近操作”等场景用） */
  async function fetchRecentActivities(projectId, limit = 10) {
    if (!projectId) return
    try {
      const activities = await dashboardApi.getRecentActivities(projectId, limit)
      recentActivities.value = activities || []
    } catch (e) {
      console.warn('最近活动刷新失败：', e.message)
    }
  }

  // 编辑器计算属性
  const currentChapter = computed(() =>
    chapters.value.find(c => c.id === currentChapterId.value) || null
  )

  const currentWordCount = computed(() => editorContent.value.length)

  const wordProgress = computed(() =>
    Math.min(100, Math.round((currentWordCount.value / Math.max(1, targetWords.value)) * 100))
  )

  const paragraphCount = computed(() => {
    if (!editorContent.value) return 0
    return editorContent.value.split(/\n\n+/).filter(p => p.trim()).length
  })

  const readingTimeMinutes = computed(() =>
    Math.max(1, Math.ceil(currentWordCount.value / 500))
  )

  const saveStatusText = computed(() => {
    if (isSaving.value) return '保存中...'
    if (lastSavedAt.value) return `已保存 ${lastSavedAt.value}`
    return '未保存'
  })

  const saveStatusColor = computed(() => {
    if (isSaving.value) return '#d97706'
    if (lastSavedAt.value) return '#0d9488'
    return '#9c9690'
  })

  // --- Projects ---
  async function fetchProjects() {
    loading.value = true; error.value = null
    try {
      // 先同步所有作品的统计数据（修复历史数据不一致）
      const syncResult = await projectApi.syncStats().catch(e => {
        console.warn('统计同步失败（非致命）：', e.message)
        return null
      })
      console.log('🔄 作品统计同步完成，同步了', syncResult, '个项目')
      const data = await projectApi.list()
      projects.value = data.records || data || []
      console.log('📚 作品列表已加载:', projects.value.length, '个作品')
    } catch (e) {
      console.warn('项目列表加载失败：', e.message)
      projects.value = []
      error.value = '服务器连接失败，请确认后端服务已启动'
    } finally {
      if (!currentProject.value && projects.value.length) {
        currentProject.value = projects.value[0]
      }
      loading.value = false
    }
  }

  /**
   * 同步项目统计到 projects 列表中
   * 用于章节操作后实时更新「我的作品」界面的字数和章节数
   */
  async function syncProjectStats(projectId) {
    if (!projectId) return
    // 统一转为字符串对比，避免 Number vs String 类型不匹配
    const pid = String(projectId)
    const idx = projects.value.findIndex(p => String(p.id) === pid)
    if (idx < 0) return
    // 仅当本地章节数据真实存在（chapters 已加载且非空，或编辑器有内容）时才用本地计算，
    // 否则回退到后端拉取实时统计——避免 chapters 尚未加载（空数组）时把统计错误覆盖为 0
    const isCurrent = currentProject.value && String(currentProject.value.id) === pid
    const hasEditorContent = currentChapterId.value && editorContent.value
    const hasLocalChapters = chapters.value.length > 0
    if (isCurrent && (hasLocalChapters || hasEditorContent)) {
      // 如果有当前编辑的章节，使用编辑器内容计算字数
      if (hasEditorContent) {
        const currentChIdx = chapters.value.findIndex(c => c.id === currentChapterId.value)
        if (currentChIdx >= 0) {
          // 使用编辑器内容长度作为当前章节字数
          const currentWordCount = editorContent.value.length
          // 计算其他章节的总字数
          const otherWords = chapters.value
            .filter((_, i) => i !== currentChIdx)
            .reduce((sum, ch) => sum + (ch.wordCount || 0), 0)
          const totalWords = otherWords + currentWordCount
          const totalChapters = chapters.value.length
          projects.value[idx] = {
            ...projects.value[idx],
            wordCount: totalWords,
            chapterCount: totalChapters
          }
        } else {
          // 当前章节不在chapters列表中，可能是新章节
          const totalWords = chapters.value.reduce((sum, ch) => sum + (ch.wordCount || 0), 0)
          const totalChapters = chapters.value.length
          projects.value[idx] = {
            ...projects.value[idx],
            wordCount: totalWords,
            chapterCount: totalChapters
          }
        }
      } else {
        // 没有当前编辑的章节，从chapters计算
        const totalWords = chapters.value.reduce((sum, ch) => sum + (ch.wordCount || 0), 0)
        const totalChapters = chapters.value.length
        projects.value[idx] = {
          ...projects.value[idx],
          wordCount: totalWords,
          chapterCount: totalChapters
        }
      }
    } else {
      // 非当前项目 / 本地数据不可信：从后端拉取最新统计，确保数据准确
      try {
        const p = await projectApi.getById(typeof projectId === 'string' ? parseInt(projectId) : projectId)
        if (p) {
          const idx2 = projects.value.findIndex(x => String(x.id) === pid)
          if (idx2 >= 0) {
            projects.value[idx2] = { ...projects.value[idx2], wordCount: p.wordCount, chapterCount: p.chapterCount }
          }
        }
      } catch (e) {
        console.warn('同步项目统计失败：', e.message)
      }
    }
  }

  async function createProject(data) {
    const p = await projectApi.create(data)
    projects.value.unshift(p)
    return p
  }

  async function updateProject(id, data) {
    const p = await projectApi.update(id, data)
    const idx = projects.value.findIndex(x => x.id === id)
    if (idx >= 0) projects.value[idx] = p
    if (currentProject.value?.id === id) currentProject.value = p
    return p
  }

  async function deleteProject(id) {
    await projectApi.delete(id)
    projects.value = projects.value.filter(x => x.id !== id)
    if (currentProject.value?.id === id) {
      currentProject.value = projects.value[0] || null
    }
  }

  function selectProject(project) {
    currentProject.value = project
    // 切换作品时清空旧数据，避免闪现旧项目的残留
    // 加载新作品数据
    if (project?.id) refreshAll(project.id)
  }

  async function fetchProjectById(id) {
    if (!id) return null
    try {
      const p = await projectApi.getById(id)
      if (p) {
        currentProject.value = p
        const sid = String(id)
        const idx = projects.value.findIndex(x => String(x.id) === sid)
        if (idx >= 0) projects.value[idx] = p
      }
      return p
    } catch (e) { return null }
  }

  // ════════════════════════════════════
  // 模块数据刷新方法（核心 — AI 生成后调用）
  // ════════════════════════════════════

  async function refreshWorld(projectId) {
    if (!projectId) return []
    loadingModules.value.world = true
    try {
      const data = await worldApi.listSettings(projectId)
      worldSettings.value = data || []
      lastUpdated.value.world = new Date().toISOString()
      return worldSettings.value
    } catch (e) {
      console.warn('刷新世界观失败：', e.message)
      return worldSettings.value
    } finally {
      loadingModules.value.world = false
    }
  }

  async function refreshCharacters(projectId) {
    if (!projectId) return []
    loadingModules.value.characters = true
    try {
      const data = await characterApi.list(projectId)
      characters.value = data || []
      lastUpdated.value.characters = new Date().toISOString()
      return characters.value
    } catch (e) {
      console.warn('刷新人物失败：', e.message)
      return characters.value
    } finally {
      loadingModules.value.characters = false
    }
  }

  async function refreshOutlines(projectId) {
    if (!projectId) return []
    loadingModules.value.outline = true
    try {
      const data = await outlineApi.list(projectId)
      outlines.value = data || []
      lastUpdated.value.outline = new Date().toISOString()
      return outlines.value
    } catch (e) {
      console.warn('刷新大纲失败：', e.message)
      return outlines.value
    } finally {
      loadingModules.value.outline = false
    }
  }

  async function createOutline(projectId, data) {
    const node = await outlineApi.create(projectId, data)
    await refreshOutlines(projectId)
    return node
  }

  async function updateOutline(projectId, id, data) {
    const node = await outlineApi.update(projectId, id, data)
    await refreshOutlines(projectId)
    return node
  }

  async function deleteOutline(projectId, id) {
    await outlineApi.delete(projectId, id)
    await refreshOutlines(projectId)
  }

  /** 批量更新排序（同幕拖拽） */
  async function batchSortOutlines(projectId, items) {
    await outlineApi.batchSort(projectId, items)
    await refreshOutlines(projectId)
  }

  /** 批量更新排序和幕归属（跨幕拖拽） */
  async function batchSortActOutlines(projectId, items) {
    await outlineApi.batchSortAct(projectId, items)
    await refreshOutlines(projectId)
  }

  /** 批量保存幕与节点（AI 生成的大纲整体保存，支持任意数量的幕） */
  async function saveOutlineActs(projectId, acts) {
    const res = await outlineApi.saveActs(projectId, acts)
    await refreshOutlines(projectId)
    return res
  }

  /** 修复幕区分布（旧数据重建幕节点与归属） */
  async function fixOutlineDistribution(projectId) {
    const res = await outlineApi.fixActDistribution(projectId)
    await refreshOutlines(projectId)
    return res
  }

  /** 批量更新状态 */
  async function batchStatusOutlines(projectId, ids, status) {
    await outlineApi.batchStatus(projectId, ids, status)
    await refreshOutlines(projectId)
  }

  /** 批量删除 */
  async function batchDeleteOutlines(projectId, ids) {
    await outlineApi.batchDelete(projectId, ids)
    await refreshOutlines(projectId)
  }

  async function refreshPlot(projectId) {
    if (!projectId) return []
    loadingModules.value.plot = true
    try {
      const threads = await plotApi.listThreads(projectId).catch(e => { console.warn('刷新情节-线程失败：', e.message); return [] })
      plotThreads.value = threads || []
      lastUpdated.value.plot = new Date().toISOString()
      return plotThreads.value
    } catch (e) {
      console.warn('刷新情节失败：', e.message)
      return plotThreads.value
    } finally {
      loadingModules.value.plot = false
    }
  }

  async function refreshInspirations(projectId) {
    if (!projectId) return []
    loadingModules.value.inspirations = true
    try {
      const data = await inspirationApi.list(projectId)
      inspirations.value = data || []
      lastUpdated.value.inspirations = new Date().toISOString()
      return inspirations.value
    } catch (e) {
      console.warn('刷新灵感失败：', e.message)
      return inspirations.value
    } finally {
      loadingModules.value.inspirations = false
    }
  }

  async function refreshChapters(projectId) {
    if (!projectId) return []
    loadingModules.value.chapters = true
    try {
      const ch = await chapterApi.listChapters(projectId)
      chapters.value = (ch || []).map(c => ({ ...c, id: c.id != null ? String(c.id) : c.id }))
      lastUpdated.value.chapters = new Date().toISOString()

      // ✅ 同步项目统计到 projects 列表
      await syncProjectStats(projectId)

      return chapters.value
    } catch (e) {
      console.warn('刷新章节失败：', e.message)
      return chapters.value
    } finally {
      loadingModules.value.chapters = false
    }
  }

  async function refreshAll(projectId) {
    const pid = projectId || currentProject.value?.id
    if (!pid) return
    await Promise.all([
      fetchProjectById(pid),
      refreshWorld(pid),
      refreshCharacters(pid),
      refreshOutlines(pid),
      refreshPlot(pid),
      refreshInspirations(pid),
      refreshChapters(pid),
      loadDashboard(pid),
      loadAllContextData(pid)
    ])
  }

  // --- Chapters ---
  async function fetchChapters(projectId) {
    if (!projectId) return
    try {
      const ch = await chapterApi.listChapters(projectId)
      // 统一 ID 为字符串，防止 Snowflake Long ID 在 JS 中精度丢失
      chapters.value = (ch || []).map(c => ({ ...c, id: c.id != null ? String(c.id) : c.id }))

      // ✅ 同步项目统计到 projects 列表
      await syncProjectStats(projectId)
    } catch (e) {
      console.warn('章节列表加载失败：', e.message)
      chapters.value = []
    }
  }

  async function createChapter(projectId, data) {
    try {
      const autoTitle = data?.title && data.title !== '未命名章节'
        ? data.title
        : `第${chapters.value.length + 1}章`
      const c = await chapterApi.createChapter(projectId, { ...data, title: autoTitle })

      // 验证后端返回的数据
      if (!c || c.id == null) {
        throw new Error('后端返回的章节数据缺少 id 字段')
      }

      // 统一 ID 为字符串，防止 Snowflake Long ID 精度丢失
      const normalized = { ...c, id: String(c.id) }
      chapters.value.push(normalized)

      // ✅ 同步项目统计到 projects 列表
      await syncProjectStats(projectId)

      // ✅ 先同步统计数据到后端，再刷新仪表盘
      await projectApi.syncStats().catch(e => console.warn('统计同步失败（非致命）：', e.message))
      await loadDashboard(projectId)

      console.log('✅ 章节创建成功，ID：', normalized.id)
      return normalized

    } catch (e) {
      console.error('❌ 创建章节失败：', e.message)

      // 降级方案：创建临时章节（标记为 _local）
      const tempChapter = {
        id: 'temp_' + Date.now(),
        title: data?.title || '未命名章节',
        content: data?.content || '',
        status: 'draft',
        _local: true,
        wordCount: 0
      }
      chapters.value.push(tempChapter)
      return tempChapter
    }
  }

  // 获取单个章节完整内容（按需加载）
  async function fetchChapterContent(projectId, chapterId) {
    if (!projectId || !chapterId) return null
    try {
      const ch = await chapterApi.getChapter(projectId, chapterId)
      if (ch) {
        const idx = chapters.value.findIndex(x => x.id === chapterId)
        if (idx >= 0) chapters.value[idx] = { ...chapters.value[idx], ...ch }
      }
      return ch
    } catch (e) {
      console.warn('获取章节内容失败：', e.message)
      return chapters.value.find(c => c.id === chapterId) || null
    }
  }

  // 切换章节状态（草稿 ↔ 已发布）
  async function toggleChapterStatus(projectId, chapterId) {
    const ch = chapters.value.find(c => c.id === chapterId)
    if (!ch) return
    const newStatus = ch.status === 'published' ? 'draft' : 'published'
    const updated = await chapterApi.updateChapter(projectId, { id: chapterId, status: newStatus })
    const idx = chapters.value.findIndex(x => x.id === chapterId)
    if (idx >= 0) chapters.value[idx] = { ...chapters.value[idx], ...updated }

    // ✅ 同步项目统计到 projects 列表
    await syncProjectStats(projectId)

    return updated
  }

  // 复制章节
  async function duplicateChapter(projectId, chapterId) {
    const source = chapters.value.find(c => c.id === chapterId)
    if (!source) return null
    const newData = {
      title: `${source.title || '未命名'} (副本)`,
      content: source.content || '',
      status: 'draft'
    }
    return await createChapter(projectId, newData)
  }

  // 重排章节顺序
  async function reorderChapters(projectId, orderedIds) {
    const ordered = []
    for (const id of orderedIds) {
      const ch = chapters.value.find(c => c.id === id)
      if (ch) ordered.push(ch)
    }
    // 保留不在排序列表中的章节
    const remaining = chapters.value.filter(c => !orderedIds.includes(c.id))
    chapters.value = [...ordered, ...remaining]
    // 尝试同步到后端
    try {
      await chapterApi.reorderChapters(projectId, orderedIds)
    } catch (e) {
      console.warn('排序同步失败，仅本地生效：', e.message)
    }
  }

  async function updateChapter(projectId, data) {
    const c = await chapterApi.updateChapter(projectId, data)
    const normalized = { ...c, id: String(c.id) }
    const idx = chapters.value.findIndex(x => x.id === normalized.id)
    if (idx >= 0) chapters.value[idx] = normalized

    // ✅ 同步项目统计到 projects 列表
    await syncProjectStats(projectId)

    return normalized
  }

  async function deleteChapter(projectId, chapterId) {
    // 1. 查找章节
    const ch = chapters.value.find(c => c.id === chapterId)
    if (!ch) {
      console.warn('⚠️ 章节不存在，跳过删除')
      return
    }

    console.log('🔍 删除章节：', { id: chapterId, title: ch.title, isLocal: ch._local })

    // 2. 如果是本地临时章节（未保存到后端），只从本地移除
    if (ch._local === true || isLocalId(chapterId)) {
      console.log('📦 删除本地临时章节：', chapterId)
      chapters.value = chapters.value.filter(x => x.id !== chapterId)
      switchToNextChapter(chapterId)
      return
    }

    // 3. 正常调用后端 API 删除
    try {
      await chapterApi.deleteChapter(projectId, chapterId)
      console.log('✅ 后端删除成功，ID：', chapterId)
      chapters.value = chapters.value.filter(x => x.id !== chapterId)

      // ✅ 同步项目统计到 projects 列表
      await syncProjectStats(projectId)

      // ✅ 刷新仪表盘
      loadDashboard(projectId)

      switchToNextChapter(chapterId)

    } catch (e) {
      console.error('❌ 删除API失败：', e.message)

      // 4. 如果后端返回 404（章节不存在），可能是 ID 无效，同步清理本地
      if (e.code === 404) {
        console.warn('⚠️ 后端返回 404，章节可能已被删除或 ID 无效')
        chapters.value = chapters.value.filter(x => x.id !== chapterId)
        switchToNextChapter(chapterId)
      } else {
        // 5. 其他错误，提示用户
        alert('删除失败：' + (e.message || '未知错误'))
      }
    }
  }

  // 删除后切换到下一章节
  function switchToNextChapter(deletedChapterId) {
    if (currentChapterId.value !== deletedChapterId) return
    currentChapterId.value = chapters.value[0]?.id || null
    if (currentChapterId.value) {
      const ch = chapters.value.find(c => c.id === currentChapterId.value)
      editorContent.value = ch?.content || ''
      chapterTitle.value = ch?.title || ''
    } else {
      editorContent.value = ''
      chapterTitle.value = ''
    }
  }

  // 批量删除：删除标题匹配的所有章节
  async function deleteChaptersBatch(projectId, filterFn) {
    const targets = chapters.value.filter(filterFn)
    if (!targets.length) return 0
    let deleted = 0
    for (const ch of targets) {
      try {
        await deleteChapter(projectId, ch.id)
        deleted++
      } catch (e) {
        console.warn(`删除章节「${ch.title}」失败：`, e.message)
      }
    }
    return deleted
  }

  // ─── 编辑器操作 ───
  function selectChapter(chapter) {
    if (!chapter) {
      currentChapterId.value = null
      editorContent.value = ''
      chapterTitle.value = ''
      return
    }
    currentChapterId.value = chapter.id
    editorContent.value = chapter.content || ''
    chapterTitle.value = chapter.title || ''
  }

  async function selectChapterByIndex(index) {
    const target = chapters.value.find(c => c.sortOrder === index)
    if (target) {
      selectChapter(target)
      return true
    }
    return false
  }

  async function saveCurrentChapter(projectId, silent = false) {
    if (!currentChapterId.value || !projectId) return null

    const localIdx = chapters.value.findIndex(x => x.id === currentChapterId.value)
    if (localIdx === -1) { isSaving.value = false; return null }

    isSaving.value = true
    const ch = chapters.value[localIdx]

    // 如果是本地临时章节，需要先创建（后端分配真实 ID）
    if (ch._local === true || isLocalId(currentChapterId.value)) {
      try {
        const created = await chapterApi.createChapter(projectId, {
          title: chapterTitle.value || '未命名章节',
          content: editorContent.value,
          status: 'draft'
        })

        // 用后端返回的真实数据替换临时数据（统一 ID 为字符串）
        const normalized = { ...created, id: String(created.id) }
        chapters.value[localIdx] = normalized
        currentChapterId.value = normalized.id

        console.log('✅ 临时章节已同步到后端，真实 ID：', normalized.id)

        // ✅ 同步项目统计到 projects 列表
        await syncProjectStats(projectId)

        // ✅ 刷新仪表盘统计数据
        await loadDashboard(projectId)

        const now = new Date()
        lastSavedAt.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        isSaving.value = false
        return normalized

      } catch (e) {
        console.error('❌ 保存临时章节失败：', e.message)
        // 保持本地状态
        chapters.value[localIdx] = {
          ...ch,
          title: chapterTitle.value || '未命名章节',
          content: editorContent.value,
          wordCount: editorContent.value.length,
          updatedAt: new Date().toISOString()
        }

        // ✅ 即使保存失败，也同步项目统计（使用本地数据）
        syncProjectStats(projectId)

        const now = new Date()
        lastSavedAt.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        isSaving.value = false
        return null
      }
    }

    // 正常的更新逻辑（章节已有真实 ID）
    try {
      const data = {
        id: currentChapterId.value,
        title: chapterTitle.value || '未命名章节',
        content: editorContent.value,
        wordCount: editorContent.value.length
      }
      const c = await chapterApi.updateChapter(projectId, data, silent)
      // 统一 ID 为字符串
      const normalized = { ...c, id: String(c.id) }
      const idx = chapters.value.findIndex(x => x.id === normalized.id)
      if (idx >= 0) chapters.value[idx] = normalized

      // ✅ 同步项目统计到 projects 列表
      await syncProjectStats(projectId)

      // ✅ 刷新仪表盘统计数据（今日字数/本周累计/写作时长/热力图/活动）
      await loadDashboard(projectId)

      const now = new Date()
      lastSavedAt.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      isSaving.value = false
      return normalized

    } catch (e) {
      console.error('❌ 更新失败：', e.message)
      // 仅更新本地
      chapters.value[localIdx] = {
        ...ch,
        title: chapterTitle.value || '未命名章节',
        content: editorContent.value,
        wordCount: editorContent.value.length,
        updatedAt: new Date().toISOString()
      }

      // ✅ 即使后端保存失败，也同步项目统计（使用本地数据）
      syncProjectStats(projectId)

      const now = new Date()
      lastSavedAt.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      isSaving.value = false
      return null
    }
  }

  function updateEditorContent(text) {
    editorContent.value = text
  }

  function updateChapterTitle(title) {
    chapterTitle.value = title
  }

  function setTargetWords(count) {
    targetWords.value = count
  }

  /**
   * 添加本地通知（供前端业务模块使用，如写作目标达成、版本保存等）
   */
  function addLocalNotification({ type, title, description, severity = 'info' }) {
    const notif = {
      id: 'local_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
      type,
      title,
      description: description || '',
      severity,
      resolved: false,
      createTime: new Date().toISOString(),
      _local: true
    }
    notifications.value.unshift(notif)
    unreadCount.value++
  }

  // --- Characters ---
  async function fetchCharacters(projectId) {
    if (!projectId) return
    try {
      characters.value = await characterApi.list(projectId) || []
    } catch (e) {
      console.warn('人物列表加载失败，使用空列表：', e.message)
      characters.value = []
    }
  }

  async function createCharacter(projectId, data) {
    const c = await characterApi.create(projectId, data)
    characters.value.push(c)
    return c
  }

  async function updateCharacter(projectId, id, data) {
    const c = await characterApi.update(projectId, id, data)
    const idx = characters.value.findIndex(x => x.id === id)
    if (idx >= 0) characters.value[idx] = c
    return c
  }

  async function deleteCharacter(projectId, id) {
    await characterApi.delete(projectId, id)
    characters.value = characters.value.filter(x => x.id !== id)
  }

  // ─── AI 协同创作（流式输出，带章节衔接上下文） ───
  async function generateChapterWithContext({ projectId, chapterIndex, currentContent, direction, targetWords, lockChapterId, onToken }) {
    // 创建 AbortController
    abortController = new AbortController()
    const signal = abortController.signal

    return new Promise((resolve, reject) => {
      // 检查是否已取消
      if (signal.aborted) {
        reject(new Error('生成已取消'))
        return
      }

      aiApi.generateChapterStream(projectId, {
        chapterIndex,
        title: chapters.value.find((c, i) => i === chapterIndex - 1)?.title || '未命名章节',
        existingContent: currentContent || '',
        direction: direction || '延续故事主线，推动情节发展',
        style: '自然流畅',
        targetWords: targetWords || 2000
      },
      (token) => {
        // 检查是否被取消
        if (signal.aborted) {
          reject(new Error('生成已取消'))
          return
        }
        // 检查章节锁定：只有当前章节与目标章节一致时才写入
        if (lockChapterId && currentChapterId.value !== lockChapterId) {
          // 章节不匹配，暂停写入但不终止
          return
        }
        // 逐 token 回调，供外部实时更新编辑器
        if (onToken) onToken(token)
      },
      () => {
        // 流式完成
        abortController = null
        resolve()
      },
      (err) => {
        abortController = null
        console.error('AI 流式章节生成失败:', err)
        reject(err)
      })
    })
  }

  // 取消 AI 生成
  function cancelGeneration() {
    if (abortController) {
      abortController.abort()
      abortController = null
      console.log('✅ AI 生成已取消')
    }
  }

  // ─── 导出操作 ───
  async function fetchExportFormats() {
    try {
      exportFormats.value = await exportApi.formats() || []
    } catch (e) {
      console.warn('导出格式加载失败：', e.message)
    }
  }

  async function fetchExportHistory(projectId) {
    if (!projectId) return
    try {
      const history = await exportApi.getHistory(projectId) || []
      exportHistory.value = history.map(rec => ({
        ...rec,
        valid: rec.valid !== false,
        isBlob: rec.downloadUrl?.startsWith('blob:')
      }))
      await validateExportRecords()
    } catch (e) {
      console.warn('导出历史加载失败：', e.message)
      exportHistory.value = []
    }
  }

  async function validateExportRecords() {
    for (const rec of exportHistory.value) {
      if (rec.isBlob) {
        rec.valid = false
        rec.invalidReason = '文件已过期，请重新导出'
        continue
      }
      if (!rec.downloadUrl) {
        rec.valid = false
        rec.invalidReason = '无下载链接，请重新导出'
        continue
      }
      if (rec.downloadUrl.startsWith('blob:')) {
        try {
          const response = await fetch(rec.downloadUrl, { method: 'HEAD' })
          if (!response.ok) {
            rec.valid = false
            rec.invalidReason = '文件已失效'
          }
        } catch {
          rec.valid = false
          rec.invalidReason = '文件不可访问'
        }
      }
    }
  }

  async function doExport(projectId, params) {
    exporting.value = true
    exportProgress.value = 0
    exportStatusText.value = '正在准备导出...'
    try {
      const result = await exportApi.exportProject(projectId, params)
      exportProgress.value = 100
      exportStatusText.value = '导出完成'
      await fetchExportHistory(projectId)
      return result
    } catch (e) {
      exportStatusText.value = '导出失败：' + (e.message || '未知错误')
      throw e
    } finally {
      exporting.value = false
    }
  }

  function removeExportRecord(recordId) {
    const idx = exportHistory.value.findIndex(r => r.id === recordId)
    if (idx >= 0) {
      exportHistory.value.splice(idx, 1)
      saveExportHistoryToLocal()
    }
  }

  function clearInvalidExportRecords() {
    const before = exportHistory.value.length
    exportHistory.value = exportHistory.value.filter(r => r.valid !== false)
    const removed = before - exportHistory.value.length
    if (removed > 0) saveExportHistoryToLocal()
    return removed
  }

  function addLocalExportRecord(record) {
    const newRecord = {
      id: 'local_' + Date.now(),
      fileName: record.fileName || '未命名',
      format: record.format || 'txt',
      size: record.size || 0,
      timestamp: new Date().toISOString(),
      downloadUrl: record.downloadUrl || '',
      valid: true,
      isBlob: record.downloadUrl?.startsWith('blob:') || false,
      chapterRange: record.chapterRange || '全书',
      includeOutline: record.includeOutline || false,
      includeCharacters: record.includeCharacters || false,
      duration: record.duration || 0
    }
    exportHistory.value.unshift(newRecord)
    saveExportHistoryToLocal()
    return newRecord
  }

  function saveExportHistoryToLocal() {
    try {
      const data = exportHistory.value.map(r => ({
        ...r,
        downloadUrl: r.isBlob ? '' : r.downloadUrl,
        valid: r.isBlob ? false : r.valid
      }))
      localStorage.setItem('export_history_' + currentProjectId.value, JSON.stringify(data))
    } catch (e) {
      console.warn('保存导出历史到本地失败：', e.message)
    }
  }

  function loadExportHistoryFromLocal() {
    try {
      const saved = localStorage.getItem('export_history_' + currentProjectId.value)
      if (saved) {
        const data = JSON.parse(saved)
        exportHistory.value = data.map(r => ({ ...r, valid: false, invalidReason: '文件已过期，请重新导出' }))
      }
    } catch (e) {
      console.warn('加载本地导出历史失败：', e.message)
    }
  }

  // ─── 上下文引擎操作 ───
  async function fetchContextStats(projectId) {
    if (!projectId) return
    try {
      const data = await contextApi.getStats(projectId)
      if (data) contextStats.value = data
    } catch (e) {
      console.warn('上下文统计加载失败：', e.message)
    }
  }

  async function fetchContextConfig(projectId) {
    if (!projectId) return
    try {
      const data = await contextApi.getConfig(projectId)
      if (data) contextConfig.value = data
    } catch (e) {
      console.warn('上下文配置加载失败：', e.message)
    }
  }

  async function updateContextConfig(projectId, config) {
    try {
      const updated = await contextApi.updateConfig(projectId, config)
      if (updated) contextConfig.value = updated
      return updated
    } catch (e) {
      throw e
    }
  }

  async function doContextSearch(projectId, query, topK) {
    searching.value = true
    try {
      const results = await contextApi.search(projectId, query, topK || 10)
      searchResults.value = results || []
      return results
    } catch (e) {
      searchResults.value = []
      throw e
    } finally {
      searching.value = false
    }
  }

  async function rebuildContextIndex(projectId) {
    if (!projectId) return
    contextRebuilding.value = true
    contextRebuildProgress.value = 0
    contextOperationStatus.value = { type: 'rebuild', status: 'running', message: '正在启动重建任务...' }
    try {
      const result = await contextApi.rebuildIndex(projectId)
      contextRebuildTaskId.value = result?.taskId || result?.operationId
      if (contextRebuildTaskId.value) {
        startContextPoll(projectId)
      }
      return result
    } catch (e) {
      contextRebuilding.value = false
      contextOperationStatus.value = { type: 'rebuild', status: 'failed', message: e.message }
      throw e
    }
  }

  async function incrementalContextIndex(projectId) {
    if (!projectId) return
    contextLoading.value = true
    contextOperationStatus.value = { type: 'incremental', status: 'running', message: '正在执行增量索引...' }
    try {
      const result = await contextApi.incrementalIndex(projectId)
      contextOperationStatus.value = { type: 'incremental', status: 'success', message: '增量索引完成' }
      await fetchContextStats(projectId)
      return result
    } catch (e) {
      contextOperationStatus.value = { type: 'incremental', status: 'failed', message: e.message }
      throw e
    } finally {
      contextLoading.value = false
    }
  }

  async function cleanupContextIndex(projectId) {
    if (!projectId) return
    contextLoading.value = true
    contextOperationStatus.value = { type: 'cleanup', status: 'running', message: '正在清理无效索引...' }
    try {
      const result = await contextApi.cleanupIndex(projectId)
      contextOperationStatus.value = { type: 'cleanup', status: 'success', message: '清理完成' }
      await fetchContextStats(projectId)
      return result
    } catch (e) {
      contextOperationStatus.value = { type: 'cleanup', status: 'failed', message: e.message }
      throw e
    } finally {
      contextLoading.value = false
    }
  }

  async function fetchContextHealthCheck(projectId) {
    if (!projectId) return
    contextLoading.value = true
    try {
      const data = await contextApi.getHealthCheck(projectId)
      contextHealthCheck.value = data
      return data
    } catch (e) {
      console.warn('健康检查加载失败：', e.message)
      contextHealthCheck.value = null
      throw e
    } finally {
      contextLoading.value = false
    }
  }

  async function fetchContextSizeTrend(projectId, days = 7) {
    if (!projectId) return
    try {
      const data = await contextApi.getSizeTrend(projectId, days)
      contextSizeTrend.value = data || []
      return data
    } catch (e) {
      console.warn('索引趋势加载失败：', e.message)
      contextSizeTrend.value = []
      return []
    }
  }

  async function cancelContextOperation(projectId) {
    if (!projectId || !contextRebuildTaskId.value) return
    try {
      await contextApi.cancelIndexOperation(projectId, contextRebuildTaskId.value)
      stopContextPoll()
      contextRebuilding.value = false
      contextRebuildProgress.value = 0
      contextOperationStatus.value = { type: 'rebuild', status: 'cancelled', message: '操作已取消' }
    } catch (e) {
      console.warn('取消操作失败：', e.message)
      throw e
    }
  }

  function startContextPoll(projectId) {
    stopContextPoll()
    contextPollTimer = setInterval(async () => {
      if (!contextRebuildTaskId.value) {
        stopContextPoll()
        return
      }
      try {
        const progress = await contextApi.getOperationProgress(projectId, contextRebuildTaskId.value)
        if (progress) {
          contextRebuildProgress.value = progress.progress || 0
          contextOperationStatus.value = {
            type: 'rebuild',
            status: progress.status || 'running',
            message: progress.message || '索引重建中...',
            stage: progress.stage
          }
          if (progress.status === 'completed' || progress.status === 'failed' || progress.status === 'cancelled') {
            stopContextPoll()
            contextRebuilding.value = false
            if (progress.status === 'completed') {
              await fetchContextStats(projectId)
            }
          }
        }
      } catch (e) {
        console.warn('轮询进度失败：', e.message)
      }
    }, 2000)
  }

  function stopContextPoll() {
    if (contextPollTimer) {
      clearInterval(contextPollTimer)
      contextPollTimer = null
    }
  }

  async function loadAllContextData(projectId) {
    if (!projectId) return
    await Promise.all([
      fetchContextStats(projectId),
      fetchContextConfig(projectId),
      fetchContextSizeTrend(projectId, 7)
    ])
  }

  // ─── 通知操作 ───
  async function fetchNotifications(projectId, status = 'all') {
    if (!projectId) return
    notifLoading.value = true
    try {
      const data = await sentinelApi.listNotifications(projectId, status)
      notifications.value = data.list || []
      unreadCount.value = data.unreadCount || 0
    } catch (e) {
      console.warn('通知加载失败：', e.message)
    } finally {
      notifLoading.value = false
    }
  }

  async function markNotifRead(projectId, id) {
    try {
      await sentinelApi.markRead(projectId, id)
      const n = notifications.value.find(x => x.id === id)
      if (n) n.resolved = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) {
      console.warn('标记已读失败：', e.message)
    }
  }

  async function markAllNotifRead(projectId) {
    try {
      await sentinelApi.markAllRead(projectId)
      notifications.value.forEach(n => n.resolved = true)
      unreadCount.value = 0
    } catch (e) {
      console.warn('全部已读失败：', e.message)
    }
  }

  // ─── 哨兵操作 ───
  async function fetchSentinelStats(projectId) {
    if (!projectId) return
    try {
      const data = await sentinelApi.getStats(projectId)
      if (data) sentinelStats.value = data
    } catch (e) {
      console.warn('哨兵统计加载失败：', e.message)
    }
  }

  async function fetchSentinelAlerts(projectId, type, status) {
    if (!projectId) return
    try {
      const data = await sentinelApi.listAlerts(projectId, type || 'all', status || 'all')
      sentinelAlerts.value = data.list || []
    } catch (e) {
      console.warn('告警列表加载失败：', e.message)
      sentinelAlerts.value = []
    }
  }

  async function resolveSentinelAlert(projectId, id) {
    try {
      await sentinelApi.resolveAlert(projectId, id)
      const a = sentinelAlerts.value.find(x => x.id === id)
      if (a) a.resolved = true
      await fetchSentinelStats(projectId)
    } catch (e) {
      console.warn('处理告警失败：', e.message)
    }
  }

  async function ignoreSentinelAlert(projectId, id) {
    try {
      await sentinelApi.ignoreAlert(projectId, id)
      const a = sentinelAlerts.value.find(x => x.id === id)
      if (a) a.resolved = true
      await fetchSentinelStats(projectId)
    } catch (e) {
      console.warn('忽略告警失败：', e.message)
    }
  }

  async function fetchSentinelLogs(projectId) {
    if (!projectId) return
    try {
      const data = await sentinelApi.getLogs(projectId)
      sentinelLogs.value = data || []
    } catch (e) {
      console.warn('巡查日志加载失败：', e.message)
      sentinelLogs.value = []
    }
  }

  async function doSentinelScan(projectId) {
    scanning.value = true
    scanProgress.value = null
    try {
      const result = await sentinelApi.scan(projectId)
      scanningTaskId.value = result.taskId || null
      if (scanningTaskId.value) {
        pollScanProgress(projectId, scanningTaskId.value)
      }
      await fetchSentinelAlerts(projectId)
      await fetchSentinelStats(projectId)
      await fetchNotifications(projectId)
      await fetchSentinelLogs(projectId)
      return result
    } catch (e) {
      throw e
    } finally {
      scanning.value = false
    }
  }

  function pollScanProgress(projectId, taskId) {
    clearInterval(scanPollTimer)
    scanPollTimer = setInterval(async () => {
      if (!taskId) { clearInterval(scanPollTimer); return }
      try {
        const res = await sentinelApi.getScanProgress(projectId, taskId)
        if (res) scanProgress.value = res
        if (res?.status === 'completed' || res?.status === 'failed') {
          clearInterval(scanPollTimer)
          scanPollTimer = null
          scanning.value = false
          await fetchSentinelAlerts(projectId)
          await fetchSentinelStats(projectId)
          await fetchNotifications(projectId)
        }
      } catch (e) {
        console.warn('轮询扫描进度失败：', e.message)
        clearInterval(scanPollTimer)
        scanPollTimer = null
        scanning.value = false
      }
    }, 3000)
  }

  return {
    // 项目数据
    projects, currentProject, chapters, characters, loading, error,
    worldSettings, outlines, plotThreads, inspirations,
    loadingModules, lastUpdated,
    currentProjectId, totalWordCount, completedChapters,
    // 仪表盘状态
    dashboardStats, heatmapData, weeklyTrend, recentActivities,
    // 仪表盘计算属性
    todayWords, weekWords, writingDuration, totalDashboardChapterCount, targetWordCount, bestHours, avgSpeed, progress,
    // 仪表盘方法
    loadDashboard, fetchRecentActivities,
    // 编辑器状态
    currentChapterId, editorContent, chapterTitle, isSaving, lastSavedAt, targetWords,
    aiSuggestLoading, aiExpandLoading,
    // 编辑器计算属性
    currentChapter, currentWordCount, wordProgress, paragraphCount, readingTimeMinutes,
    saveStatusText, saveStatusColor,
    // 项目操作
    fetchProjects, createProject, updateProject, deleteProject, selectProject, fetchProjectById,
    syncProjectStats,
    // 章节操作
    fetchChapters, createChapter, updateChapter, deleteChapter, deleteChaptersBatch,
    fetchChapterContent, toggleChapterStatus, duplicateChapter, reorderChapters,
    // 编辑器操作
    selectChapter, selectChapterByIndex, saveCurrentChapter, updateEditorContent, updateChapterTitle, setTargetWords,
    addLocalNotification,
    // 人物操作
    fetchCharacters, createCharacter, updateCharacter, deleteCharacter,
    // AI 协同创作
    generateChapterWithContext, cancelGeneration,
    // 导出操作
    exportHistory, exportFormats, exporting, exportProgress, exportStatusText,
    fetchExportFormats, fetchExportHistory, doExport,
    removeExportRecord, clearInvalidExportRecords, addLocalExportRecord, validateExportRecords,
    // 上下文引擎操作
    contextStats, contextConfig, searching, searchResults,
    contextLoading, contextRebuilding, contextRebuildProgress, contextHealthCheck, contextSizeTrend, contextOperationStatus,
    fetchContextStats, fetchContextConfig, updateContextConfig, doContextSearch,
    rebuildContextIndex, incrementalContextIndex, cleanupContextIndex,
    fetchContextHealthCheck, fetchContextSizeTrend, cancelContextOperation,
    loadAllContextData,
    // 通知 & 哨兵
    notifications, unreadCount, notifLoading, sentinelStats, sentinelAlerts, sentinelFilter, scanning,
    scanningTaskId, scanProgress, sentinelLogs, alertDetail, sentinelCriticalCount,
    fetchNotifications, markNotifRead, markAllNotifRead,
    fetchSentinelStats, fetchSentinelAlerts, resolveSentinelAlert, ignoreSentinelAlert, doSentinelScan,
    fetchSentinelLogs,
    // 大纲操作
    createOutline, updateOutline, deleteOutline,
    batchSortOutlines, batchSortActOutlines, batchStatusOutlines, batchDeleteOutlines,
    saveOutlineActs, fixOutlineDistribution,
    // 模块刷新方法（核心）
    refreshWorld, refreshCharacters, refreshOutlines, refreshPlot, refreshInspirations,
    refreshChapters, refreshAll
  }
})

function isTokenExpired(token) {
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload))
    return decoded.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

// Auto-fetch projects on store creation (仅在有有效token时)
setTimeout(() => {
  const token = localStorage.getItem('token')
  if (!token || isTokenExpired(token)) return
  const store = useNovelStore()
  if (store.projects.length === 0) store.fetchProjects()
}, 0)