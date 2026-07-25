import { defineStore } from 'pinia'
import { ref, watch, reactive, computed } from 'vue'
import { settingsApi } from '@/api'

const STORAGE_KEY = 'novel-settings'
const SHORTCUTS_KEY = 'novel-shortcuts'

const defaultShortcuts = {
  save: 'Ctrl+S',
  undo: 'Ctrl+Z',
  redo: 'Ctrl+Shift+Z',
  findReplace: 'Ctrl+H',
  aiContinue: 'Ctrl+Space',
  aiPolish: 'Ctrl+Shift+P',
  aiExpand: 'Ctrl+Shift+E',
  aiSummarize: 'Ctrl+Shift+C',
  focusMode: 'F11',
  outlineMode: 'Ctrl+0',
  toggleSidebar: 'Ctrl+\\'
}

const defaultSettings = {
  editor: {
    theme: 'warm-ivory',
    fontSize: 16,
    lineHeight: 1.65,
    fontFamily: 'Crimson Pro',
    writingMode: 'dual',
    autoSave: true,
    saveInterval: 5,
    cloudSync: true,
    versionHistory: 20
  },
  appearance: {
    themeColor: 'amber',
    sidebarPosition: 'left',
    sidebarWidth: 'standard',
    showSidebar: true,
    compactMode: false,
    pageTransition: true,
    hoverEffect: true,
    loadingAnimation: true,
    backgroundTexture: true
  },
  notification: {
    sentinelAlert: true,
    agentComplete: true,
    writingGoal: true,
    versionSave: false,
    notificationBell: true,
    soundAlert: false,
    severityThreshold: 'warning',
    quietHours: {
      enabled: false,
      start: '22:00',
      end: '08:00'
    }
  }
}

export const useSettingsStore = defineStore('settings', () => {
  // 编辑器设置
  const editorTheme = ref('warm-ivory')
  const fontSize = ref(16)
  const lineHeight = ref(1.65)
  const fontFamily = ref('Crimson Pro')
  const writingMode = ref('dual')
  const autoSave = ref(true)
  const saveInterval = ref(5)
  const cloudSync = ref(true)
  const versionHistory = ref(20)
  
  // 外观设置
  const themeColor = ref('amber')
  const sidebarPosition = ref('left')
  const sidebarWidth = ref('standard')
  const showSidebar = ref(true)
  const compactMode = ref(false)
  const pageTransition = ref(true)
  const hoverEffect = ref(true)
  const loadingAnimation = ref(true)
  const backgroundTexture = ref(true)
  
  // 通知设置
  const sentinelAlert = ref(true)
  const agentComplete = ref(true)
  const writingGoal = ref(true)
  const versionSave = ref(false)
  const notificationBell = ref(true)
  const soundAlert = ref(false)
  const severityThreshold = ref('warning')
  const quietHoursEnabled = ref(false)
  const quietHoursStart = ref('22:00')
  const quietHoursEnd = ref('08:00')
  
  // 快捷键
  const shortcuts = ref({ ...defaultShortcuts })
  
  // 同步状态
  const synced = ref(false)
  const loading = ref(false)

  const lightTheme = {
    '--bg': '#faf7f2', '--sidebar-bg': '#f3efe8', '--card': '#ffffff',
    '--text': '#1a1815', '--text-secondary': '#6b6560', '--text-muted': '#9c9690',
    '--border': '#e8e3dc', '--border-hover': '#d4cec6'
  }
  const darkTheme = {
    '--bg': '#1a1a2e', '--sidebar-bg': '#1e1e3a', '--card': '#252540',
    '--text': '#e8e3dc', '--text-secondary': '#b0aaa4', '--text-muted': '#8a857e',
    '--border': '#3a3a52', '--border-hover': '#525270'
  }

  function applyTheme() {
    const root = document.documentElement
    root.style.setProperty('--accent', getThemeColorValue(themeColor.value))
  }

  /**
   * 全局暗色/亮色主题：将 CSS 变量应用到 :root
   * 依赖 lightTheme / darkTheme 两个变量映射表
   */
  function applyGlobalTheme() {
    const root = document.documentElement
    const isDark = editorTheme.value === 'dark'
    const vars = isDark ? darkTheme : lightTheme
    for (const [key, value] of Object.entries(vars)) {
      root.style.setProperty(key, value)
    }
    root.classList.toggle('theme-dark', isDark)
  }

  function getThemeColorValue(colorKey) {
    const colors = {
      amber: '#d97706',
      teal: '#0d9488',
      rose: '#be123c',
      purple: '#7c3aed',
      blue: '#2563eb',
      emerald: '#0d9488'
    }
    return colors[colorKey] || '#d97706'
  }

  function applyEditorSettings() {
    const root = document.documentElement
    root.style.setProperty('--editor-font-size', fontSize.value + 'px')
    root.style.setProperty('--editor-line-height', lineHeight.value.toString())
    root.style.setProperty('--editor-font-family', fontFamily.value)
    root.setAttribute('data-editor-theme', editorTheme.value)
  }

  function applyCompactMode() {
    const root = document.documentElement
    root.classList.toggle('compact-mode', compactMode.value)
  }

  function applySidebarSettings() {
    const root = document.documentElement
    root.setAttribute('data-sidebar-position', sidebarPosition.value)
    root.setAttribute('data-sidebar-width', sidebarWidth.value)
    root.setAttribute('data-sidebar-visible', showSidebar.value ? 'true' : 'false')
  }

  function applyAnimationSettings() {
    const root = document.documentElement
    root.classList.toggle('no-page-transition', !pageTransition.value)
    root.classList.toggle('no-hover-effect', !hoverEffect.value)
    root.classList.toggle('no-loading-anim', !loadingAnimation.value)
    document.body.classList.toggle('no-bg-texture', !backgroundTexture.value)
  }

  // 监听设置变化并应用
  watch(themeColor, () => applyTheme(), { immediate: true })
  watch([fontSize, lineHeight, fontFamily, editorTheme], () => applyEditorSettings(), { immediate: true })
  watch(editorTheme, () => applyGlobalTheme(), { immediate: true })
  watch(compactMode, () => applyCompactMode(), { immediate: true })
  watch([sidebarPosition, sidebarWidth, showSidebar], () => applySidebarSettings(), { immediate: true })
  watch([pageTransition, hoverEffect, loadingAnimation, backgroundTexture], () => applyAnimationSettings(), { immediate: true })

  function toPayload() {
    return {
      editor: {
        theme: editorTheme.value,
        fontSize: fontSize.value,
        lineHeight: lineHeight.value,
        fontFamily: fontFamily.value,
        writingMode: writingMode.value,
        autoSave: autoSave.value,
        saveInterval: saveInterval.value,
        cloudSync: cloudSync.value,
        versionHistory: versionHistory.value
      },
      appearance: {
        themeColor: themeColor.value,
        sidebarPosition: sidebarPosition.value,
        sidebarWidth: sidebarWidth.value,
        showSidebar: showSidebar.value,
        compactMode: compactMode.value,
        pageTransition: pageTransition.value,
        hoverEffect: hoverEffect.value,
        loadingAnimation: loadingAnimation.value,
        backgroundTexture: backgroundTexture.value
      },
      notification: {
        sentinelAlert: sentinelAlert.value,
        agentComplete: agentComplete.value,
        writingGoal: writingGoal.value,
        versionSave: versionSave.value,
        notificationBell: notificationBell.value,
        soundAlert: soundAlert.value,
        severityThreshold: severityThreshold.value,
        quietHours: {
          enabled: quietHoursEnabled.value,
          start: quietHoursStart.value,
          end: quietHoursEnd.value
        }
      }
    }
  }

  function applyPayload(data) {
    if (!data) return
    
    if (data.editor) {
      editorTheme.value = data.editor.theme || 'warm-ivory'
      fontSize.value = data.editor.fontSize || 16
      lineHeight.value = data.editor.lineHeight || 1.65
      fontFamily.value = data.editor.fontFamily || 'Crimson Pro'
      writingMode.value = data.editor.writingMode || 'dual'
      autoSave.value = data.editor.autoSave !== undefined ? data.editor.autoSave : true
      saveInterval.value = data.editor.saveInterval || 5
      cloudSync.value = data.editor.cloudSync !== undefined ? data.editor.cloudSync : true
      versionHistory.value = data.editor.versionHistory || 20
    }
    
    if (data.appearance) {
      themeColor.value = data.appearance.themeColor || 'amber'
      sidebarPosition.value = data.appearance.sidebarPosition || 'left'
      sidebarWidth.value = data.appearance.sidebarWidth || 'standard'
      showSidebar.value = data.appearance.showSidebar !== undefined ? data.appearance.showSidebar : true
      compactMode.value = data.appearance.compactMode || false
      pageTransition.value = data.appearance.pageTransition !== undefined ? data.appearance.pageTransition : true
      hoverEffect.value = data.appearance.hoverEffect !== undefined ? data.appearance.hoverEffect : true
      loadingAnimation.value = data.appearance.loadingAnimation !== undefined ? data.appearance.loadingAnimation : true
      backgroundTexture.value = data.appearance.backgroundTexture !== undefined ? data.appearance.backgroundTexture : true
    }
    
    if (data.notification) {
      sentinelAlert.value = data.notification.sentinelAlert !== undefined ? data.notification.sentinelAlert : true
      agentComplete.value = data.notification.agentComplete !== undefined ? data.notification.agentComplete : true
      writingGoal.value = data.notification.writingGoal !== undefined ? data.notification.writingGoal : true
      versionSave.value = data.notification.versionSave !== undefined ? data.notification.versionSave : false
      notificationBell.value = data.notification.notificationBell !== undefined ? data.notification.notificationBell : true
      soundAlert.value = data.notification.soundAlert !== undefined ? data.notification.soundAlert : false
      severityThreshold.value = data.notification.severityThreshold || 'warning'
      if (data.notification.quietHours) {
        quietHoursEnabled.value = data.notification.quietHours.enabled || false
        quietHoursStart.value = data.notification.quietHours.start || '22:00'
        quietHoursEnd.value = data.notification.quietHours.end || '08:00'
      }
    }
  }

  function saveToLocalStorage() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(toPayload()))
    localStorage.setItem(SHORTCUTS_KEY, JSON.stringify(shortcuts.value))
  }

  function loadFromLocalStorage() {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        applyPayload(JSON.parse(stored))
      }
      
      const storedShortcuts = localStorage.getItem(SHORTCUTS_KEY)
      if (storedShortcuts) {
        shortcuts.value = JSON.parse(storedShortcuts)
      }
    } catch (e) {
      console.warn('从本地存储加载设置失败', e)
    }
  }

  async function loadSettings() {
    loading.value = true
    try {
      // 首先从本地存储加载
      loadFromLocalStorage()
      
      // 然后尝试从后端加载
      const data = await settingsApi.get()
      if (data) {
        applyPayload(data)
        saveToLocalStorage()
      }
      synced.value = true
    } catch (e) {
      console.warn('从后端加载设置失败，使用本地存储', e)
      // 本地存储已有数据，继续使用
    } finally {
      loading.value = false
    }
  }

  async function saveSettings() {
    try {
      await settingsApi.update(toPayload())
      saveToLocalStorage()
      synced.value = true
    } catch (e) {
      console.warn('保存设置到后端失败，仅保存到本地', e)
      saveToLocalStorage()
    }
  }

  function resetAll() {
    const defaults = defaultSettings
    applyPayload(defaults)
    shortcuts.value = { ...defaultShortcuts }
    saveToLocalStorage()
    applyTheme()
  }

  function resetShortcuts() {
    shortcuts.value = { ...defaultShortcuts }
    localStorage.setItem(SHORTCUTS_KEY, JSON.stringify(shortcuts.value))
  }

  function updateShortcut(key, value) {
    shortcuts.value[key] = value
    saveToLocalStorage()
  }

  // Settings object for SettingsView.vue - flat structure with all settings
  // Uses a reactive Proxy to intercept property access and route to individual refs
  const settings = reactive(new Proxy({}, {
    get(target, prop) {
      switch(prop) {
        case 'editorTheme': return editorTheme.value
        case 'writingMode': return writingMode.value
        case 'fontSize': return fontSize.value
        case 'lineHeight': return lineHeight.value
        case 'fontFamily': return fontFamily.value
        case 'autoSave': return autoSave.value
        case 'saveInterval': return saveInterval.value
        case 'cloudSync': return cloudSync.value
        case 'versionHistory': return versionHistory.value
        case 'themeColor': return themeColor.value
        case 'sidebarPosition': return sidebarPosition.value
        case 'sidebarWidth': return sidebarWidth.value
        case 'showSidebar': return showSidebar.value
        case 'compactMode': return compactMode.value
        case 'pageTransition': return pageTransition.value
        case 'hoverEffect': return hoverEffect.value
        case 'loadingAnimation': return loadingAnimation.value
        case 'backgroundTexture': return backgroundTexture.value
        case 'sentinelAlert': return sentinelAlert.value
        case 'agentComplete': return agentComplete.value
        case 'writingGoal': return writingGoal.value
        case 'versionSave': return versionSave.value
        case 'notificationBell': return notificationBell.value
        case 'soundAlert': return soundAlert.value
        case 'severityThreshold': return severityThreshold.value
        case 'quietHoursEnabled': return quietHoursEnabled.value
        case 'quietHoursStart': return quietHoursStart.value
        case 'quietHoursEnd': return quietHoursEnd.value
        default: return undefined
      }
    },
    set(target, prop, value) {
      switch(prop) {
        case 'editorTheme': editorTheme.value = value; break
        case 'writingMode': writingMode.value = value; break
        case 'fontSize': fontSize.value = value; break
        case 'lineHeight': lineHeight.value = value; break
        case 'fontFamily': fontFamily.value = value; break
        case 'autoSave': autoSave.value = value; break
        case 'saveInterval': saveInterval.value = value; break
        case 'cloudSync': cloudSync.value = value; break
        case 'versionHistory': versionHistory.value = value; break
        case 'themeColor': themeColor.value = value; break
        case 'sidebarPosition': sidebarPosition.value = value; break
        case 'sidebarWidth': sidebarWidth.value = value; break
        case 'showSidebar': showSidebar.value = value; break
        case 'compactMode': compactMode.value = value; break
        case 'pageTransition': pageTransition.value = value; break
        case 'hoverEffect': hoverEffect.value = value; break
        case 'loadingAnimation': loadingAnimation.value = value; break
        case 'backgroundTexture': backgroundTexture.value = value; break
        case 'sentinelAlert': sentinelAlert.value = value; break
        case 'agentComplete': agentComplete.value = value; break
        case 'writingGoal': writingGoal.value = value; break
        case 'versionSave': versionSave.value = value; break
        case 'notificationBell': notificationBell.value = value; break
        case 'soundAlert': soundAlert.value = value; break
        case 'severityThreshold': severityThreshold.value = value; break
        case 'quietHoursEnabled': quietHoursEnabled.value = value; break
        case 'quietHoursStart': quietHoursStart.value = value; break
        case 'quietHoursEnd': quietHoursEnd.value = value; break
      }
      return true
    }
  }))

  // ═══ 设置变化自动保存到 localStorage ═══
  watch([
    editorTheme, fontSize, lineHeight, fontFamily, writingMode,
    autoSave, saveInterval, cloudSync, versionHistory,
    themeColor, sidebarPosition, sidebarWidth, showSidebar, compactMode,
    pageTransition, hoverEffect, loadingAnimation, backgroundTexture,
    sentinelAlert, agentComplete, writingGoal, versionSave,
    notificationBell, soundAlert, severityThreshold,
    quietHoursEnabled, quietHoursStart, quietHoursEnd
  ], () => {
    saveToLocalStorage()
  }, { deep: true })

  // 快捷键变化也自动保存
  watch(shortcuts, () => {
    localStorage.setItem(SHORTCUTS_KEY, JSON.stringify(shortcuts.value))
  }, { deep: true })

  // 初始化时加载本地存储
  loadFromLocalStorage()

  return {
    // 编辑器设置
    editorTheme, fontSize, lineHeight, fontFamily, writingMode,
    autoSave, saveInterval, cloudSync, versionHistory,
    
    // 外观设置
    themeColor, sidebarPosition, sidebarWidth, showSidebar, compactMode,
    pageTransition, hoverEffect, loadingAnimation, backgroundTexture,
    
    // 通知设置
    sentinelAlert, agentComplete, writingGoal, versionSave,
    notificationBell, soundAlert, severityThreshold,
    quietHoursEnabled, quietHoursStart, quietHoursEnd,
    
    // 快捷键
    shortcuts,
    
    // 状态
    synced, loading,
    
    // Settings object (flat structure for SettingsView.vue)
    settings,
    
    // 方法
    applyTheme, applyGlobalTheme, loadSettings, saveSettings, resetAll, resetShortcuts,
    updateShortcut, toPayload
  }
})