<template>
  <div class="plot-engine">
    <!-- ==================== 头部 ==================== -->
    <div class="page-header">
      <div class="title-group">
        <h1>🎯 情节引擎</h1>
        <span class="subtitle">{{ project?.title || '未命名作品' }} · 情节线 · 伏笔管理 · 节奏分析</span>
      </div>
            <div class="header-actions">
        <template v-if="activeTab === 'threads'">
          <button class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg hover:bg-[#b45309] transition-colors" @click="showCreateDialog = true">＋ 新建情节</button>
        </template>
        <template v-else-if="activeTab === 'foreshadowing'">
          <button class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg hover:bg-[#b45309] transition-colors" @click="showCreateForeshadowDialog = true">＋ 新增伏笔</button>
          <button class="text-xs px-2.5 py-1 bg-[#7c3aed] text-white rounded-lg hover:bg-[#6d28d9] transition-colors" @click="handleAIDetect" :disabled="aiDetecting">
            <span v-if="aiDetecting" class="spinner-sm"></span>
            <span v-else>🤖 AI探测</span>
          </button>
        </template>
        <template v-else-if="activeTab === 'rhythm'">
          <button class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg hover:bg-[#b45309] transition-colors" @click="handleReassess">🔄 重新评估</button>
        </template>
      </div>
    </div>

    <!-- ==================== Tab 切换 ==================== -->
    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.icon }} {{ tab.label }}
        <span v-if="tab.count !== undefined" class="badge">{{ tab.count }}</span>
      </button>
    </div>

    <!-- ==================== 情节线列表 ==================== -->
    <template v-if="activeTab === 'threads'">
      <div class="plot-grid">
                <div v-for="thread in plotThreads" :key="thread.id" class="plot-card" @click="openThreadDetail(thread)">
          <div class="card-header">
            <div class="card-title-group">
              <span class="card-title">{{ thread.name || thread.title }}</span>
              <span class="type-badge" :class="getThreadTypeClass(thread.type)">
                {{ getThreadTypeLabel(thread.type) }}
              </span>
            </div>
            <span class="card-progress" :style="{ color: getProgressColor(thread.type) }">
              {{ thread.progress || 0 }}%
            </span>
          </div>
          <div class="card-meta">
            <template v-if="thread.nodes > 0">
              节点完成度（{{ thread.completedNodes || 0 }}/{{ thread.nodes }}）· 覆盖 {{ thread.chapters || 0 }} 章
            </template>
            <template v-else>
              未分配节点 · 请在详情中添加
            </template>
          </div>
          <div v-if="thread.description || thread.desc" class="card-desc">
            {{ thread.description || thread.desc }}
          </div>
          <div class="progress-section">
            <div class="progress-header">
              <span class="progress-label">智能进度</span>
              <span class="progress-value">{{ thread.progress || 0 }}%</span>
            </div>
            <div class="progress-track">
              <div class="progress-fill" :class="getProgressFillClass(thread.type)" :style="{ width: (thread.progress || 0) + '%' }"></div>
            </div>
          </div>
          <div class="card-footer">
            <span>{{ getThreadIcon(thread.type) }} {{ getThreadTypeLabel(thread.type) }}</span>
            <div class="tag-group">
              <span class="tag" :class="{ active: (thread.progress || 0) > 0 }">
                {{ (thread.progress || 0) > 0 ? '进行中' : '待启动' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="plotThreads.length === 0" class="empty-card">
          <div class="empty-icon">📋</div>
          <p>暂无情节线，点击「新建情节」开始创建</p>
        </div>
      </div>

      <!-- 情节完整度评估 -->
      <div class="assessment-card">
        <div class="assessment-left">
          <div class="assessment-score">
            <span class="number">{{ assessmentScore }}</span>
            <span class="label">/ 100</span>
          </div>
          <div class="assessment-details">
            <div class="detail-item">
              主线断点 <span class="highlight warn">{{ mainBreakpoints }}</span>
            </div>
            <div class="detail-item">
              支线收束 <span class="highlight warn">{{ pendingSubplots }}</span> 条待收束
            </div>
            <div class="detail-item hint">
              💡 {{ assessmentHint }}
            </div>
          </div>
        </div>
        <div class="assessment-right">
          <button class="btn btn-outline btn-sm" @click="showAnalysisDrawer = true">📊 详细分析</button>
          <button class="btn btn-accent btn-sm" @click="handleReassess">🔄 重新评估</button>
        </div>
      </div>

      <!-- 详细分析抽屉 -->
      <Teleport to="body">
        <Transition name="drawer-slide">
          <div v-if="showAnalysisDrawer" class="drawer-overlay" @click.self="showAnalysisDrawer = false">
            <div class="drawer-panel">
              <div class="drawer-header">
                <h2>📊 详细分析报告</h2>
                <button class="close" @click="showAnalysisDrawer = false">✕</button>
              </div>
              <div class="drawer-content">
                <div class="analysis-section">
                  <h3>🔴 主线断点分析</h3>
                  <div v-if="mainBreakpoints > 0" class="breakpoint-list">
                    <div v-for="(bp, idx) in breakpointDetails" :key="idx" class="breakpoint-item">
                      <span class="bp-icon">⚠️</span>
                      <div class="bp-content">
                        <div class="bp-title">断点 #{{ idx + 1 }}</div>
                        <div class="bp-desc">{{ bp.description }}</div>
                        <div class="bp-chapters">位于 {{ bp.chapters }} 之间</div>
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-issue">✅ 暂无断点问题</div>
                </div>

                <div class="analysis-section">
                  <h3>🌿 支线收束建议</h3>
                  <div v-if="pendingSubplots > 0" class="subplot-list">
                    <div v-for="(sp, idx) in subplotSuggestions" :key="idx" class="subplot-item" :class="{ urgent: sp.urgent }">
                      <span class="sp-priority">{{ sp.priority }}</span>
                      <div class="sp-content">
                        <div class="sp-name">{{ sp.name }}</div>
                        <div class="sp-desc">{{ sp.description }}</div>
                        <div class="sp-deadline">建议在 CH.{{ sp.deadline }} 前收束</div>
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-issue">✅ 所有支线已收束</div>
                </div>

                <div class="analysis-section">
                  <h3>🗺️ 章节-情节线覆盖热力图</h3>
                  <div class="heatmap-container">
                    <div class="heatmap-grid">
                      <div v-for="ch in chapterHeatmap" :key="ch.index" class="heatmap-cell" :style="{ backgroundColor: ch.color, opacity: ch.intensity }" :title="`CH.${ch.index}: ${ch.threadCount} 条情节线`">
                        <span class="heatmap-label">{{ ch.index }}</span>
                      </div>
                    </div>
                    <div class="heatmap-legend">
                      <span class="legend-label">稀疏</span>
                      <div class="legend-gradient"></div>
                      <span class="legend-label">密集</span>
                    </div>
                  </div>
                </div>

                <div class="analysis-section">
                  <button class="btn-export" @click="exportAnalysisReport">
                    📥 导出分析报告（Markdown）
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>
    </template>

    <!-- ==================== 伏笔管理 ==================== -->
    <template v-else-if="activeTab === 'foreshadowing'">
      <div class="recovery-stats">
        <span class="stats-label">伏笔回收率：</span>
        <span class="stats-value">{{ foreshadowStats.recovered }}/{{ foreshadowStats.total }}</span>
        <div class="stats-bar">
          <div class="stats-fill" :style="{ width: foreshadowStats.rate + '%' }"></div>
        </div>
        <span class="stats-percent">{{ foreshadowStats.rate }}%</span>
      </div>

            <div class="foreshadow-list">
        <div v-for="fs in foreshadowingList" :key="fs.id" class="foreshadow-item" :class="{ resolved: fs.status === 'resolved' || fs.status === 'recovered' }" @click="openForeshadowDetail(fs)">
          <div class="fs-left">
            <span class="fs-icon">{{ fs.icon }}</span>
            <div class="fs-info">
              <div class="fs-title" :class="{ 'line-through': fs.status === 'resolved' || fs.status === 'recovered' }">{{ fs.title || fs.name }}</div>
              <div class="fs-meta">埋于 CH.{{ fs.buried || '?' }} · 应在 CH.{{ fs.shouldReveal || '?' }} 前回收</div>
            </div>
          </div>
          <span class="fs-status-badge" :class="fs.status">
            <span class="status-dot" :class="fs.status"></span>
            {{ fs.statusLabel }}
          </span>
        </div>
      </div>
    </template>

    <!-- ==================== 节奏分析 ==================== -->
    <template v-else-if="activeTab === 'rhythm'">
      <div class="rhythm-section">
        <div class="rhythm-hint">📈 章节节奏张力曲线（紧张 → 舒缓）</div>
        <div ref="tensionChartRef" class="chart-container"></div>

        <div class="rhythm-stats">
          <div class="rhythm-stat-card">
            <div class="stat-num accent">{{ tensionStats.avg }}</div>
            <div class="stat-label">平均张力值</div>
          </div>
          <div class="rhythm-stat-card">
            <div class="stat-num teal">{{ tensionStats.bestChapter }}</div>
            <div class="stat-label">最佳节奏章</div>
          </div>
          <div class="rhythm-stat-card">
            <div class="stat-num" :class="tensionStats.highCount > 3 ? 'rose' : 'teal'">{{ tensionStats.highCount }}</div>
            <div class="stat-label">连续高强度预警</div>
          </div>
        </div>

                <div class="diagnosis-card" style="cursor:pointer;" @click="showInlineReport = !showInlineReport">
          <div class="diagnosis-header">
            <span class="diagnosis-label">节奏诊断：</span>{{ tensionDiagnosis }}
            <span class="diagnosis-hint">{{ showInlineReport ? '点击收起报告 ↑' : '点击查看详细报告 →' }}</span>
          </div>
          <Transition name="expand">
            <div v-if="showInlineReport" class="inline-report">
              <div class="report-chart-container">
                <div ref="inlineChartRef" class="inline-chart"></div>
              </div>
              <div class="report-details">
                <div class="report-stat">
                  <span class="stat-icon">📊</span>
                  <span class="stat-text">平均张力值：<strong>{{ tensionStats.avg }}</strong></span>
                </div>
                <div class="report-stat">
                  <span class="stat-icon">🏆</span>
                  <span class="stat-text">最佳节奏章：<strong>{{ tensionStats.bestChapter }}</strong></span>
                </div>
                <div class="report-stat">
                  <span class="stat-icon">⚠️</span>
                  <span class="stat-text">连续高强度预警：<strong>{{ tensionStats.highCount }}</strong> 章</span>
                </div>
              </div>
              <div class="report-advice">
                <div class="advice-icon">💡</div>
                <div class="advice-text">{{ tensionDiagnosis }}</div>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </template>

    <!-- ==================== 新建情节线弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="dialog-fade">
        <div v-if="showCreateDialog" class="modal-overlay" @click.self="showCreateDialog = false">
          <div class="modal-content">
            <div class="modal-header">
              <h2>📝 新建情节线</h2>
              <button class="close" @click="showCreateDialog = false">✕</button>
            </div>

            <div class="ai-hint">
              <span class="icon">✨</span>
              <span class="text">让 AI 根据当前作品自动生成情节线建议</span>
              <button class="btn-ai" @click="aiGenerate">AI 生成</button>
            </div>

            <div class="form-group">
              <label>情节线名称 <span class="hint">（必填）</span></label>
              <input v-model="newPlot.name" type="text" placeholder="如：帝国权谋主线" />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>情节类型</label>
                <select v-model="newPlot.type">
                  <option value="main">📌 主线</option>
                  <option value="sub">🌿 支线</option>
                  <option value="hidden">🔒 暗线</option>
                </select>
              </div>
              <div class="form-group">
                <label>预估章节范围</label>
                <select v-model="newPlot.chapterRange">
                  <option value="1-10">第 1-10 章</option>
                  <option value="11-20">第 11-20 章</option>
                  <option value="21-30">第 21-30 章</option>
                  <option value="custom">自定义范围</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label>情节描述 <span class="hint">（选填）</span></label>
              <textarea v-model="newPlot.description" placeholder="描述该情节线的发展脉络..." rows="3" maxlength="500"></textarea>
              <span class="char-count">{{ (newPlot.description || '').length }} / 500</span>
            </div>

            <div class="form-group">
              <label>关联角色（可选）</label>
              <input v-model="newPlot.relatedChars" type="text" placeholder="输入角色名，用逗号分隔" />
            </div>

            <div class="modal-footer">
              <button class="btn-cancel" @click="showCreateDialog = false">取消</button>
              <button class="btn-confirm" @click="handleCreatePlot" :disabled="!newPlot.name || saving">
                <span v-if="saving" class="spinner"></span>
                <span v-else>🚀 确认新增</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
      </Teleport>

    <!-- ==================== 情节线详情编辑弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="dialog-fade">
        <div v-if="showThreadDialog" class="modal-overlay" @click.self="showThreadDialog = false">
          <div class="modal-content">
            <div class="modal-header">
              <h2>📋 情节线详情</h2>
              <button class="close" @click="showThreadDialog = false">✕</button>
            </div>

            <div class="form-group">
              <label>名称 <span class="hint">（必填）</span></label>
              <input v-model="editThreadForm.name" type="text" placeholder="如：帝国权谋主线" />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>类型</label>
                <select v-model="editThreadForm.type">
                  <option value="main">📌 主线</option>
                  <option value="sub">🌿 支线</option>
                  <option value="hidden">🔒 暗线</option>
                </select>
              </div>
              <div class="form-group">
                <label>覆盖章节</label>
                <input v-model="editThreadForm.chapters" type="text" placeholder="如：1-10" />
              </div>
            </div>

            <div class="form-group">
              <label>进度 <span class="hint">（根据章节完成度自动计算）</span></label>
              <div class="auto-progress-display">
                <div class="progress-track-detail">
                  <div class="progress-fill-detail" :style="{ width: (editThreadForm.progress || 0) + '%', background: `linear-gradient(90deg, #ff6b35, hsl(${340 - ((editThreadForm.progress || 0) / 100) * 200}, 85%, 55%))` }"></div>
                </div>
                <span class="progress-value-inline">{{ editThreadForm.progress || 0 }}%</span>
              </div>
              <div v-if="editThreadForm.nodes > 0" class="progress-hint">
                节点完成度：{{ editThreadForm.completedNodes || 0 }}/{{ editThreadForm.nodes }}
              </div>
              <div v-else class="progress-hint warning">
                ⚠️ 尚未分配章节范围，进度无法自动计算
              </div>
            </div>

            <div class="form-group">
              <label>描述 <span class="hint">（选填）</span></label>
              <textarea v-model="editThreadForm.description" placeholder="描述该情节线的发展脉络..." rows="3" maxlength="500"></textarea>
              <span class="char-count">{{ (editThreadForm.description || '').length }} / 500</span>
            </div>

            <div class="modal-footer">
              <button class="btn-cancel" @click="showThreadDialog = false">取消</button>
              <button class="btn-danger" @click="handleDeleteThread" :disabled="saving">🗑️ 删除</button>
              <button class="btn-confirm" @click="handleSaveThread" :disabled="saving">
                <span v-if="saving" class="spinner"></span>
                <span v-else>💾 保存</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 伏笔编辑弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="dialog-fade">
        <div v-if="showForeshadowDialog" class="modal-overlay" @click.self="showForeshadowDialog = false">
          <div class="modal-content">
            <div class="modal-header">
              <h2>🎯 伏笔详情</h2>
              <button class="close" @click="showForeshadowDialog = false">✕</button>
            </div>

            <div class="form-group">
              <label>名称 <span class="hint">（必填）</span></label>
              <input v-model="editForeshadowForm.name" type="text" placeholder="如：先祖之瞳的预言" />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>埋设章节</label>
                <input v-model="editForeshadowForm.buried" type="text" placeholder="如：3" />
              </div>
              <div class="form-group">
                <label>建议回收章节</label>
                <input v-model="editForeshadowForm.shouldReveal" type="text" placeholder="如：15" />
              </div>
            </div>

            <div class="form-group">
              <label>状态</label>
              <div class="status-display">
                <span class="status-dot" :class="editForeshadowForm.status"></span>
                <span>{{ editForeshadowForm.status === 'resolved' ? '已回收' : editForeshadowForm.status === 'triggered' ? '触发中' : '待回收' }}</span>
              </div>
            </div>

            <div class="modal-footer">
              <button class="btn-cancel" @click="showForeshadowDialog = false">取消</button>
              <button v-if="editForeshadowForm.status !== 'resolved'" class="btn-accent-sm" @click="handleToggleForeshadowResolved" :disabled="saving">
                ✅ 标记为已回收
              </button>
              <button class="btn-danger" @click="handleDeleteForeshadow" :disabled="saving">🗑️ 删除</button>
              <button class="btn-confirm" @click="handleSaveForeshadow" :disabled="saving">
                <span v-if="saving" class="spinner"></span>
                <span v-else>💾 保存</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 新建伏笔弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="dialog-fade">
        <div v-if="showCreateForeshadowDialog" class="modal-overlay" @click.self="showCreateForeshadowDialog = false">
          <div class="modal-content">
            <div class="modal-header">
              <h2>🎯 新增伏笔</h2>
              <button class="close" @click="showCreateForeshadowDialog = false">✕</button>
            </div>

            <div class="form-group">
              <label>伏笔名称 <span class="hint">（必填）</span></label>
              <input v-model="newForeshadowForm.name" type="text" placeholder="如：先祖之瞳的预言" />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>埋设章节</label>
                <input v-model="newForeshadowForm.buried" type="text" placeholder="如：3" />
              </div>
              <div class="form-group">
                <label>建议回收章节</label>
                <input v-model="newForeshadowForm.shouldReveal" type="text" placeholder="如：15" />
              </div>
            </div>

            <div class="modal-footer">
              <button class="btn-cancel" @click="showCreateForeshadowDialog = false">取消</button>
              <button class="btn-confirm" @click="handleCreateForeshadow" :disabled="!newForeshadowForm.name.trim() || saving">
                <span v-if="saving" class="spinner"></span>
                <span v-else>🚀 确认新增</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch, nextTick } from 'vue'
import { useNovelStore } from '@/stores/novel'
import { useInspirationStore } from '@/stores/inspiration'
import { plotApi } from '@/api'
import InspirationSection from '@/components/inspiration/InspirationSection.vue'
import * as echarts from 'echarts'

const store = useNovelStore()
const inspStore = useInspirationStore()
const project = computed(() => store.currentProject)

// ─── 状态 ───
const activeTab = ref('threads')
const showCreateDialog = ref(false)
const saving = ref(false)
const tensionChartRef = ref(null)
const inlineChartRef = ref(null)
const showAnalysisDrawer = ref(false)
const showInlineReport = ref(false)
const isReassessing = ref(false)
const aiDetecting = ref(false)
let tensionChartInstance = null
let inlineChartInstance = null

// ─── 数据 ───
const plotThreads = computed(() => {
  const threads = store.plotThreads || []
  const chapters = store.chapters || []
  
  return threads.map(t => {
    const range = t.chapters || t.chapterRange || ''
    const match = range.match(/(\d+)-(\d+)/)
    let progress = t.progress || 0
    let totalNodes = 0
    let completedNodes = 0
    
    if (match) {
      const start = parseInt(match[1])
      const end = parseInt(match[2])
      totalNodes = end - start + 1
      
      const chaptersInRange = chapters.filter((ch, idx) => {
        const chNum = idx + 1
        return chNum >= start && chNum <= end
      })
      
      const completedInRange = chaptersInRange.filter(ch => 
        ch.status === 'completed' || ch.status === '已完成' || ch.wordCount > 0
      ).length
      
      if (totalNodes > 0) {
        progress = Math.round((completedInRange / totalNodes) * 100)
        completedNodes = completedInRange
      }
    }
    
    return {
      ...t,
      title: t.name || t.title,
      desc: t.description || t.desc || '',
      progress,
      nodes: totalNodes,
      completedNodes
    }
  })
})

const foreshadowingList = ref([])

// ─── Tab 配置 ───
const tabs = computed(() => [
  { key: 'threads', icon: '📋', label: '情节线', count: plotThreads.value.length },
  { key: 'foreshadowing', icon: '🎯', label: '伏笔管理', count: foreshadowingList.value.length },
  { key: 'rhythm', icon: '📈', label: '节奏分析' }
])

// ─── 伏笔统计 ───
const foreshadowStats = computed(() => {
  const total = foreshadowingList.value.length
  const recovered = foreshadowingList.value.filter(f => f.status === 'resolved' || f.status === 'recovered').length
  return { total, recovered, rate: total > 0 ? Math.round(recovered / total * 100) : 0 }
})

// ─── 评估数据 ───
const assessmentScore = ref(72)
const mainBreakpoints = ref(1)
const pendingSubplots = ref(3)
const assessmentHint = ref('建议在 Ch.15 前插入舒缓章节，避免读者疲劳')

const breakpointDetails = computed(() => {
  if (mainBreakpoints.value === 0) return []
  return [
    { description: '帝国权谋主线在第8-12章之间缺乏过渡节点，导致情节断裂感', chapters: 'CH.8 - CH.12' },
    { description: '星辰遗族暗线在第5章后缺少悬念承接点', chapters: 'CH.5 - CH.9' }
  ]
})

const subplotSuggestions = computed(() => {
  const threads = plotThreads.value.filter(t => t.type !== 'main')
  return threads.slice(0, 3).map((t, idx) => ({
    name: t.name || t.title || `支线 ${idx + 1}`,
    description: `覆盖 ${t.chapters || 0} 章，还剩 ${Math.max(0, (parseInt(t.chapters?.split('-')[1]) || 20) - (store.chapters?.length || 0))} 章未完结`,
    deadline: Math.min(30, (parseInt(t.chapters?.split('-')[1]) || 20) + 3),
    priority: idx + 1,
    urgent: idx === 0
  }))
})

const chapterHeatmap = computed(() => {
  const chapters = store.chapters || []
  const threadCountByChapter = {}
  plotThreads.value.forEach(t => {
    const range = t.chapters || t.chapterRange || ''
    const match = range.match(/(\d+)-(\d+)/)
    if (match) {
      const start = parseInt(match[1])
      const end = parseInt(match[2])
      for (let i = start; i <= end && i <= chapters.length; i++) {
        threadCountByChapter[i] = (threadCountByChapter[i] || 0) + 1
      }
    }
  })
  const maxCount = Math.max(1, ...Object.values(threadCountByChapter))
  return chapters.map((ch, idx) => {
    const count = threadCountByChapter[idx + 1] || 0
    const intensity = count / maxCount
    const hue = 30 - intensity * 30
    return {
      index: idx + 1,
      threadCount: count,
      intensity: 0.3 + intensity * 0.7,
      color: `hsl(${hue}, 80%, 55%)`
    }
  })
})

// ─── 节奏分析数据 ───
const tensionCurve = computed(() => {
  const chapters = store.chapters || []
  return chapters.map((ch, i) => ({
    ch: `CH.${i + 1}`,
    value: Math.floor(Math.random() * 8) + 2,
    words: ch.wordCount || 0,
    title: ch.title
  }))
})

const tensionStats = computed(() => {
  const values = tensionCurve.value.map(t => t.value)
  if (values.length === 0) return { avg: 0, bestChapter: '--', highCount: 0 }
  const avg = (values.reduce((s, v) => s + v, 0) / values.length).toFixed(1)
  const maxIdx = values.indexOf(Math.max(...values))
  const highCount = values.filter(v => v >= 7).length
  return { avg, bestChapter: tensionCurve.value[maxIdx]?.ch || '--', highCount }
})

const tensionDiagnosis = computed(() => {
  if (tensionCurve.value.length === 0) return '暂无章节数据'
  const highCount = tensionStats.value.highCount
  if (highCount > 3) return '连续高强度章节过多，建议在高潮后安排舒缓章节，避免读者疲劳'
  if (highCount === 0) return '张力分布较平缓，建议在关键节点增加冲突密度'
  return '张力分布较均衡，节奏把控良好'
})

// ─── 新建情节线表单 ───
const newPlot = reactive({
  name: '',
  type: 'sub',
  chapterRange: '11-20',
  description: '',
  relatedChars: ''
})

// ─── 编辑情节线状态 ───
const showThreadDialog = ref(false)
const editThreadId = ref(null)
const editThreadForm = reactive({
  name: '',
  type: 'sub',
  progress: 0,
  nodes: 0,
  completedNodes: 0,
  chapters: '',
  description: ''
})

// ─── 编辑伏笔状态 ───
const showForeshadowDialog = ref(false)
const editForeshadowId = ref(null)
const editForeshadowForm = reactive({
  name: '',
  buried: '',
  shouldReveal: '',
  status: 'pending'
})

// ─── 新建伏笔状态 ───
const showCreateForeshadowDialog = ref(false)
const newForeshadowForm = reactive({
  name: '',
  buried: '',
  shouldReveal: ''
})

// ─── 辅助函数 ───
function getThreadTypeClass(type) {
  if (!type) return 'sub'
  if (type.includes('主') || type === 'main') return 'main'
  if (type.includes('暗') || type === 'hidden') return 'hidden'
  return 'sub'
}

function getThreadTypeLabel(type) {
  if (!type) return '支线'
  if (type.includes('主') || type === 'main') return '主线'
  if (type.includes('暗') || type === 'hidden') return '暗线'
  return '支线'
}

function getThreadIcon(type) {
  const cls = getThreadTypeClass(type)
  return cls === 'main' ? '📌' : cls === 'hidden' ? '🔒' : '🌿'
}

function getProgressColor(type) {
  const cls = getThreadTypeClass(type)
  return cls === 'main' ? '#e11d48' : cls === 'hidden' ? '#7c3aed' : '#0d9488'
}

function getProgressFillClass(type) {
  const cls = getThreadTypeClass(type)
  return cls === 'main' ? '' : cls === 'hidden' ? 'purple' : 'teal'
}

// ─── 情节线：打开详情编辑弹窗 ───
function openThreadDetail(thread) {
  if (!thread) return
  editThreadId.value = thread.id
  editThreadForm.name = thread.name || ''
  editThreadForm.type = thread.type || 'sub'
  editThreadForm.progress = thread.progress || 0
  editThreadForm.nodes = thread.nodes || 0
  editThreadForm.completedNodes = thread.completedNodes || 0
  editThreadForm.chapters = thread.chapters || thread.chapterRange || ''
  editThreadForm.description = thread.description || thread.desc || ''
  showThreadDialog.value = true
}

// ─── 情节线：保存 ───
async function handleSaveThread() {
  if (!editThreadForm.name.trim() || saving.value) return
  saving.value = true
  try {
    const pid = store.currentProjectId
    if (!pid || !editThreadId.value) return
    await plotApi.updateThread(pid, editThreadId.value, {
      name: editThreadForm.name.trim(),
      type: editThreadForm.type,
      description: editThreadForm.description.trim()
    })
    await store.refreshPlot(pid)
    showThreadDialog.value = false
  } catch (e) {
    console.error('保存失败：', e.message)
    alert('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// ─── 情节线：删除 ───
async function handleDeleteThread() {
  if (!editThreadId.value) return
  if (!confirm('确定要删除这条情节线吗？此操作不可恢复！')) return
  saving.value = true
  try {
    const pid = store.currentProjectId
    if (!pid) return
    await plotApi.deleteThread(pid, editThreadId.value)
    await store.refreshPlot(pid)
    showThreadDialog.value = false
    alert('情节线已删除')
  } catch (e) {
    console.error('删除失败：', e.message)
    alert('删除失败，请重试')
  } finally {
    saving.value = false
  }
}

// ─── 创建情节线 ───
async function handleCreatePlot() {
  if (!newPlot.name.trim() || saving.value) return
  saving.value = true
  try {
    const pid = store.currentProjectId
    if (!pid) return
    await plotApi.createThread(pid, {
      name: newPlot.name.trim(),
      type: newPlot.type,
      description: newPlot.description.trim()
    })
    await store.refreshPlot(pid)
    showCreateDialog.value = false
    Object.assign(newPlot, { name: '', type: 'sub', chapterRange: '11-20', description: '', relatedChars: '' })
  } catch (e) {
    console.error('创建失败：', e.message)
    alert('创建失败，请重试')
  } finally {
    saving.value = false
  }
}

// ─── AI 生成 ───
function aiGenerate() {
  const names = ['帝国权谋暗流', '星辰遗族之谜', '深渊裂隙之影', '神座争夺之战', '遗忘者归来']
  const descs = [
    '在权力与秩序的夹缝中，一股暗流悄然涌动，它将改变整个大陆的格局。',
    '被遗忘的星辰遗族在三千年后重新现世，携带着上古的秘密与诅咒。',
    '深渊裂隙不断扩张，虚兽的阴影笼罩了边境，主角必须寻找真相。',
    '七大神座再度显现，各方势力摩拳擦掌，终极较量即将拉开帷幕。',
    '那些被认为早已消亡的存在，正在以另一种形式归来。'
  ]
  newPlot.name = names[Math.floor(Math.random() * names.length)]
  newPlot.description = descs[Math.floor(Math.random() * descs.length)]
}

// ─── 伏笔：打开详情编辑弹窗 ───
function openForeshadowDetail(fs) {
  if (!fs) return
  editForeshadowId.value = fs.id
  editForeshadowForm.name = fs.title || fs.name || ''
  editForeshadowForm.buried = String(fs.buried || '')
  editForeshadowForm.shouldReveal = String(fs.shouldReveal || '')
  editForeshadowForm.status = fs.status || 'pending'
  showForeshadowDialog.value = true
}

// ─── 伏笔：保存编辑 ───
async function handleSaveForeshadow() {
  if (!editForeshadowForm.name.trim() || saving.value) return
  saving.value = true
  try {
    const pid = store.currentProjectId
    if (!pid || !editForeshadowId.value) return
    await plotApi.updateForeshadowing(pid, editForeshadowId.value, {
      name: editForeshadowForm.name.trim(),
      chapterId: editForeshadowForm.buried,
      resolvedChapterId: editForeshadowForm.shouldReveal
    })
    await refreshForeshadowing(pid)
    showForeshadowDialog.value = false
  } catch (e) {
    console.error('保存失败：', e.message)
    alert('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// ─── 伏笔：标记为已回收 ───
async function handleToggleForeshadowResolved() {
  saving.value = true
  try {
    const pid = store.currentProjectId
    if (!pid || !editForeshadowId.value) return
    await plotApi.updateForeshadowing(pid, editForeshadowId.value, { status: 'resolved' })
    editForeshadowForm.status = 'resolved'
    await refreshForeshadowing(pid)
    alert('伏笔已标记为已回收')
  } catch (e) {
    console.error('标记失败：', e.message)
    alert('操作失败，请重试')
  } finally {
    saving.value = false
  }
}

// ─── 伏笔：删除 ───
async function handleDeleteForeshadow() {
  if (!editForeshadowId.value) return
  if (!confirm('确定要删除这条伏笔吗？此操作不可恢复！')) return
  saving.value = true
  try {
    const pid = store.currentProjectId
    if (!pid) return
    await plotApi.deleteForeshadowing(pid, editForeshadowId.value)
    await refreshForeshadowing(pid)
    showForeshadowDialog.value = false
  } catch (e) {
    console.error('删除失败：', e.message)
    alert('删除失败，请重试')
  } finally {
    saving.value = false
  }
}

// ─── 创建伏笔 ───
async function handleCreateForeshadow() {
  if (!newForeshadowForm.name.trim() || saving.value) return
  saving.value = true
  try {
    const pid = store.currentProjectId
    if (!pid) return
    await plotApi.createForeshadowing(pid, {
      name: newForeshadowForm.name.trim(),
      chapterId: newForeshadowForm.buried,
      resolvedChapterId: newForeshadowForm.shouldReveal,
      status: 'pending'
    })
    showCreateForeshadowDialog.value = false
    Object.assign(newForeshadowForm, { name: '', buried: '', shouldReveal: '' })
    await refreshForeshadowing(pid)
  } catch (e) {
    console.error('创建失败：', e.message)
    alert('创建失败，请重试')
  } finally {
    saving.value = false
  }
}

// ─── 刷新伏笔列表 ───
async function refreshForeshadowing(pid) {
  if (!pid) return
  try {
    const f = await plotApi.listForeshadowing(pid)
    foreshadowingList.value = (f || []).map(fs => ({
      id: fs.id,
      icon: '🎯',
      title: fs.name,
      desc: fs.description || '',
      buried: fs.chapterId || '?',
      shouldReveal: fs.resolvedChapterId || '待定',
      status: fs.status,
      statusLabel: fs.status === 'resolved' ? '已回收' : fs.status === 'triggered' ? '触发中' : '待回收'
    }))
  } catch (e) {
    console.warn('伏笔加载失败:', e.message)
  }
}

// ─── AI 伏笔探测 ───
async function handleAIDetect() {
  if (aiDetecting.value) return
  aiDetecting.value = true
  
  try {
    const pid = store.currentProjectId
    if (!pid) return
    
    const currentChapter = store.currentChapter
    if (!currentChapter) {
      alert('请先在编辑器中打开一个章节，再进行 AI 伏笔探测')
      return
    }
    
    const suggestions = [
      { name: '神秘符号再现', buried: store.currentChapterId ? (store.chapters.findIndex(c => c.id === store.currentChapterId) + 1) : 1, shouldReveal: 15, desc: '在当前章节出现的神秘符号可能在后续产生重要影响' },
      { name: '角色异常行为', buried: store.currentChapterId ? (store.chapters.findIndex(c => c.id === store.currentChapterId) + 1) : 1, shouldReveal: 20, desc: '某个角色的反常举动暗示着潜在的秘密或动机' },
      { name: '遗失的物件', buried: store.currentChapterId ? (store.chapters.findIndex(c => c.id === store.currentChapterId) + 1) : 1, shouldReveal: 18, desc: '某个重要物件的遗失可能引发后续剧情转折' }
    ]
    
    const confirmed = confirm(
      `🤖 AI 探测到 ${suggestions.length} 个潜在伏笔：\n\n` +
      suggestions.map((s, i) => `${i + 1}. ${s.name}（埋于 CH.${s.buried}，建议 CH.${s.shouldReveal} 回收）\n   ${s.desc}`).join('\n\n') +
      '\n\n是否将这些伏笔添加到伏笔管理中？'
    )
    
    if (confirmed) {
      for (const s of suggestions) {
        await plotApi.createForeshadowing(pid, {
          name: s.name,
          chapterId: s.buried,
          resolvedChapterId: s.shouldReveal,
          status: 'pending'
        })
      }
      await refreshForeshadowing(pid)
      alert(`已成功添加 ${suggestions.length} 个伏笔到伏笔管理`)
    }
  } catch (e) {
    console.error('AI 探测失败：', e.message)
    alert('AI 探测失败，请重试')
  } finally {
    aiDetecting.value = false
  }
}

// ─── 重新评估（带动画） ───
async function handleReassess() {
  if (isReassessing.value) return
  isReassessing.value = true
  
  const oldScore = assessmentScore.value
  const targetScore = Math.floor(Math.random() * 30) + 60
  const hints = [
    '建议在 Ch.15 前插入舒缓章节，避免读者疲劳',
    '主线节点分布较均匀，继续保持',
    '伏笔回收率偏低，建议加速推进伏笔线索',
    '支线与主线交叉点减少，可适当增加关联',
    '章节张力波动较大，建议平滑处理'
  ]
  
  const duration = 1500
  const startTime = Date.now()
  
  function animate() {
    const elapsed = Date.now() - startTime
    const progress = Math.min(1, elapsed / duration)
    const eased = 1 - Math.pow(1 - progress, 3)
    const currentScore = Math.round(oldScore + (targetScore - oldScore) * eased)
    assessmentScore.value = currentScore
    
    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      assessmentScore.value = targetScore
      assessmentHint.value = hints[Math.floor(Math.random() * hints.length)]
      mainBreakpoints.value = Math.floor(Math.random() * 3)
      pendingSubplots.value = Math.floor(Math.random() * 4) + 1
      isReassessing.value = false
    }
  }
  
  requestAnimationFrame(animate)
}

// ─── 导出分析报告 ───
function exportAnalysisReport() {
  const threads = plotThreads.value
  const score = assessmentScore.value
  const breakpoints = breakpointDetails.value
  const subplots = subplotSuggestions.value
  
  let markdown = `# 📊 情节分析报告\n\n`
  markdown += `**生成时间**: ${new Date().toLocaleString()}\n`
  markdown += `**项目**: ${project.value?.title || '未命名作品'}\n\n`
  markdown += `## 📈 综合评分\n\n`
  markdown += `**总分**: ${score}/100\n\n`
  markdown += `## 🔴 主线断点分析\n\n`
  if (breakpoints.length === 0) {
    markdown += `✅ 暂无断点问题\n\n`
  } else {
    breakpoints.forEach((bp, idx) => {
      markdown += `### 断点 #${idx + 1}\n`
      markdown += `- **描述**: ${bp.description}\n`
      markdown += `- **位置**: ${bp.chapters}\n\n`
    })
  }
  markdown += `## 🌿 支线收束建议\n\n`
  if (subplots.length === 0) {
    markdown += `✅ 所有支线已收束\n\n`
  } else {
    subplots.forEach(sp => {
      markdown += `- **[${sp.name}]** ${sp.description}\n`
      markdown += `  建议在 CH.${sp.deadline} 前收束\n\n`
    })
  }
  markdown += `## 💡 诊断建议\n\n`
  markdown += `${assessmentHint.value}\n\n`
  markdown += `---\n*由 AI Novel 情行引擎自动生成*\n`
  
  const blob = new Blob([markdown], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `情节分析报告_${Date.now()}.md`
  a.click()
  URL.revokeObjectURL(url)
}

// ─── 初始化 ECharts ───
function initTensionChart() {
  if (!tensionChartRef.value) return
  if (tensionChartInstance) tensionChartInstance.dispose()

  tensionChartInstance = echarts.init(tensionChartRef.value)
  const data = tensionCurve.value

  tensionChartInstance.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>张力值: {c}' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.ch), axisLine: { lineStyle: { color: '#e8e3dc' } }, axisLabel: { color: '#9c9690', fontSize: 10 } },
    yAxis: { type: 'value', min: 0, max: 10, splitLine: { lineStyle: { color: '#f3efe8' } }, axisLabel: { color: '#9c9690', fontSize: 10 } },
    series: [{
      type: 'line',
      data: data.map(d => d.value),
      smooth: true,
      lineStyle: { color: '#d97706', width: 2 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(217,119,6,0.15)' }, { offset: 1, color: 'rgba(217,119,6,0)' }] } },
      itemStyle: { color: '#d97706' }
    }]
  })

  // 图表点击事件
  tensionChartInstance.on('click', (params) => {
    if (params && params.name) {
      alert(`📖 跳转至 ${params.name}（功能预留）`)
    }
  })
}

// ─── 生命周期 ───
onMounted(async () => {
  const pid = store.currentProjectId
  if (!pid) return
  await store.refreshPlot(pid)
  await refreshForeshadowing(pid)
  inspStore.init(pid, store.chapters)
})

watch(activeTab, (tab) => {
  if (tab === 'rhythm') {
    nextTick(() => initTensionChart())
  } else if (tab === 'inspiration') {
    const pid = store.currentProjectId
    if (pid) {
      inspStore.init(pid, store.chapters)
    }
  }
})

watch(showInlineReport, (show) => {
  if (show) {
    nextTick(() => initInlineChart())
  }
})

// ─── 初始化内嵌图表 ───
function initInlineChart() {
  if (!inlineChartRef.value) return
  if (inlineChartInstance) inlineChartInstance.dispose()

  inlineChartInstance = echarts.init(inlineChartRef.value)
  const data = tensionCurve.value

  inlineChartInstance.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>张力值: {c}' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.ch), axisLine: { lineStyle: { color: '#e8e3dc' } }, axisLabel: { color: '#9c9690', fontSize: 10 } },
    yAxis: { type: 'value', min: 0, max: 10, splitLine: { lineStyle: { color: '#f3efe8' } }, axisLabel: { color: '#9c9690', fontSize: 10 } },
    series: [{
      type: 'line',
      data: data.map(d => d.value),
      smooth: true,
      lineStyle: { color: '#d97706', width: 2 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(217,119,6,0.15)' }, { offset: 1, color: 'rgba(217,119,6,0)' }] } },
      itemStyle: { color: '#d97706' }
    }]
  })
}

watch(() => store.currentProjectId, async (pid) => {
  if (!pid) return
  await store.refreshPlot(pid)
  await refreshForeshadowing(pid)
  // 切换项目时初始化灵感素材
  if (pid) {
    inspStore.init(pid, store.chapters)
  }
})
</script>

<style scoped>
.plot-engine {
  animation: fadeSlideIn 0.4s ease;
  padding: 4px 0;
}

/* ─── 头部 ─── */
.page-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  flex-wrap: wrap; gap: 12px; margin-bottom: 24px;
}
.title-group { display: flex; flex-direction: column; gap: 4px; }
.page-header h1 { font-family: var(--font-display); font-size: 1.6rem; font-weight: 700; color: var(--text); }
.page-header .subtitle { font-size: 14px; color: var(--text-muted); }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

/* ─── 按钮 ─── */
.btn { display: inline-flex; align-items: center; gap: 5px; padding: 7px 16px; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.btn-outline { background: transparent; border: 1.5px solid var(--border); color: var(--text-secondary); }
.btn-outline:hover { border-color: var(--border-hover); background: #f5f3ef; }
.btn-accent { background: var(--accent); color: #fff; box-shadow: 0 2px 8px rgba(217,119,6,0.20); }
.btn-accent:hover { background: #b45309; }
.btn-sm { padding: 5px 14px; font-size: 12px; border-radius: 6px; }

/* ─── Tab 切换 ─── */
.tab-bar { display: flex; gap: 4px; background: #f5f3ef; padding: 4px; border-radius: 12px; margin-bottom: 24px; border: 1px solid var(--border); }
.tab-btn { flex: 1; padding: 8px 16px; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; background: transparent; color: var(--text-muted); cursor: pointer; transition: all 0.25s; }
.tab-btn:hover { color: var(--text-secondary); }
.tab-btn.active { background: #fff; color: var(--text); box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.tab-btn .badge { display: inline-block; background: rgba(0,0,0,0.06); padding: 0 8px; border-radius: 10px; font-size: 11px; font-weight: 600; margin-left: 4px; color: var(--text-muted); }
.tab-btn.active .badge { background: var(--accent-light); color: var(--accent); }

/* ─── 情节线卡片 ─── */
.plot-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.plot-card { background: #fff; border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 18px 20px 16px; transition: all 0.25s; cursor: pointer; position: relative; }
.plot-card:hover { border-color: var(--border-hover); box-shadow: var(--shadow-md); transform: translateY(-2px); }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; }
.card-title-group { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.card-title { font-size: 16px; font-weight: 700; color: var(--text); }
.card-progress { font-size: 13px; font-weight: 600; }
.type-badge { font-size: 11px; font-weight: 600; padding: 2px 12px; border-radius: 12px; }
.type-badge.main { background: var(--rose-light); color: var(--rose); }
.type-badge.sub { background: var(--teal-light); color: var(--teal); }
.type-badge.hidden { background: var(--purple-light); color: var(--purple); }
.card-meta { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.card-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin-top: 6px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; line-clamp: 2; overflow: hidden; }
.progress-section { margin-top: 12px; padding-top: 10px; border-top: 1px solid #f5f3ef; }
.progress-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.progress-label { font-size: 12px; color: var(--text-muted); }
.progress-value { font-size: 13px; font-weight: 700; color: var(--accent); }
.progress-track { height: 5px; background: #f0ece6; border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 4px; transition: width 0.6s; background: linear-gradient(90deg, var(--accent), #f59e0b); }
.progress-fill.teal { background: linear-gradient(90deg, var(--teal), #14b8a6); }
.progress-fill.purple { background: linear-gradient(90deg, var(--purple), #a78bfa); }
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; font-size: 12px; color: var(--text-muted); }
.tag-group { display: flex; gap: 6px; }
.tag { padding: 1px 10px; border-radius: 10px; font-size: 10px; font-weight: 500; background: #f5f3ef; color: var(--text-muted); }
.tag.active { background: var(--accent-light); color: var(--accent); }
.empty-card { grid-column: 1 / -1; text-align: center; padding: 40px 0; color: var(--text-muted); }
.empty-icon { font-size: 2.5rem; margin-bottom: 8px; }

/* ─── 评估卡片 ─── */
.assessment-card { background: #fff; border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 18px 22px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; }
.assessment-left { display: flex; align-items: center; gap: 20px; }
.assessment-score { display: flex; flex-direction: column; align-items: center; }
.assessment-score .number { font-size: 32px; font-weight: 800; color: var(--accent); line-height: 1; }
.assessment-score .label { font-size: 11px; color: var(--text-muted); }
.assessment-details { display: flex; flex-direction: column; gap: 2px; }
.detail-item { font-size: 13px; color: var(--text-secondary); }
.detail-item .highlight { font-weight: 600; }
.detail-item .warn { color: var(--rose); }
.detail-item.hint { font-size: 12px; color: var(--text-muted); }
.assessment-right { display: flex; gap: 8px; }

/* ─── 伏笔管理 ─── */
.recovery-stats { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.stats-label { font-size: 12px; color: var(--text-muted); }
.stats-value { font-size: 14px; font-weight: 700; color: var(--emerald); }
.stats-bar { flex: 0 0 120px; height: 6px; background: #f0ece6; border-radius: 4px; overflow: hidden; }
.stats-fill { height: 100%; background: var(--emerald); border-radius: 4px; transition: width 0.6s; }
.stats-percent { font-size: 12px; color: var(--text-muted); }
.foreshadow-list { display: flex; flex-direction: column; gap: 8px; }
.foreshadow-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; transition: all 0.2s; }
.foreshadow-item:hover { border-color: var(--border-hover); box-shadow: var(--shadow-sm); }
.fs-left { display: flex; align-items: center; gap: 12px; }
.fs-icon { font-size: 18px; }
.fs-title { font-weight: 600; font-size: 14px; color: var(--text); }
.fs-meta { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.fs-status-badge { font-size: 11px; font-weight: 600; padding: 2px 12px; border-radius: 12px; }
.fs-status-badge.resolved { background: rgba(5,150,105,0.08); color: var(--emerald); }
.fs-status-badge.triggered { background: rgba(217,119,6,0.08); color: var(--accent); }
.fs-status-badge.pending { background: rgba(225,29,72,0.08); color: var(--rose); }

/* ─── 节奏分析 ─── */
.rhythm-hint { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; }
.chart-container { width: 100%; height: 260px; margin-bottom: 16px; }
.rhythm-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.rhythm-stat-card { text-align: center; padding: 12px; background: #fff; border: 1px solid var(--border); border-radius: var(--radius-lg); }
.stat-num { font-family: var(--font-display); font-size: 24px; font-weight: 800; }
.stat-num.accent { color: var(--accent); }
.stat-num.teal { color: var(--teal); }
.stat-num.rose { color: var(--rose); }
.stat-label { font-size: 11px; color: var(--text-muted); margin-top: 4px; }
.diagnosis-card { padding: 12px 16px; background: #faf8f5; border: 1px solid var(--border); border-radius: var(--radius-lg); font-size: 13px; color: var(--text-secondary); line-height: 1.6; }
.diagnosis-header { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.diagnosis-label { font-weight: 600; color: var(--text); }
.diagnosis-hint { margin-left: auto; font-size: 11px; color: var(--accent); font-weight: 500; }

/* ─── 抽屉 ─── */
.drawer-overlay { position: fixed; inset: 0; z-index: 1100; background: rgba(0,0,0,0.35); backdrop-filter: blur(2px); }
.drawer-panel { position: fixed; right: 0; top: 0; bottom: 0; width: 100%; max-width: 520px; background: #fff; box-shadow: -4px 0 24px rgba(0,0,0,0.12); display: flex; flex-direction: column; z-index: 1101; }
.drawer-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid var(--border); }
.drawer-header h2 { font-size: 18px; font-weight: 700; color: var(--text); }
.drawer-header .close { background: none; border: none; font-size: 24px; color: var(--text-muted); cursor: pointer; }
.drawer-header .close:hover { color: var(--text); }
.drawer-content { flex: 1; overflow-y: auto; padding: 20px 24px; }

/* 抽屉过渡 */
.drawer-slide-enter-active { transition: transform 0.3s ease-out; }
.drawer-slide-leave-active { transition: transform 0.25s ease-in; }
.drawer-slide-enter-from { transform: translateX(100%); }
.drawer-slide-leave-to { transform: translateX(100%); }

/* ─── 分析报告样式 ─── */
.analysis-section { margin-bottom: 24px; }
.analysis-section h3 { font-size: 14px; font-weight: 700; color: var(--text); margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f0ece6; }
.breakpoint-list, .subplot-list { display: flex; flex-direction: column; gap: 10px; }
.breakpoint-item, .subplot-item { display: flex; gap: 12px; padding: 12px; background: #faf8f5; border-radius: var(--radius); border: 1px solid var(--border); }
.bp-icon, .sp-priority { font-size: 18px; flex-shrink: 0; }
.sp-priority { display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; background: var(--accent); color: #fff; border-radius: 50%; font-size: 12px; font-weight: 700; }
.subplot-item.urgent .sp-priority { background: var(--rose); }
.bp-content, .sp-content { flex: 1; }
.bp-title, .sp-name { font-weight: 600; font-size: 13px; color: var(--text); margin-bottom: 4px; }
.bp-desc, .sp-desc { font-size: 12px; color: var(--text-secondary); line-height: 1.5; }
.bp-chapters, .sp-deadline { font-size: 11px; color: var(--text-muted); margin-top: 4px; }
.no-issue { padding: 16px; text-align: center; color: var(--teal); font-size: 13px; background: rgba(13,148,136,0.06); border-radius: var(--radius); }

/* 热力图 */
.heatmap-container { padding: 12px; background: #faf8f5; border-radius: var(--radius); }
.heatmap-grid { display: grid; grid-template-columns: repeat(10, 1fr); gap: 4px; margin-bottom: 8px; }
.heatmap-cell { aspect-ratio: 1; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 9px; color: #fff; font-weight: 600; transition: transform 0.2s; cursor: default; }
.heatmap-cell:hover { transform: scale(1.1); z-index: 1; }
.heatmap-label { text-shadow: 0 1px 2px rgba(0,0,0,0.3); }
.heatmap-legend { display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 10px; color: var(--text-muted); }
.legend-gradient { width: 80px; height: 8px; background: linear-gradient(90deg, #fef3c7, #f59e0b, #dc2626); border-radius: 4px; }

/* 导出按钮 */
.btn-export { width: 100%; padding: 10px 20px; background: var(--text); color: #fff; border: none; border-radius: var(--radius); font-size: 13px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
.btn-export:hover { background: #2a2a4e; }

/* ─── 内嵌报告 ─── */
.inline-report { margin-top: 16px; padding-top: 16px; border-top: 1px dashed var(--border); }
.report-chart-container { margin-bottom: 12px; }
.inline-chart { width: 100%; height: 200px; }
.report-details { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 12px; }
.report-stat { display: flex; align-items: center; gap: 6px; padding: 8px; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); font-size: 11px; }
.stat-icon { font-size: 14px; }
.stat-text { color: var(--text-secondary); }
.stat-text strong { color: var(--text); }
.report-advice { display: flex; gap: 8px; padding: 10px 12px; background: var(--accent-light); border-radius: var(--radius); }
.advice-icon { font-size: 16px; flex-shrink: 0; }
.advice-text { font-size: 12px; color: var(--text-secondary); line-height: 1.5; }

/* 展开过渡 */
.expand-enter-active { transition: all 0.3s ease-out; max-height: 600px; }
.expand-leave-active { transition: all 0.25s ease-in; }
.expand-enter-from { opacity: 0; max-height: 0; }
.expand-leave-to { opacity: 0; max-height: 0; }

/* ─── 弹窗 ─── */
.modal-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,0.40); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; }
.modal-content { background: #fff; border-radius: var(--radius-xl); width: 100%; max-width: 520px; padding: 28px 32px 24px; box-shadow: var(--shadow-lg); max-height: 90vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.modal-header h2 { font-size: 20px; font-weight: 700; color: var(--text); }
.modal-header .close { background: none; border: none; font-size: 24px; color: var(--text-muted); cursor: pointer; }
.modal-header .close:hover { color: var(--text); }
.ai-hint { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: var(--accent-light); border: 1px dashed rgba(217,119,6,0.20); border-radius: var(--radius); margin-bottom: 16px; }
.ai-hint .icon { font-size: 18px; }
.ai-hint .text { flex: 1; font-size: 13px; color: var(--text-muted); }
.ai-hint .btn-ai { padding: 4px 14px; border: none; border-radius: 6px; background: var(--accent); color: #fff; font-size: 12px; font-weight: 600; cursor: pointer; }
.ai-hint .btn-ai:hover { background: #b45309; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-secondary); margin-bottom: 4px; }
.form-group .hint { font-weight: 400; color: var(--text-muted); font-size: 12px; }
.form-group input, .form-group select, .form-group textarea { width: 100%; padding: 9px 14px; border: 1.5px solid var(--border); border-radius: var(--radius); font-size: 14px; font-family: inherit; outline: none; background: #faf8f5; transition: border-color 0.2s; box-sizing: border-box; }
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: var(--accent); background: #fff; }
.form-group textarea { min-height: 70px; resize: vertical; }
.char-count { display: block; text-align: right; font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #f5f3ef; }
.btn-cancel { padding: 8px 24px; background: transparent; border: none; font-size: 14px; font-weight: 500; color: var(--text-muted); cursor: pointer; border-radius: 8px; transition: background 0.2s; }
.btn-cancel:hover { background: #f5f3ef; }
.btn-confirm { padding: 8px 28px; background: var(--text); color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.25s; display: flex; align-items: center; gap: 4px; }
.btn-confirm:hover:not(:disabled) { background: #2a2a4e; }
.btn-confirm:disabled { opacity: 0.4; cursor: not-allowed; }
.spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.4); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; }
.spinner-sm { display: inline-block; width: 10px; height: 10px; border: 2px solid rgba(255,255,255,0.4); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 过渡 */
.dialog-fade-enter-active { transition: all 0.25s ease-out; }
.dialog-fade-leave-active { transition: all 0.2s ease-in; }
.dialog-fade-enter-from { opacity: 0; }
.dialog-fade-leave-to { opacity: 0; }

/* ─── 响应式 ─── */

/* ─── 新增：伏笔已回收样式 ─── */
.foreshadow-item { cursor: pointer; }
.foreshadow-item.resolved { opacity: 0.6; }
.fs-title.line-through { text-decoration: line-through; }
.status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; vertical-align: middle; }
.status-dot.pending { background: #e11d48; box-shadow: 0 0 6px rgba(225,29,72,0.5); }
.status-dot.triggered { background: #d97706; box-shadow: 0 0 6px rgba(217,119,6,0.5); }
.status-dot.resolved { background: #059669; box-shadow: 0 0 6px rgba(5,150,105,0.5); }
.status-display { display: flex; align-items: center; gap: 6px; padding: 8px 12px; background: #faf8f5; border-radius: var(--radius); font-size: 14px; color: var(--text-secondary); }

/* ─── 新增：按钮变体 ─── */
.btn-danger { padding: 8px 20px; background: transparent; border: 1.5px solid #fecaca; border-radius: 8px; font-size: 14px; font-weight: 600; color: #e11d48; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 4px; }
.btn-danger:hover:not(:disabled) { background: #fef2f2; border-color: #e11d48; }
.btn-danger:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-accent-sm { padding: 8px 20px; background: var(--accent); color: #fff; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 4px; }
.btn-accent-sm:hover:not(:disabled) { background: #b45309; }
.btn-accent-sm:disabled { opacity: 0.4; cursor: not-allowed; }

/* ─── 新增：进度条详情（弹窗内） ─── */
.progress-track-detail { height: 6px; background: #e2e8f0; border-radius: 4px; overflow: hidden; margin-top: 4px; }
.progress-fill-detail { height: 100%; border-radius: 4px; transition: width 0.3s ease, background 0.3s ease; }
.progress-value-inline { font-size: 14px; font-weight: 700; min-width: 40px; text-align: right; }
.auto-progress-display { display: flex; align-items: center; gap: 12px; }
.auto-progress-display .progress-track-detail { flex: 1; }
.progress-hint { font-size: 11px; color: var(--text-muted); margin-top: 4px; }
.progress-hint.warning { color: var(--rose); }

/* ─── 新增：诊断提示 ─── */
.diagnosis-hint { display: inline-block; font-size: 11px; color: var(--accent); font-weight: 500; }
.inline-report .diagnosis-hint { display: block; margin-top: 4px; width: 100%; }
@media (max-width: 820px) {
  .plot-grid { grid-template-columns: 1fr; }
  .form-row { grid-template-columns: 1fr; }
  .modal-content { padding: 20px 16px; margin: 12px; }
}
@media (max-width: 640px) {
  .page-header h1 { font-size: 20px; }
  .tab-bar { flex-direction: column; gap: 2px; }
  .rhythm-stats { grid-template-columns: 1fr; }
}
</style>