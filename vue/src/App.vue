<template>
  <router-view />
</template>

<script setup>
import { watch, onMounted } from 'vue'
import { useSettingsStore } from '@/stores/settings'

const settingsStore = useSettingsStore()

const themeColorMap = {
  amber: '#d97706',
  teal: '#0d9488',
  rose: '#be123c',
  purple: '#7c3aed',
  blue: '#2563eb',
  emerald: '#059669'
}

function applySettings() {
  const root = document.documentElement
  const s = settingsStore

  const isDark = s.editorTheme === 'dark'
  const themeVars = isDark
    ? { '--bg': '#1a1a2e', '--sidebar-bg': '#1e1e3a', '--card': '#252540', '--text': '#e8e3dc', '--text-secondary': '#b0aaa4', '--text-muted': '#8a857e', '--border': '#3a3a52', '--border-hover': '#525270' }
    : { '--bg': '#faf7f2', '--sidebar-bg': '#f3efe8', '--card': '#ffffff', '--text': '#1a1815', '--text-secondary': '#6b6560', '--text-muted': '#9c9690', '--border': '#e8e3dc', '--border-hover': '#d4cec6' }
  for (const [key, value] of Object.entries(themeVars)) {
    root.style.setProperty(key, value)
  }
  root.classList.toggle('theme-dark', isDark)

  root.style.setProperty('--accent', themeColorMap[s.themeColor] || '#d97706')
  root.style.setProperty('--editor-font-size', s.fontSize + 'px')
  root.style.setProperty('--editor-line-height', s.lineHeight.toString())
  root.style.setProperty('--editor-font-family', s.fontFamily)
  root.setAttribute('data-editor-theme', s.editorTheme)
  root.classList.toggle('compact-mode', s.compactMode)
  root.setAttribute('data-sidebar-position', s.sidebarPosition)
  root.setAttribute('data-sidebar-width', s.sidebarWidth)
  root.setAttribute('data-sidebar-visible', s.showSidebar ? 'true' : 'false')
  root.classList.toggle('no-page-transition', !s.pageTransition)
  root.classList.toggle('no-hover-effect', !s.hoverEffect)
  root.classList.toggle('no-loading-anim', !s.loadingAnimation)
  document.body.classList.toggle('no-bg-texture', !s.backgroundTexture)
}

onMounted(() => { applySettings() })

watch([
  () => settingsStore.themeColor,
  () => settingsStore.fontSize,
  () => settingsStore.lineHeight,
  () => settingsStore.fontFamily,
  () => settingsStore.editorTheme,
  () => settingsStore.compactMode,
  () => settingsStore.sidebarPosition,
  () => settingsStore.sidebarWidth,
  () => settingsStore.showSidebar,
  () => settingsStore.pageTransition,
  () => settingsStore.hoverEffect,
  () => settingsStore.loadingAnimation,
  () => settingsStore.backgroundTexture
], () => { applySettings() }, { immediate: true })
</script>
