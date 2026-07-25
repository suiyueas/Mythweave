import { watch } from 'vue'
import { useSettingsStore } from '@/stores/settings'

// 主题色配置
const THEME_COLORS = {
  amber: {
    '--accent': '#d97706',
    '--accent-light': '#fef3c7',
    '--accent-glow': 'rgba(217,119,6,0.15)',
    '--accent-hover': '#b45309'
  },
  teal: {
    '--accent': '#0d9488',
    '--accent-light': '#ccfbf1',
    '--accent-glow': 'rgba(13,148,136,0.15)',
    '--accent-hover': '#0f766e'
  },
  rose: {
    '--accent': '#e11d48',
    '--accent-light': '#ffe4e6',
    '--accent-glow': 'rgba(225,29,72,0.15)',
    '--accent-hover': '#be123c'
  },
  purple: {
    '--accent': '#7c3aed',
    '--accent-light': '#ede9fe',
    '--accent-glow': 'rgba(124,58,237,0.15)',
    '--accent-hover': '#6d28d9'
  },
  blue: {
    '--accent': '#2563eb',
    '--accent-light': '#dbeafe',
    '--accent-glow': 'rgba(37,99,235,0.15)',
    '--accent-hover': '#1d4ed8'
  },
  emerald: {
    '--accent': '#059669',
    '--accent-light': '#d1fae5',
    '--accent-glow': 'rgba(5,150,105,0.15)',
    '--accent-hover': '#047857'
  }
}

export function useTheme() {
  const settings = useSettingsStore()

  // 应用主题色
  function applyThemeColor(colorKey) {
    const theme = THEME_COLORS[colorKey]
    if (!theme) return

    const root = document.documentElement
    Object.entries(theme).forEach(([key, value]) => {
      root.style.setProperty(key, value)
    })
  }

  // 应用深色/浅色主题
  function applyDarkMode(isDark) {
    const root = document.documentElement
    root.classList.toggle('dark-theme', isDark)
    root.classList.toggle('light-theme', !isDark)
  }

  // 应用紧凑模式
  function applyCompactMode(enabled) {
    const root = document.documentElement
    root.classList.toggle('compact-mode', enabled)
  }

  // 应用侧边栏设置
  function applySidebarSettings(position, width, visible) {
    const root = document.documentElement
    root.setAttribute('data-sidebar-position', position === '左侧' ? 'left' : 'right')
    
    let widthValue = 'standard'
    if (width.includes('窄')) widthValue = 'narrow'
    else if (width.includes('宽')) widthValue = 'wide'
    root.setAttribute('data-sidebar-width', widthValue)
    
    root.setAttribute('data-sidebar-visible', visible ? 'true' : 'false')
  }

  // 应用动效设置
  function applyAnimationSettings(pageAnim, hoverEffect, loadingAnim, bgTexture) {
    const root = document.documentElement
    root.style.setProperty('--page-anim-enabled', pageAnim ? '1' : '0')
    
    if (!bgTexture) {
      document.body.style.setProperty('background-image', 'none')
    } else {
      document.body.style.removeProperty('background-image')
    }
    
    root.classList.toggle('no-hover-effect', !hoverEffect)
    root.classList.toggle('no-loading-anim', !loadingAnim)
  }

  // 监听设置变化并应用
  watch(() => settings.themeColor, (newColor) => {
    applyThemeColor(newColor)
  }, { immediate: true })

  watch(() => settings.isDarkTheme, (isDark) => {
    applyDarkMode(isDark)
  }, { immediate: true })

  watch(() => settings.compactMode, (enabled) => {
    applyCompactMode(enabled)
  }, { immediate: true })

  watch([
    () => settings.sidebarPosition,
    () => settings.sidebarWidth,
    () => settings.showSidebar
  ], ([position, width, visible]) => {
    applySidebarSettings(position, width, visible)
  }, { immediate: true })

  watch([
    () => settings.pageAnim,
    () => settings.hoverEffect,
    () => settings.loadingAnim,
    () => settings.bgTexture
  ], ([pageAnim, hoverEffect, loadingAnim, bgTexture]) => {
    applyAnimationSettings(pageAnim, hoverEffect, loadingAnim, bgTexture)
  }, { immediate: true })

  return {
    applyThemeColor,
    applyDarkMode,
    applyCompactMode,
    applySidebarSettings,
    applyAnimationSettings,
    THEME_COLORS
  }
}
