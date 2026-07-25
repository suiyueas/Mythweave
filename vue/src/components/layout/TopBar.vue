<template>
  <header class="top-bar">
    <div class="breadcrumb">
      创作中心 <span style="margin:0 4px;">›</span>
      <span>{{ currentTitle }}</span>
    </div>
    <div class="top-bar-spacer"></div>
    <!-- 内联搜索框 -->
    <div class="top-bar-search" ref="searchContainer">
      <span class="top-bar-search-icon">🔍</span>
      <input
        v-model="searchText"
        class="top-bar-search-input"
        placeholder="搜索章节、人物、设定..."
        @input="handleSearchInput"
        @focus="handleSearchFocus"
        @keyup.enter="onSearch"
      >
      <button v-if="searchText" @click="clearSearch" class="top-bar-search-clear">✕</button>
      
      <!-- 搜索结果下拉 -->
      <SearchResultsDropdown
        :show="showSearchResults"
        :results="searchResults"
        :loading="searchLoading"
        :keyword="searchText"
        @select="handleSearchResultSelect"
        @viewAll="handleViewAllResults"
      />
    </div>

    <!-- 按钮2：通知 -->
    <div v-if="showBell" class="action-wrapper">
      <button class="top-bar-btn" title="通知" @click="togglePanel('notif')" :class="{ active: activePanel === 'notif' }">
        🔔
        <span v-if="unreadCount" class="notif-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </button>
      <transition name="fade">
        <div v-if="activePanel === 'notif'" class="action-dropdown" style="width:360px">
          <div class="dropdown-header">
            <span class="text-sm font-semibold text-[#6b6560]">通知中心</span>
            <div class="flex items-center gap-2">
              <select v-model="notifFilter" class="text-xs px-2 py-0.5 rounded border border-[#e8e3dc] bg-white text-[#6b6560] outline-none">
                <option value="all">全部</option>
                <option value="unread">未读</option>
                <option value="read">已读</option>
              </select>
              <button class="text-xs text-[#d97706] hover:underline" @click="markAllRead">全部已读</button>
            </div>
          </div>
          <div class="dropdown-scrollable" style="max-height:320px">
            <div v-if="filteredNotifs.length === 0" class="text-center py-6 text-xs text-[#9c9690]">暂无通知</div>
            <div v-for="n in filteredNotifs.slice(0, 5)" :key="n.id" class="notif-item" :class="{ unread: !n.resolved }" @click="handleNotifClick(n)">
              <span class="ni-dot" :class="n.severity || 'info'"></span>
              <div class="ni-content flex-1 min-w-0">
                <div class="ni-title">{{ n.title }}</div>
                <div class="ni-desc" v-if="n.description">{{ n.description }}</div>
                <div class="ni-meta">
                  <span class="ni-cat" :class="n.type">{{ typeLabel(n.type) }}</span>
                  <span>{{ fmtTime(n.createTime) }}</span>
                </div>
              </div>
              <button v-if="!n.resolved" @click.stop="markRead(n.id)" class="w-5 h-5 rounded-full bg-[#d97706] text-white text-[10px] flex items-center justify-center flex-shrink-0" title="标记已读">✓</button>
            </div>
            <div v-if="filteredNotifs.length > 5" class="text-center py-2 border-t border-[#f3efe8] mt-1">
              <button class="text-xs text-[#d97706] hover:underline" @click="goToSentinel">查看全部 {{ filteredNotifs.length }} 条告警 →</button>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- 按钮3：主题与外观（仅保留编辑器主题） -->
    <div class="action-wrapper">
      <button class="top-bar-btn" title="编辑器主题" @click="togglePanel('theme')" :class="{ active: activePanel === 'theme' }">
        {{ activeTheme.icon }}
      </button>
      <transition name="fade">
        <div v-if="activePanel === 'theme'" class="action-dropdown" style="width:280px">
          <div class="dropdown-header">
            <span class="text-sm font-semibold text-[#6b6560]">编辑器主题</span>
          </div>
          <div class="dropdown-section">
            <div class="theme-grid">
              <div v-for="t in themeOptions" :key="t.key" class="theme-card" :class="{ active: editorTheme === t.key }" @click="applyEditorTheme(t)">
                <div class="theme-preview" :style="{ background: t.bg, borderColor: t.accent }">
                  <div class="theme-preview-line" v-for="n in 3" :key="n" :style="{ background: t.lineColor }"></div>
                </div>
                <span class="text-[10px] text-center mt-1.5">{{ t.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- 按钮4：全屏（点击直接切换，无下拉面板） -->
    <div class="action-wrapper">
      <button class="top-bar-btn" :title="appStore.isFullscreen ? '退出全屏' : '全屏写作'" @click="appStore.toggleFullscreen()">
        {{ appStore.isFullscreen ? '⛶' : '🗖' }}
      </button>
    </div>

  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSettingsStore } from '@/stores/settings'
import { useAppStore } from '@/stores/app'
import { useNovelStore } from '@/stores/novel'
import { searchApi } from '@/api/search'
import SearchResultsDropdown from '@/components/common/SearchResultsDropdown.vue'
import { filterNotifications, playNotificationSound } from '@/services/notificationService'

const route = useRoute()
const router = useRouter()
const settingsStore = useSettingsStore()
const appStore = useAppStore()
const novelStore = useNovelStore()
const currentTitle = computed(() => route.meta?.title || '仪表盘')

// ═══ 面板切换 ═══
const activePanel = ref(null)
function togglePanel(name) { activePanel.value = activePanel.value === name ? null : name }

// 点击外部关闭
function handleClickOutside(e) {
  if (activePanel.value && !e.target.closest('.action-wrapper')) activePanel.value = null
}
onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))

// ═══ 内联搜索 ═══
const searchText = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const showSearchResults = ref(false)
const searchContainer = ref(null)
let searchTimeout = null

function handleSearchInput() {
  // 防抖处理
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  
  if (!searchText.value.trim()) {
    searchResults.value = []
    showSearchResults.value = false
    return
  }
  
  searchTimeout = setTimeout(async () => {
    await performSearch(searchText.value.trim())
  }, 300)
}

function handleSearchFocus() {
  if (searchText.value.trim() && searchResults.value.length > 0) {
    showSearchResults.value = true
  }
}

async function performSearch(keyword) {
  const projectId = novelStore.currentProjectId
  if (!projectId) return
  
  searchLoading.value = true
  showSearchResults.value = true
  
  try {
    const results = await searchApi.quickSearch(projectId, keyword)
    searchResults.value = results || []
  } catch (error) {
    console.error('搜索失败:', error)
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

function clearSearch() {
  searchText.value = ''
  searchResults.value = []
  showSearchResults.value = false
}

function onSearch() {
  if (!searchText.value.trim()) return
  // 跳转到搜索结果页面
  router.push({ 
    path: `/my-works/${novelStore.currentProjectId}/search`,
    query: { keyword: searchText.value.trim() }
  })
  showSearchResults.value = false
}

function handleSearchResultSelect(result) {
  // 根据结果类型跳转到相应页面
  const projectId = novelStore.currentProjectId
  let url = ''
  
  switch (result.type) {
    case 'chapter':
      url = `/my-works/${projectId}?tool=chapters`
      break
    case 'character':
      url = `/my-works/${projectId}?tool=characters`
      break
    case 'world':
      url = `/my-works/${projectId}?tool=worldBuilding`
      break
    case 'outline':
      url = `/my-works/${projectId}?tool=storyStructure`
      break
    case 'plot':
      url = `/my-works/${projectId}?tool=plotEngine`
      break
    case 'inspiration':
      url = `/my-works/${projectId}?tool=inspiration`
      break
    case 'foreshadowing':
      url = `/my-works/${projectId}?tool=plotEngine&tab=foreshadowing`
      break
    default:
      url = `/my-works/${projectId}`
  }
  
  router.push(url)
  showSearchResults.value = false
  searchText.value = ''
}

function handleViewAllResults(keyword) {
  // 跳转到搜索结果页面
  router.push({ 
    path: `/my-works/${novelStore.currentProjectId}/search`,
    query: { keyword }
  })
  showSearchResults.value = false
}

// 点击外部关闭搜索结果
function handleSearchClickOutside(e) {
  if (searchContainer.value && !searchContainer.value.contains(e.target)) {
    showSearchResults.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleSearchClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleSearchClickOutside)
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
})

// ═══ 2. 通知 ═══
const notifFilter = ref('all')

const allNotifications = computed(() => novelStore.notifications || [])

// 根据用户设置过滤通知
const notifications = computed(() => filterNotifications(allNotifications.value))

// 未读计数 — 只基于过滤后的可见通知
const unreadCount = computed(() => notifications.value.filter(n => !n.resolved).length)

// 通知铃铛显隐
const showBell = computed(() => {
  return settingsStore.notificationBell !== false && settingsStore.settings?.notificationBell !== false
})

const filteredNotifs = computed(() => {
  let list = notifications.value
  if (notifFilter.value === 'unread') list = list.filter(n => !n.resolved)
  else if (notifFilter.value === 'read') list = list.filter(n => n.resolved)
  return list.sort((a, b) => (a.resolved === b.resolved ? 0 : a.resolved ? 1 : -1))
})

// 监听新通知到来时播放声音
watch(() => allNotifications.value.length, (newLen, oldLen) => {
  if (newLen > oldLen && oldLen > 0) {
    playNotificationSound()
  }
})

function markRead(id) { novelStore.markNotifRead(novelStore.currentProjectId, id) }
function markAllRead() { novelStore.markAllNotifRead(novelStore.currentProjectId) }
function goToSentinel() {
  const projectId = novelStore.currentProjectId
  const url = '/my-works/' + projectId + '?tool=sentinel'
  router.push(url)
  activePanel.value = null
}
function handleNotifClick(n) {
  if (!n.resolved) markRead(n.id)
  const projectId = novelStore.currentProjectId
  const typeMap = {
    foreshadowing: '/my-works/' + projectId + '?tool=sentinel',
    logic: '/my-works/' + projectId + '?tool=sentinel',
    character: '/my-works/' + projectId + '?tool=sentinel',
    rhythm: '/my-works/' + projectId + '?tool=sentinel'
  }
  const url = typeMap[n.type] || '/my-works/' + projectId + '?tool=sentinel'
  if (url && router.currentRoute.value.path !== url) router.push(url)
  activePanel.value = null
}

// 加载通知 & 项目切换时刷新
watch(() => novelStore.currentProjectId, (pid) => {
  if (pid) novelStore.fetchNotifications(pid)
}, { immediate: true })

function typeLabel(type) {
  const map = {
    foreshadowing: '伏笔',
    logic: '逻辑',
    character: '人物',
    rhythm: '节奏',
    normal: '正常'
  }
  return map[type] || type || '未知'
}
function fmtTime(t) {
  if (!t) return ''
  // 后端返回 ISO 格式或 yyyy-MM-dd HH:mm:ss
  return t.replace('T', ' ').substring(0, 16)
}

// ═══ 3. 编辑器主题 ═══
const editorTheme = computed(() => settingsStore.editorTheme)

const themeOptions = [
  { key: 'warm-ivory', label: '暖调象牙', icon: '☀️', bg: '#faf7f2', accent: '#d97706', lineColor: '#e8e3dc' },
  { key: 'dark', label: '暗夜护眼', icon: '🌙', bg: '#1a1a2e', accent: '#6366f1', lineColor: '#3a3a52' },
]

const activeTheme = computed(() => themeOptions.find(t => t.key === editorTheme.value) || themeOptions[0])

function applyEditorTheme(t) {
  settingsStore.editorTheme = t.key
  // editorTheme 的 watcher 已自动调用 applyEditorSettings()，无需额外操作
}
</script>

<style scoped>
/* ─── Top Bar 基础 ─── */
.top-bar {
  height: var(--header-height);
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0 1.5rem;
  border-bottom: 1px solid var(--border);
  background: rgba(250,247,242,0.85);
  backdrop-filter: blur(10px);
  flex-shrink: 0; z-index: 15;
  min-width: 0;
}
.breadcrumb {
  font-size: 0.8rem; color: var(--text-muted);
  display: flex; align-items: center; gap: 0.4rem;
  min-width: 0; max-width: 320px;
}
.breadcrumb span {
  color: var(--text-secondary);
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
  min-width: 0;
}
.top-bar-spacer { flex: 1; min-width: 0.5rem; }

.top-bar-btn {
  width: 34px; height: 34px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); background: var(--card);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; font-size: 0.9rem; color: var(--text-secondary);
  transition: all 0.15s ease; position: relative;
}
.top-bar-btn:hover, .top-bar-btn.active { border-color: var(--border-hover); color: var(--text); }
.top-bar-btn.active { background: var(--accent-glow); }

/* ─── 通用下拉面板 ─── */
.action-wrapper { position: relative; flex-shrink: 0; }
.action-dropdown {
  position: absolute; top: 42px; right: 0;
  background: var(--card); border: 1px solid var(--border);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-lg);
  z-index: 40; overflow: hidden;
}
.dropdown-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0.6rem 0.8rem; border-bottom: 1px solid var(--border);
}
.dropdown-section { padding: 0.6rem 0.8rem; border-bottom: 1px solid #f3efe8; }
.dropdown-section:last-child { border-bottom: none; }
.dropdown-section-label { font-size: 0.65rem; font-weight: 700; text-transform: uppercase; color: var(--text-muted); letter-spacing: 0.05em; margin-bottom: 0.5rem; }
.dropdown-scrollable { max-height: 280px; overflow-y: auto; padding: 0.3rem 0.5rem; }

/* ─── 内联搜索框 ─── */
.top-bar-search {
  display: flex; align-items: center;
  max-width: 320px; width: 100%;
  position: relative;
  flex-shrink: 1;
}
.top-bar-search-icon {
  position: absolute; left: 10px; top: 50%; transform: translateY(-50%);
  font-size: 0.75rem; color: var(--text-muted); pointer-events: none;
}
.top-bar-search-input {
  width: 100%; padding: 0.3rem 2rem 0.3rem 2rem;
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  font-size: 0.7rem; background: #faf8f5;
  outline: none; font-family: var(--font-body);
  transition: border-color 0.15s;
}
.top-bar-search-input:focus { border-color: var(--accent); }
.top-bar-search-clear {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  width: 18px; height: 18px; border-radius: 50%;
  border: none; background: #f3efe8; cursor: pointer;
  font-size: 0.55rem; color: var(--text-muted);
  display: flex; align-items: center; justify-content: center;
}

/* ─── 通知面板 ─── */
.notif-badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 16px; height: 16px; border-radius: 8px;
  background: #be123c; color: #fff; font-size: 0.55rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center; padding: 0 4px;
  border: 1.5px solid #faf7f2;
}
.notif-item {
  display: flex; align-items: flex-start; gap: 0.5rem; padding: 0.5rem 0.6rem;
  cursor: pointer; transition: all 0.12s; border-radius: var(--radius-sm); margin-bottom: 1px;
}
.notif-item:hover { background: rgba(217,119,6,0.04); }
.notif-item.unread { background: #fffbeb; }
.ni-dot { width: 7px; height: 7px; border-radius: 50%; margin-top: 0.4rem; flex-shrink: 0; }
.ni-dot.critical { background: #be123c; }
.ni-dot.warning { background: #d97706; }
.ni-dot.info { background: #0d9488; }
.ni-content { min-width: 0; }
.ni-title { font-size: 0.78rem; font-weight: 600; line-height: 1.3; }
.ni-desc { font-size: 0.7rem; color: var(--text-muted); margin-top: 0.15rem; line-height: 1.35; }
.ni-meta { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.2rem; font-size: 0.6rem; color: var(--text-muted); }
.ni-cat { padding: 1px 5px; border-radius: 4px; font-size: 0.55rem; font-weight: 600; }
.ni-cat.foreshadowing { background: #f3e8ff; color: #7c3aed; }
.ni-cat.logic { background: #fee2e2; color: #dc2626; }
.ni-cat.character { background: #dbeafe; color: #2563eb; }
.ni-cat.rhythm { background: #fef3c7; color: #d97706; }
.ni-cat.normal { background: #dcfce7; color: #16a34a; }

/* ─── 主题面板 ─── */
.theme-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0.5rem; }
.theme-card {
  padding: 0.4rem; border-radius: var(--radius); cursor: pointer;
  border: 2px solid transparent; transition: all 0.15s; text-align: center;
}
.theme-card:hover { border-color: var(--border-hover); }
.theme-card.active { border-color: var(--accent); background: var(--accent-glow); }
.theme-preview {
  height: 32px; border-radius: 4px; border: 1px solid;
  display: flex; flex-direction: column; gap: 3px; padding: 6px 5px;
}
.theme-preview-line { height: 3px; border-radius: 1px; width: 100%; }
.theme-preview-line:nth-child(2) { width: 70%; }
.theme-preview-line:nth-child(3) { width: 50%; }

/* ─── 动画 ─── */
.fade-enter-active, .fade-leave-active { transition: all 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-6px); }
</style>