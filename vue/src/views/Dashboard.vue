<template>
  <div class="dashboard">
    <!-- ==================== 头部 ==================== -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1 class="dashboard-title">📊 写作仪表盘</h1>
        <p class="dashboard-subtitle">
          {{ project?.title || '未命名作品' }}
          <span class="divider">·</span>
          <span class="subtitle-date">{{ currentDate }}</span>
          <BaseTag v-if="store.todayWords > 0" color="emerald" size="sm">
            ✍️ 今日 {{ store.todayWords.toLocaleString() }} 字
          </BaseTag>
          <BaseTag v-else color="amber" size="sm">⏳ 今日尚未写作</BaseTag>
        </p>
      </div>
      <div class="header-right">
        <BaseButton size="sm" class="btn-refresh" :loading="dashboardLoading" @click="refreshDashboard">
          <span class="refresh-icon">⟳</span> 刷新
        </BaseButton>
      </div>
    </div>

    <!-- ==================== AI 助手入口 ==================== -->
    <section class="ai-section">
      <div class="ai-card">
        <div class="ai-icon-wrapper">
          <span class="ai-icon">✨</span>
          <span class="ai-pulse"></span>
        </div>
        <div class="ai-content">
          <h3 class="ai-title">AI 写作助手</h3>
          <p class="ai-desc">
            <span class="status-dot"></span>
            就绪状态 · DeepSeek v4 Pro · 上下文已加载全书
            <strong>{{ store.totalWordCount.toLocaleString() }}</strong> 字
          </p>
        </div>
        <div class="ai-actions">
          <BaseButton size="sm" class="btn-continue" @click="$emit('navigate', 'aiWrite')">📝 继续写作</BaseButton>
          <BaseButton variant="outline" size="sm" class="btn-chat" @click="$emit('navigate', 'aiChat')">💬 与AI对话</BaseButton>
        </div>
      </div>
    </section>

    <!-- ==================== 统计卡片 ==================== -->
    <section class="stats-section">
      <div class="stats-grid">
        <StatCard
          label="今日字数"
          :value="store.todayWords.toLocaleString() || '0'"
          :change="store.todayWords > 0 ? '✍️ 继续加油' : '今日尚未写作'"
          icon="✍️"
          color="accent"
          :progress="Math.min(100, (store.todayWords / 5000) * 100)"
        />
        <StatCard
          label="本周累计"
          :value="store.weekWords.toLocaleString() || '0'"
          :change="store.targetWordCount > 0 ? '目标 ' + store.targetWordCount.toLocaleString() + ' 字' : '未设目标'"
          icon="📈"
          color="teal"
          :progress="Math.min(100, (store.weekWords / (store.targetWordCount || 5000)) * 100)"
        />
        <StatCard
          label="总字数"
          :value="formatWordCount(store.totalWordCount)"
          :change="store.totalDashboardChapterCount + ' 章' + (store.targetWordCount > 0 ? ' · 目标 ' + store.targetWordCount.toLocaleString() + ' 字' : '')"
          icon="📚"
          color="emerald"
          :progress="store.progress || 0"
        />
        <StatCard
          label="写作时长"
          :value="(store.writingDuration / 60).toFixed(1) + 'h'"
          :change="store.writingDuration > 0 ? '⏰ 最佳时段 ' + store.bestHours : '今天还没有写作记录'"
          icon="⏱️"
          color="purple"
          :progress="Math.min(100, (store.writingDuration / 360) * 100)"
        />
      </div>
    </section>

    <!-- ==================== 热力图 ==================== -->
    <section class="heatmap-section">
      <BaseCard class="heatmap-card">
        <div class="heatmap-header">
          <div class="heatmap-title-group">
            <span class="heatmap-icon">📅</span>
            <h3 class="heatmap-title">写作日历热力图</h3>
          </div>
          <span class="heatmap-meta">{{ currentYear }} 年 · {{ writingDays }} 天写作</span>
        </div>

        <!-- 调试信息（排查用，修复后可删除） -->
        <div style="font-size:11px;color:#94a3b8;padding:2px 0 4px;">
          📊 热力图：{{ heatmapRows.reduce((s,r)=>s+r.length,0) }} 格 | {{ writingDays }} 天有写作 | 趋势：{{ weeklyTrend.length }} 天
        </div>

        <!-- 热力图网格：7行（周一~周日）× N列（周） -->
        <div class="heatmap-wrapper">
          <div class="heatmap-grid" :style="{ gridTemplateColumns: '28px repeat(' + totalWeeks + ', 1fr)' }">
            <!-- 周一到周日标签 + 每行的格子 -->
            <template v-for="(row, ri) in heatmapRows" :key="'hrow' + ri">
              <span class="heatmap-weekday">{{ weekLabels[ri] }}</span>
              <div
                v-for="(cell, ci) in row"
                :key="'hc' + ri + '-' + ci"
                class="heatmap-cell"
                :class="getCellClass(cell)"
                :title="getCellTooltip(cell)"
              ></div>
            </template>
          </div>
        </div>

        <!-- 图例 -->
        <div class="heatmap-legend">
          <div class="legend-left">
            <span class="legend-label">少</span>
            <span class="legend-swatch" v-for="level in 5" :key="level" :class="'level-' + (level - 1)"></span>
            <span class="legend-label">多</span>
          </div>
          <div class="legend-right">
            <span>0</span>
            <span>1-999</span>
            <span>1k-2k</span>
            <span>2k-4k</span>
            <span>4k+</span>
          </div>
        </div>
      </BaseCard>
    </section>

    <!-- ==================== 底部双栏 ==================== -->
    <section class="bottom-section">
      <div class="bottom-grid">
        <!-- 写作趋势 -->
        <BaseCard class="trend-card">
          <div class="card-header">
            <h3 class="card-title">📊 本周写作趋势</h3>
            <span class="card-meta">日均 {{ weekAvgWords.toLocaleString() }} 字 | max={{ maxTrendWords }}</span>
          </div>
          <div class="trend-chart">
            <div
              v-for="(item, index) in weeklyTrend"
              :key="index"
              class="trend-bar-wrapper"
            >
              <div
                class="trend-bar"
                :class="{ 'trend-bar-today': index === weeklyTrend.length - 1 }"
                :style="{ height: Math.max(4, maxTrendWords > 0 ? (item.words / maxTrendWords * 100) : 0) + '%' }"
                :title="item.day + ': ' + item.words.toLocaleString() + ' 字'"
              >
                <span class="trend-value">{{ item.words.toLocaleString() }}</span>
              </div>
              <span class="trend-label">{{ item.day }}</span>
            </div>
          </div>
        </BaseCard>

        <!-- 最近活动 -->
        <BaseCard class="activity-card">
          <div class="card-header">
            <h3 class="card-title">📋 最近活动</h3>
            <button class="link-btn" @click="goToChapters">查看全部 →</button>
          </div>
          <div v-if="store.recentActivities.length" class="activity-list">
            <div
              v-for="act in store.recentActivities.slice(0, 5)"
              :key="act.id"
              class="activity-item"
              :class="{ 'activity-highlight': act.highlight }"
            >
              <div class="activity-dot" :class="'dot-' + (act.type || 'default')"></div>
              <div class="activity-body">
                <div class="activity-time">{{ formatActivityTime(act.time) }}</div>
                <div class="activity-text">{{ act.title }} <span class="activity-desc">· {{ act.desc }}</span></div>
              </div>
              <BaseTag :color="getActivityColor(act.type)" size="xs">
                {{ getActivityLabel(act.type) }}
              </BaseTag>
            </div>
          </div>
          <div v-else class="activity-empty">
            <span class="empty-icon">📭</span>
            <span>暂无写作活动</span>
          </div>
        </BaseCard>
      </div>
    </section>

    <!-- ==================== 项目设定 ==================== -->
    <section class="settings-section" v-if="project?.coreSetting || project?.worldSettings || project?.characters || project?.outlines">
      <BaseCard class="settings-card">
        <div class="card-header">
          <h3 class="card-title">📝 项目设定</h3>
          <button class="link-btn" @click="$emit('navigate', 'world')">编辑设定 →</button>
        </div>
        <div class="settings-grid">
          <div v-if="project?.coreSetting" class="setting-item">
            <div class="setting-label">核心设定</div>
            <div class="setting-content">{{ project.coreSetting }}</div>
          </div>
          <div v-if="project?.worldSettings" class="setting-item">
            <div class="setting-label">世界观设定</div>
            <div class="setting-content">{{ project.worldSettings }}</div>
          </div>
          <div v-if="project?.characters" class="setting-item">
            <div class="setting-label">人物设定</div>
            <div class="setting-content">{{ project.characters }}</div>
          </div>
          <div v-if="project?.outlines" class="setting-item">
            <div class="setting-label">大纲结构</div>
            <div class="setting-content">{{ project.outlines }}</div>
          </div>
        </div>
      </BaseCard>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRouter } from 'vue-router'
const emit = defineEmits(['navigate'])
import BaseCard from '@/components/common/BaseCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseTag from '@/components/common/BaseTag.vue'
import StatCard from '@/components/common/StatCard.vue'
import { useNovelStore } from '@/stores/novel'
import { projectApi } from '@/api/project'
import { formatWordCount, formatActivityTime } from '@/utils/format'

const store = useNovelStore()
const router = useRouter()
const project = computed(() => store.currentProject)

// ─── 刷新加载状态 ───
const dashboardLoading = ref(false)

// ─── 当前日期 ───
const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
})

// ─── 加载数据 ───
async function loadAllDashboardData() {
  const pid = store.currentProject?.id
  if (!pid) return
  dashboardLoading.value = true
  try {
    await projectApi.syncStats().catch(e => console.warn('统计同步失败（非致命）：', e.message))
    await Promise.all([
      store.refreshChapters(pid),
      store.loadDashboard(pid)
    ])
  } catch (e) {
    console.warn('仪表盘数据加载失败：', e.message)
  } finally {
    dashboardLoading.value = false
  }
}

// ─── 跳转到章节管理 ───
function goToChapters() {
  const pid = store.currentProject?.id
  if (pid) router.push(`/my-works/${pid}?tool=chapters`)
}

// ─── 日期格式化工具 ───
// 将后端 LocalDate（数组 [2026,7,23] 或字符串 "2026-07-23"）统一转为 "YYYY-MM-DD"
function normalizeDate(dateVal) {
  if (!dateVal) return null
  if (Array.isArray(dateVal) && dateVal.length >= 3) {
    const [year, month, day] = dateVal
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  if (typeof dateVal === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateVal)) return dateVal
    const parsed = new Date(dateVal)
    if (!isNaN(parsed.getTime())) {
      return `${parsed.getFullYear()}-${String(parsed.getMonth() + 1).padStart(2, '0')}-${String(parsed.getDate()).padStart(2, '0')}`
    }
    return null
  }
  return null
}

// 本地日期格式化（避免 toISOString 时区偏移）
function formatLocalDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  if (store.currentProject?.id) loadAllDashboardData()
})
onActivated(() => {
  if (store.currentProject?.id) loadAllDashboardData()
})
watch(() => store.currentProject?.id, (newPid) => {
  if (newPid) loadAllDashboardData()
}, { immediate: false })
watch(() => store.projects.length, (len) => {
  if (len > 0 && store.currentProject?.id) loadAllDashboardData()
})

async function refreshDashboard() {
  try { await projectApi.syncStats() } catch (e) { /* non-fatal */ }
  await loadAllDashboardData()
}

// ─── 活动相关 ───
const getActivityColor = (type) => {
  const map = { chapter: 'emerald', writing: 'amber', auto: 'accent', ai: 'teal', alert: 'rose', export: 'emerald' }
  return map[type] || 'amber'
}
const getActivityLabel = (type) => {
  const map = { chapter: '章节', writing: '写作', auto: '自动', ai: 'AI', alert: '提醒', export: '导出' }
  return map[type] || type
}

// ═══════════════════════════════════════════════════════════════
// 热力图 — 简化实现：7行（周一~周日）× N列（周）
// ═══════════════════════════════════════════════════════════════
const weekLabels = ['一', '二', '三', '四', '五', '六', '日']
const currentYear = computed(() => new Date().getFullYear())

// 将后端 heatmapData 转为 { date, words } 的扁平数组
const heatmapRaw = computed(() => {
  const raw = store.heatmapData || []
  return raw.map(d => {
    const dateStr = normalizeDate(d.date)
    return {
      date: dateStr,
      words: d.count || 0,
      isFuture: d.isFuture || false
    }
  }).filter(d => d.date)
})

// 生成7行×N列的网格（按周组织，周一为第0行）
const heatmapRows = computed(() => {
  const data = heatmapRaw.value
  if (!data.length) return [[], [], [], [], [], [], []]

  // 按日期排序
  const sorted = [...data].sort((a, b) => a.date.localeCompare(b.date))

  // 找到第一天是星期几（周一=0，周日=6）
  const firstDate = new Date(sorted[0].date)
  const firstDayOfWeek = firstDate.getDay() // 0=周日
  const offset = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1 // 转为 周一=0

  // 用 Map 做快速查找
  const wordMap = new Map()
  sorted.forEach(d => wordMap.set(d.date, d.words))

  // 从第一天所在周的周一开始，逐日填充
  const startDate = new Date(sorted[0].date)
  startDate.setDate(startDate.getDate() - offset)

  // 计算总天数（从起始周一开始到最后一天结束）
  const lastDate = new Date(sorted[sorted.length - 1].date)
  const totalDays = Math.ceil((lastDate - startDate) / 86400000) + 1
  const totalWeeks = Math.ceil(totalDays / 7)

  // 初始化7行
  const rows = [[], [], [], [], [], [], []]
  for (let w = 0; w < totalWeeks; w++) {
    for (let d = 0; d < 7; d++) {
      const cellDate = new Date(startDate)
      cellDate.setDate(startDate.getDate() + w * 7 + d)
      const dateStr = formatLocalDate(cellDate)
      const isFuture = cellDate > new Date()
      const words = wordMap.get(dateStr) || 0
      rows[d].push({
        date: dateStr,
        words,
        isFuture
      })
    }
  }
  return rows
})

// 7行×N列网格的列数（总周数）
const totalWeeks = computed(() => {
  if (!heatmapRows.value.length || !heatmapRows.value[0].length) return 0
  return heatmapRows.value[0].length
})

const writingDays = computed(() => heatmapRaw.value.filter(d => !d.isFuture && d.words > 0).length)

function getWordLevel(words) {
  if (words <= 0) return 0
  if (words < 1000) return 1
  if (words < 2000) return 2
  if (words < 4000) return 3
  return 4
}

function getCellClass(cell) {
  if (!cell) return 'cell-empty'
  if (cell.isFuture) return 'cell-future'
  return 'cell-l' + getWordLevel(cell.words)
}

function getCellTooltip(cell) {
  if (!cell) return ''
  if (cell.isFuture) return cell.date + ' · 尚未到来'
  return cell.date + ' · ' + cell.words.toLocaleString() + ' 字'
}

// ═══════════════════════════════════════════════════════════════
// 本周趋势 — 简化实现：动态最大值
// ═══════════════════════════════════════════════════════════════
const weeklyTrend = computed(() => {
  const raw = store.weeklyTrend || []
  // 构建日期→字数映射
  const wordMap = new Map()
  raw.forEach(d => {
    const dateStr = normalizeDate(d.date)
    if (dateStr) wordMap.set(dateStr, d.count || 0)
  })
  // 生成当前周（周一→周日）
  const today = new Date()
  const dayOfWeek = today.getDay()
  const monday = new Date(today)
  monday.setDate(today.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1))
  const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const result = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    result.push({
      day: dayNames[i],
      words: wordMap.get(formatLocalDate(d)) || 0
    })
  }
  return result
})

const maxTrendWords = computed(() => {
  const max = Math.max(...weeklyTrend.value.map(d => d.words || 0), 1)
  return max
})

const weekAvgWords = computed(() => {
  const trend = weeklyTrend.value
  if (!trend.length) return 0
  const total = trend.reduce((s, d) => s + (d.words || 0), 0)
  const activeDays = trend.filter(d => d.words > 0).length
  return Math.round(total / Math.max(1, activeDays))
})
</script>

<style scoped>
/* ============================================================
   仪表盘 — 紧凑、均匀、有呼吸感
   ============================================================ */

.dashboard {
  padding: 12px 12px 20px;
  width: 100%;
}

/* ─── 头部 ─── */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.dashboard-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: -0.2px;
  margin: 0;
}
.dashboard-subtitle {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}
.dashboard-subtitle .divider { color: #d4d0ca; }
.subtitle-date { color: #6b6560; font-weight: 500; }

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 2px;
}
.btn-refresh {
  background: #1a1a2e;
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.25s ease;
}
.btn-refresh:hover {
  background: #2a2a4e;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(26, 26, 46, 0.15);
}
.refresh-icon {
  display: inline-block;
  font-size: 16px;
  transition: transform 0.5s ease;
}
.btn-refresh:hover .refresh-icon { transform: rotate(180deg); }

/* ─── AI 卡片 ─── */
.ai-section { margin-bottom: 16px; }
.ai-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: linear-gradient(135deg, rgba(217,119,6,0.04) 0%, rgba(14,116,144,0.04) 100%);
  border-radius: 10px;
  border: 1px solid rgba(217,119,6,0.10);
  transition: all 0.3s ease;
}
.ai-card:hover {
  border-color: rgba(217,119,6,0.18);
  box-shadow: 0 2px 12px rgba(217,119,6,0.05);
}
.ai-icon-wrapper {
  position: relative;
  flex-shrink: 0;
  width: 40px; height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(217,119,6,0.10);
  border-radius: 50%;
}
.ai-icon { font-size: 18px; }
.ai-pulse {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 2px solid rgba(217,119,6,0.12);
  animation: pulse-ring 2s ease-in-out infinite;
}
@keyframes pulse-ring {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.12); opacity: 0.4; }
}
.ai-content { flex: 1; min-width: 0; }
.ai-title { font-size: 13px; font-weight: 700; color: #1a1a2e; margin: 0 0 2px 0; }
.ai-desc {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
  flex-wrap: wrap;
}
.status-dot {
  display: inline-block;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: dot-blink 1.8s ease-in-out infinite;
}
@keyframes dot-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
.ai-desc strong { color: #1a1a2e; font-weight: 700; }
.ai-actions { display: flex; gap: 8px; flex-shrink: 0; }
.btn-continue {
  background: #1a1a2e;
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.25s ease;
}
.btn-continue:hover {
  background: #2a2a4e;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(26,26,46,0.12);
}
.btn-chat {
  border: 1.5px solid #e2e0dc;
  background: rgba(255,255,255,0.6);
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.25s ease;
}
.btn-chat:hover {
  background: #ffffff;
  border-color: #c8c4be;
  transform: translateY(-1px);
}

/* ─── 统计卡片 ─── */
.stats-section { margin-bottom: 16px; }
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.stats-grid :deep(.stat-card) { padding: 12px 14px !important; border-radius: 10px !important; }
.stats-grid :deep(.stat-card .stat-value) { font-size: 20px !important; }
.stats-grid :deep(.stat-card .stat-label) { font-size: 11px !important; }

/* ─── 热力图 ─── */
.heatmap-section { margin-bottom: 16px; }
.heatmap-card { padding: 12px 14px 14px !important; border-radius: 10px; }
.heatmap-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.heatmap-title-group { display: flex; align-items: center; gap: 8px; }
.heatmap-icon { font-size: 18px; }
.heatmap-title { font-size: 13px; font-weight: 700; color: #1a1a2e; margin: 0; }
.heatmap-meta {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
  background: #f1f0ec;
  padding: 2px 10px;
  border-radius: 16px;
}

.heatmap-wrapper {
  overflow-x: auto;
  padding: 2px 0 6px;
}

/* 核心：7行（周一~周日）× N列（周），weekday标签 + 格子 */
.heatmap-grid {
  display: grid;
  gap: 2px;
  min-width: 280px;
}

.heatmap-weekday {
  font-size: 10px;
  font-weight: 500;
  color: #b8b4b0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 4px;
}

.heatmap-cell {
  aspect-ratio: 1;
  border-radius: 3px;
  transition: all 0.2s ease;
  background: transparent;
}
.heatmap-cell:not(.cell-empty):not(.cell-future) {
  cursor: pointer;
}

/* 等级颜色 */
.heatmap-cell.cell-l0 { background: #ede8e0; }
.heatmap-cell.cell-l1 { background: rgba(251,191,36,0.25); }
.heatmap-cell.cell-l2 { background: rgba(251,191,36,0.50); }
.heatmap-cell.cell-l3 { background: rgba(217,119,6,0.70); }
.heatmap-cell.cell-l4 { background: #d97706; box-shadow: 0 0 6px rgba(217,119,6,0.20); }
.heatmap-cell.cell-future { background: #f5f3ef; }

.heatmap-cell:not(.cell-empty):not(.cell-future):hover {
  transform: scale(1.30);
  z-index: 5;
  outline: 2px solid #1a1a2e;
  outline-offset: -1px;
}

/* ─── 图例 ─── */
.heatmap-legend {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f1efe8;
}
.legend-left { display: flex; align-items: center; gap: 3px; }
.legend-label { font-size: 11px; color: #94a3b8; font-weight: 500; }
.legend-swatch { width: 14px; height: 14px; border-radius: 3px; flex-shrink: 0; }
.legend-swatch.level-0 { background: #ede8e0; }
.legend-swatch.level-1 { background: rgba(251,191,36,0.25); }
.legend-swatch.level-2 { background: rgba(251,191,36,0.50); }
.legend-swatch.level-3 { background: rgba(217,119,6,0.70); }
.legend-swatch.level-4 { background: #d97706; }
.legend-right {
  display: flex;
  gap: 6px;
  font-size: 10px;
  color: #94a3b8;
  font-weight: 500;
}

/* ─── 底部双栏 ─── */
.bottom-section { margin-top: 2px; }
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* ─── 卡片通用 ─── */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-title { font-size: 13px; font-weight: 700; color: #1a1a2e; margin: 0; }
.card-meta { font-size: 11px; color: #94a3b8; font-weight: 500; }
.link-btn {
  background: none;
  border: none;
  font-size: 12px;
  color: #6366f1;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all 0.2s;
}
.link-btn:hover { background: #f1f0ec; }

/* ─── 趋势图 ─── */
.trend-card { padding: 12px 14px 12px !important; border-radius: 10px; }
.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 80px;
  padding-top: 4px;
}
.trend-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  height: 100%;
}
.trend-bar {
  width: 100%;
  max-width: 28px;
  border-radius: 3px 3px 0 0;
  background: #e2ddd6;
  transition: all 0.4s ease;
  position: relative;
  cursor: pointer;
  min-height: 4px;
}
.trend-bar:hover {
  background: #d97706;
  transform: scaleY(1.03);
}
.trend-bar-today {
  background: #d97706;
  box-shadow: 0 0 10px rgba(217,119,6,0.15);
}
.trend-bar-today:hover { background: #b45309; }
.trend-value {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 9px;
  font-weight: 600;
  color: #d97706;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.2s;
}
.trend-bar:hover .trend-value { opacity: 1; }
.trend-label { font-size: 10px; color: #94a3b8; font-weight: 500; }

/* ─── 活动列表 ─── */
.activity-card { padding: 12px 14px 12px !important; border-radius: 10px; }
.activity-list { display: flex; flex-direction: column; gap: 2px; }
.activity-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}
.activity-item:hover { background: #f5f3ef; }
.activity-highlight {
  background: rgba(217,119,6,0.04);
  border-left: 2.5px solid #d97706;
}
.activity-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.activity-dot.dot-chapter { background: #10b981; }
.activity-dot.dot-writing { background: #d97706; }
.activity-dot.dot-auto { background: #d97706; }
.activity-dot.dot-ai { background: #6366f1; }
.activity-dot.dot-alert { background: #ef4444; }
.activity-dot.dot-export { background: #10b981; }
.activity-dot.dot-default { background: #94a3b8; }
.activity-body { flex: 1; min-width: 0; }
.activity-time { font-size: 11px; color: #94a3b8; }
.activity-text { font-size: 13px; color: #1a1a2e; }
.activity-desc { font-size: 12px; color: #94a3b8; }
.activity-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
  gap: 6px;
  color: #94a3b8;
}
.empty-icon { font-size: 28px; opacity: 0.5; }

/* ─── 项目设定 ─── */
.settings-section { margin-top: 16px; }
.settings-card { padding: 16px 20px; }
.settings-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.settings-card .card-title { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0; }
.settings-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.setting-item { background: #faf8f5; border-radius: 8px; padding: 12px 14px; }
.setting-label { font-size: 11px; font-weight: 600; color: #7c3aed; margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.5px; }
.setting-content { font-size: 13px; color: #475569; line-height: 1.6; max-height: 80px; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; }

/* ─── 响应式 ─── */
@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}
@media (max-width: 992px) {
  .bottom-grid { grid-template-columns: 1fr; gap: 12px; }
}
@media (max-width: 768px) {
  .dashboard { padding: 10px 8px 16px; }
  .dashboard-header { flex-direction: column; align-items: stretch; gap: 10px; }
  .header-right { justify-content: flex-start; }
  .ai-card { flex-wrap: wrap; padding: 10px 12px; gap: 8px; }
  .ai-actions { width: 100%; justify-content: flex-start; }
  .stats-grid { grid-template-columns: 1fr 1fr; gap: 8px; }
  .stats-grid :deep(.stat-card) { padding: 10px 12px !important; }
  .stats-grid :deep(.stat-card .stat-value) { font-size: 18px !important; }
  .heatmap-card { padding: 10px 12px !important; }
  .heatmap-header { flex-direction: column; align-items: flex-start; gap: 6px; }
  .heatmap-grid { gap: 1.5px; }
  .trend-chart { height: 60px; gap: 3px; }
  .trend-value { display: none !important; }
  .trend-card, .activity-card { padding: 10px 12px 10px !important; }
}
@media (max-width: 480px) {
  .dashboard { padding: 8px 4px 12px; }
  .stats-grid { grid-template-columns: 1fr; gap: 6px; }
  .ai-card { flex-direction: column; align-items: stretch; text-align: center; padding: 10px; }
  .ai-actions { justify-content: center; }
  .heatmap-grid { gap: 1px; }
  .heatmap-cell { border-radius: 2px; }
  .legend-right { flex-wrap: wrap; gap: 3px; font-size: 9px; }
}
</style>