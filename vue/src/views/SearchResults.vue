<template>
  <div class="search-results-page">
    <div class="search-header">
      <div class="search-info">
        <h1 class="search-title">搜索结果</h1>
        <p class="search-keyword" v-if="keyword">
          关键词：<span class="keyword-highlight">"{{ keyword }}"</span>
        </p>
        <p class="search-count" v-if="!loading">
          共找到 <span class="count-highlight">{{ results.length }}</span> 条结果
        </p>
      </div>
      <div class="search-actions">
        <button class="back-btn" @click="goBack">
          ← 返回
        </button>
      </div>
    </div>

    <div v-if="loading" class="search-loading">
      <div class="loading-spinner"></div>
      <span>正在搜索...</span>
    </div>

    <div v-else-if="results.length === 0" class="search-empty">
      <div class="empty-icon">🔍</div>
      <h3>未找到匹配内容</h3>
      <p>尝试使用不同的关键词，或者检查拼写是否正确</p>
    </div>

    <div v-else class="search-results-list">
      <div
        v-for="result in results"
        :key="`${result.type}-${result.id}`"
        class="search-result-item"
        @click="handleResultClick(result)"
      >
        <div class="result-type-icon" :class="result.type">
          {{ getTypeIcon(result.type) }}
        </div>
        <div class="result-content">
          <div class="result-header">
            <h3 class="result-title">{{ result.title }}</h3>
            <span class="result-type-badge" :class="result.type">
              {{ result.typeLabel }}
            </span>
          </div>
          <div class="result-snippet" v-if="result.snippet">
            {{ result.snippet }}
          </div>
          <div class="result-meta">
            <span class="result-match-field">
              匹配字段：{{ getMatchFieldLabel(result.matchField) }}
            </span>
            <span class="result-time" v-if="result.updateTime">
              更新时间：{{ formatTime(result.updateTime) }}
            </span>
          </div>
        </div>
        <div class="result-arrow">
          →
        </div>
      </div>
    </div>

    <div v-if="results.length > 0" class="search-pagination">
      <p class="pagination-info">
        显示前 {{ Math.min(results.length, 50) }} 条结果
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { searchApi } from '@/api/search'

const route = useRoute()
const router = useRouter()
const novelStore = useNovelStore()

const keyword = computed(() => route.query.keyword || '')
const results = ref([])
const loading = ref(false)

const typeIcons = {
  chapter: '📑',
  character: '👤',
  world: '🌍',
  outline: '📋',
  plot: '🎯',
  inspiration: '💡',
  foreshadowing: '🔗'
}

const matchFieldLabels = {
  title: '标题',
  name: '名称',
  content: '内容',
  description: '描述',
  personality: '性格'
}

function getTypeIcon(type) {
  return typeIcons[type] || '📄'
}

function getMatchFieldLabel(field) {
  return matchFieldLabels[field] || field
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

async function performSearch() {
  if (!keyword.value.trim()) {
    results.value = []
    return
  }

  const projectId = novelStore.currentProjectId
  if (!projectId) return

  loading.value = true

  try {
    const searchResults = await searchApi.globalSearch(projectId, keyword.value.trim())
    results.value = searchResults || []
  } catch (error) {
    console.error('搜索失败:', error)
    results.value = []
  } finally {
    loading.value = false
  }
}

function handleResultClick(result) {
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
}

function goBack() {
  router.back()
}

// 监听关键词变化
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword) {
    performSearch()
  }
}, { immediate: true })

// 监听项目ID变化
watch(() => novelStore.currentProjectId, (newProjectId) => {
  if (newProjectId && keyword.value) {
    performSearch()
  }
})

onMounted(() => {
  if (keyword.value) {
    performSearch()
  }
})
</script>

<style scoped>
.search-results-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.search-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px 0;
}

.search-keyword {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 4px 0;
}

.keyword-highlight {
  color: var(--accent);
  font-weight: 500;
}

.search-count {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.count-highlight {
  color: var(--accent);
  font-weight: 600;
}

.search-actions {
  flex-shrink: 0;
}

.back-btn {
  padding: 8px 16px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.back-btn:hover {
  background: var(--accent-glow);
  border-color: var(--accent);
  color: var(--accent);
}

.search-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  color: var(--text-muted);
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top: 3px solid var(--accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.search-empty {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.search-empty h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px 0;
}

.search-empty p {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.search-results-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-result-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.15s ease;
}

.search-result-item:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-sm);
}

.result-type-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.result-type-icon.chapter {
  background: #dbeafe;
  color: #2563eb;
}

.result-type-icon.character {
  background: #fef3c7;
  color: #d97706;
}

.result-type-icon.world {
  background: #d1fae5;
  color: #059669;
}

.result-type-icon.outline {
  background: #e0e7ff;
  color: #6366f1;
}

.result-type-icon.plot {
  background: #fce7f3;
  color: #db2777;
}

.result-type-icon.inspiration {
  background: #fef9c3;
  color: #ca8a04;
}

.result-type-icon.foreshadowing {
  background: #f3e8ff;
  color: #7c3aed;
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.result-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-type-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
  flex-shrink: 0;
}

.result-type-badge.chapter {
  background: #dbeafe;
  color: #2563eb;
}

.result-type-badge.character {
  background: #fef3c7;
  color: #d97706;
}

.result-type-badge.world {
  background: #d1fae5;
  color: #059669;
}

.result-type-badge.outline {
  background: #e0e7ff;
  color: #6366f1;
}

.result-type-badge.plot {
  background: #fce7f3;
  color: #db2777;
}

.result-type-badge.inspiration {
  background: #fef9c3;
  color: #ca8a04;
}

.result-type-badge.foreshadowing {
  background: #f3e8ff;
  color: #7c3aed;
}

.result-snippet {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--text-muted);
}

.result-match-field {
  padding: 2px 6px;
  background: rgba(217, 119, 6, 0.1);
  border-radius: 4px;
  color: var(--accent);
}

.result-arrow {
  font-size: 18px;
  color: var(--text-muted);
  flex-shrink: 0;
  align-self: center;
}

.search-result-item:hover .result-arrow {
  color: var(--accent);
}

.search-pagination {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
  text-align: center;
}

.pagination-info {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}
</style>