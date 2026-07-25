<template>
  <div class="app-shell" :class="{ 'is-fullscreen': appStore.isFullscreen }">
    <LoadingOverlay v-if="loading" />
    <Sidebar v-show="sidebarVisible" />
    <div class="main-area">
      <TopBar v-show="!hideTopBar" />
      <ContentArea />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useSettingsStore } from '@/stores/settings'
import { storeToRefs } from 'pinia'
import Sidebar from './Sidebar.vue'
import TopBar from './TopBar.vue'
import ContentArea from './ContentArea.vue'
import LoadingOverlay from '../common/LoadingOverlay.vue'

const route = useRoute()
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const { loading } = storeToRefs(appStore)

const hideSidebar = computed(() => route.meta.hideSidebar === true)
const hideTopBar = computed(() => route.meta.hideTopBar === true)

// 侧边栏显隐：路由隐藏 + 全屏模式 + 用户设置
const sidebarVisible = computed(() => {
  return !appStore.isFullscreen && !hideSidebar.value && settingsStore.showSidebar
})

onMounted(() => {
  setTimeout(() => {
    appStore.setLoading(false)
  }, 2000)
})
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  position: relative;
  z-index: 1;
}

.app-shell.is-fullscreen {
  /* 全屏时背景适配 */
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  position: relative;
  z-index: 20;
}

/* 全屏下内容区拓宽舒适度 */
.app-shell.is-fullscreen .main-area {
  max-width: 100%;
}

/* 全屏下内容区域增加宽度约束，提升可读性 */
:global(.app-shell.is-fullscreen .content-area) {
  padding: 2rem 3rem;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}
</style>
