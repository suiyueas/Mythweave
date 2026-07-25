import { computed, watch } from 'vue'
import { useSettingsStore } from '@/stores/settings'

// 编辑器模式配置
const EDITOR_MODES = {
  dual: {
    key: 'dual',
    label: '双栏模式',
    desc: '左大纲右写作区 · 适合长篇创作',
    layout: 'grid',
    showOutline: true,
    showEditor: true
  },
  focus: {
    key: 'focus',
    label: '专注模式',
    desc: '全屏写作 · 无干扰 · 沉浸式体验',
    layout: 'center',
    showOutline: false,
    showEditor: true
  },
  outline: {
    key: 'outline',
    label: '大纲模式',
    desc: '仅大纲视图 · 适合结构规划',
    layout: 'block',
    showOutline: true,
    showEditor: false
  }
}

export function useEditorMode() {
  const settings = useSettingsStore()

  // 当前模式配置
  const currentMode = computed(() => {
    return EDITOR_MODES[settings.writingMode] || EDITOR_MODES.dual
  })

  // 是否显示大纲面板
  const showOutline = computed(() => currentMode.value.showOutline)

  // 是否显示编辑器
  const showEditor = computed(() => currentMode.value.showEditor)

  // 布局类名
  const layoutClass = computed(() => {
    return `writing-mode-${settings.writingMode}`
  })

  // 编辑器样式（字体、行高）
  const editorStyle = computed(() => ({
    fontSize: `${settings.fontSize}px`,
    lineHeight: settings.lineHeight,
    fontFamily: settings.bodyFont
  }))

  // 切换模式
  function setMode(modeKey) {
    if (EDITOR_MODES[modeKey]) {
      settings.writingMode = modeKey
    }
  }

  // 设置字体大小
  function setFontSize(size) {
    settings.fontSize = Math.min(24, Math.max(12, size))
  }

  // 设置行高
  function setLineHeight(height) {
    settings.lineHeight = Math.min(2.5, Math.max(1, height))
  }

  // 设置字体
  function setFontFamily(font) {
    settings.bodyFont = font
  }

  // 监听模式变化，应用到 DOM
  watch(() => settings.writingMode, (mode) => {
    const root = document.documentElement
    root.setAttribute('data-writing-mode', mode)
  }, { immediate: true })

  // 监听字体设置变化
  watch([
    () => settings.fontSize,
    () => settings.lineHeight,
    () => settings.bodyFont
  ], ([fontSize, lineHeight, bodyFont]) => {
    const root = document.documentElement
    root.style.setProperty('--editor-font-size', `${fontSize}px`)
    root.style.setProperty('--editor-line-height', lineHeight.toString())
    root.style.setProperty('--editor-font-family', bodyFont)
  }, { immediate: true })

  return {
    currentMode,
    showOutline,
    showEditor,
    layoutClass,
    editorStyle,
    setMode,
    setFontSize,
    setLineHeight,
    setFontFamily,
    EDITOR_MODES
  }
}
