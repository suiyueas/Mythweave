<template>
  <div v-if="show" class="search-results-dropdown">
    <div class="search-results-header">
      <span class="text-xs text-[#9c9690]">搜索结果</span>
      <span class="text-xs text-[#9c9690]">({{ results.length }})</span>
    </div>
    
    <div v-if="loading" class="search-results-loading">
      <div class="loading-spinner"></div>
      <span class="text-xs text-[#9c9690]">搜索中...</span>
    </div>
    
    <div v-else-if="results.length === 0 && keyword" class="search-results-empty">
      <span class="text-xs text-[#9c9690]">未找到匹配内容</span>
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
          <div class="result-title">{{ result.title }}</div>
          <div class="result-snippet" v-if="result.snippet">
            {{ result.snippet }}
          </div>
          <div class="result-meta">
            <span class="result-type-label">{{ result.typeLabel }}</span>
            <span class="result-time" v-if="result.updateTime">
              {{ formatTime(result.updateTime) }}
            </span>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="results.length > 0" class="search-results-footer">
      <button class="view-all-btn" @click="handleViewAll">
        查看全部 {{ results.length }} 条结果
      </button>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  results: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  keyword: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select', 'viewAll'])

const typeIcons = {
  chapter: '📑',
  character: '👤',
  world: '🌍',
  outline: '📋',
  plot: '🎯',
  inspiration: '💡',
  foreshadowing: '🔗'
}

function getTypeIcon(type) {
  return typeIcons[type] || '📄'
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  // 简单格式化时间
  return timeStr.replace('T', ' ').substring(0, 16)
}

function handleResultClick(result) {
  emit('select', result)
}

function handleViewAll() {
  emit('viewAll', props.keyword)
}
</script>

<style scoped>
.search-results-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  z-index: 50;
  max-height: 400px;
  overflow: hidden;
  margin-top: 4px;
}

.search-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border);
  background: rgba(250, 247, 242, 0.5);
}

.search-results-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border);
  border-top: 2px solid var(--accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.search-results-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.search-results-list {
  max-height: 300px;
  overflow-y: auto;
  padding: 4px 0;
}

.search-result-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.search-result-item:hover {
  background: rgba(217, 119, 6, 0.04);
}

.result-type-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
  margin-top: 2px;
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

.result-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
  line-height: 1.4;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-snippet {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-type-label {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.result-type-label.chapter {
  background: #dbeafe;
  color: #2563eb;
}

.result-type-label.character {
  background: #fef3c7;
  color: #d97706;
}

.result-type-label.world {
  background: #d1fae5;
  color: #059669;
}

.result-type-label.outline {
  background: #e0e7ff;
  color: #6366f1;
}

.result-type-label.plot {
  background: #fce7f3;
  color: #db2777;
}

.result-type-label.inspiration {
  background: #fef9c3;
  color: #ca8a04;
}

.result-type-label.foreshadowing {
  background: #f3e8ff;
  color: #7c3aed;
}

.result-time {
  font-size: 10px;
  color: var(--text-muted);
}

.search-results-footer {
  border-top: 1px solid var(--border);
  padding: 8px 12px;
  text-align: center;
}

.view-all-btn {
  font-size: 12px;
  color: var(--accent);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.15s ease;
}

.view-all-btn:hover {
  background: rgba(217, 119, 6, 0.1);
}

/* 滚动条样式 */
.search-results-list::-webkit-scrollbar {
  width: 4px;
}

.search-results-list::-webkit-scrollbar-track {
  background: transparent;
}

.search-results-list::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 2px;
}

.search-results-list::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}
</style>