import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const currentModule = ref('dashboard')
  const loading = ref(true)
  const isFullscreen = ref(false)

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const setCurrentModule = (module) => {
    currentModule.value = module
  }

  const setLoading = (value) => {
    loading.value = value
  }

  // 全屏切换
  async function toggleFullscreen() {
    if (!document.fullscreenElement) {
      await document.documentElement.requestFullscreen()
      isFullscreen.value = true
    } else {
      await document.exitFullscreen()
      isFullscreen.value = false
    }
  }

  // 监听原生全屏事件同步状态
  if (typeof document !== 'undefined') {
    document.addEventListener('fullscreenchange', () => {
      isFullscreen.value = !!document.fullscreenElement
    })
  }

  return {
    sidebarCollapsed,
    currentModule,
    loading,
    isFullscreen,
    toggleSidebar,
    setCurrentModule,
    setLoading,
    toggleFullscreen
  }
})
