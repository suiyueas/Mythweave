<template>
  <div class="h-screen flex flex-col bg-[#faf7f2]">

    <!-- ══════════ 主体 ══════════ -->
    <div class="flex flex-1 overflow-hidden">

      <!-- ══════════ 左栏：大分类工具导航 ══════════ -->
      <aside class="w-52 bg-[#faf8f5] border-r border-[#e8e3dc] flex-shrink-0 flex flex-col">
        <!-- 返回作品列表 -->
        <div class="px-2 pt-2 pb-1 flex-shrink-0">
          <button
            @click="goToMyWorks"
            class="w-full text-xs text-[#6b6560] hover:text-[#d97706] hover:bg-[#f3efe8] transition-colors flex items-center gap-1 px-3 py-2 rounded-lg"
            title="返回作品列表"
          >
            ← 返回作品列表
          </button>
        </div>
        <nav class="py-2 flex-1 overflow-y-auto">
          <template v-for="group in toolGroups" :key="group.label">
            <div class="text-[10px] font-bold uppercase tracking-wider text-[#b8b0a8] px-4 pt-3 pb-1.5 first:pt-2">{{ group.label }}</div>
            <div v-for="tool in group.items" :key="tool.key"
              class="flex items-center gap-2.5 px-4 py-2.5 mx-2 rounded-lg text-sm cursor-pointer transition-colors mb-0.5"
              :class="getToolActiveClass(tool.key)"
              @click="handleToolClick(tool.key)">
              <span class="text-base flex-shrink-0 w-5 text-center">{{ tool.icon }}</span>
              <span class="truncate">{{ tool.label }}</span>
            </div>
          </template>
        </nav>
        <!-- 左下角用户信息 -->
        <div class="flex-shrink-0 px-3 py-2.5 border-t border-[#e8e3dc]">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0" style="background:linear-gradient(135deg, #0e7490, #7c3aed)">墨</div>
            <div class="min-w-0">
              <div class="text-xs font-semibold text-[#6b6560] truncate">墨染青衫</div>
              <div class="text-[10px] text-[#9c9690]">签约作者 · LV.8</div>
            </div>
          </div>
        </div>
      </aside>

      <!-- ══════════ 中心：动态面板 ══════════ -->
      <main class="flex-1 flex flex-col overflow-hidden bg-[#faf7f2]">

        <!-- ══ 面板：写作仪表盘 ══ -->
        <template v-if="activeTool === 'dashboard'">
          <div class="flex-1 overflow-y-auto">
            <Dashboard @navigate="activeTool = $event" />
          </div>
        </template>

        <!-- ══ 面板：AI 写作编辑器（三栏布局）══ -->
        <template v-if="activeTool === 'aiWrite'">
          <Editor />
        </template>

        <!-- ══ 面板：智能大纲 ══ -->
        <template v-else-if="activeTool === 'outline'">
          <OutlinePanel :project-id="store.currentProjectId" />
        </template>

        <!-- ══ 面板：章节管理 ══ -->
        <template v-else-if="activeTool === 'chapters'">
          <div class="flex-1 overflow-y-auto px-6 py-4">
            <Chapters />
          </div>
        </template>

        <!-- ══ 面板：世界观构建（使用 WorldBuilding 组件） ══ -->
        <template v-else-if="activeTool === 'world'">
          <div class="flex-1 overflow-y-auto px-6 py-4">
            <WorldBuilding inline-mode />
          </div>
        </template>

        <!-- ══ 面板：人物工坊 ══ -->
        <template v-else-if="activeTool === 'characters'">
          <div class="flex items-center px-4 py-1.5 border-b border-[#e8e3dc] bg-[#faf8f5] flex-shrink-0 gap-3">
            <span class="text-xs font-semibold text-[#6b6560]">👤 人物工坊</span>
            <span class="text-[#d4cec6] text-xs">|</span>
            <button v-for="tab in ['卡片视图']" :key="tab" class="text-xs px-2.5 py-0.5 rounded-full transition-colors" :class="charTab === tab ? 'bg-[#d97706] text-white' : 'text-[#6b6560] hover:bg-white'" @click="charTab = tab">{{ tab }}</button>
            <div class="flex-1"></div>
            <button class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg hover:bg-[#b45309] transition-colors" @click="handleAddCharacter">+ 新建角色</button>
          </div>
          <div class="flex-1 overflow-y-auto px-10 py-8">
            <div class="grid grid-cols-3 gap-3">
              <div v-for="char in characterList" :key="char.id" class="border border-[#e8e3dc] rounded-xl p-4 hover:border-[#d97706] hover:shadow-md hover:-translate-y-0.5 transition-all cursor-pointer bg-white" :class="{ '!border-[#d97706] !shadow-md ring-2 ring-[#d97706]/20': selectedCharacter?.id === char.id }" @click="viewCharacter(char)">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-10 h-10 rounded-full flex items-center justify-center text-white text-sm font-bold" :style="{ background: char.color }">{{ char.avatar }}</div>
                  <div class="min-w-0"><div class="text-sm font-semibold text-[#6b6560] truncate">{{ char.name }}</div><div class="text-xs text-[#9c9690]">{{ char.role }}</div></div>
                </div>
                <p class="text-xs text-[#9c9690] leading-relaxed line-clamp-2">{{ char.bio }}</p>
                <div class="flex gap-1.5 mt-2 flex-wrap">
                  <span v-for="tag in char.tags" :key="tag" class="px-1.5 py-0.5 rounded-md bg-[#f3efe8] text-[10px] text-[#9c9690]">{{ tag }}</span>
                </div>
                <div class="mt-2 pt-2 border-t border-[#f3efe8]">
                  <div class="flex items-center justify-between text-[10px]"><span class="text-[#9c9690]">弧光进度</span><span class="font-semibold text-[#6b6560]">{{ char.arc }}%</span></div>
                  <div class="w-full h-1.5 bg-[#f3efe8] rounded-full mt-1 overflow-hidden"><div class="h-full rounded-full transition-all duration-300" :style="{ width: char.arc + '%', background: char.color }"></div></div>
                </div>
                <div class="mt-2 text-center">
                  <span class="text-[10px] text-[#c4bdb5]">点击查看详情</span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ══ 面板：情节引擎 ══ -->
        <template v-else-if="activeTool === 'plot'">
          <div class="flex-1 overflow-y-auto px-6 py-4">
            <PlotEngine />
          </div>
        </template>

        <!-- ══ 面板：灵感素材库（使用 InspirationSection 组件） ══ -->
        <template v-else-if="activeTool === 'inspiration'">
          <div class="flex-1 overflow-y-auto px-6 py-4">
            <InspirationSection ref="inspirationSectionRef" :projectId="store.currentProjectId" />
          </div>
        </template>

        <!-- ══ 面板：AI 对话助手 ══ -->
        <template v-else-if="activeTool === 'aiChat'">
          <AiChat />
        </template>

        <!-- ══ 面板：智能哨兵 ══ -->
        <template v-else-if="activeTool === 'sentinel'">
          <div class="flex items-center px-4 py-1.5 border-b border-[#e8e3dc] bg-[#faf8f5] flex-shrink-0 gap-2">
            <span class="text-xs font-semibold text-[#6b6560]">🔔 智能哨兵</span>
            <span class="text-[#d4cec6] text-xs">|</span>
            <span class="text-xs" :class="store.sentinelCriticalCount > 0 ? 'text-red-600' : 'text-emerald-600'">
              {{ store.sentinelCriticalCount > 0 ? store.sentinelCriticalCount + ' 条严重告警' : '一切正常' }}
            </span>
            <div class="flex-1"></div>
            <button v-if="!store.scanning" class="text-xs px-2.5 py-1 rounded-lg bg-[#d97706] text-white hover:bg-[#b45309] transition-colors font-medium" @click="handleScan">🔄 执行巡查</button>
            <span v-else class="text-xs text-[#d97706] font-medium">⏳ 巡查中...</span>
            <span class="text-[10px] text-[#9c9690]">上次巡查：{{ lastCheckTime }}</span>
          </div>
          <div class="flex-1 overflow-y-auto px-6 py-6">
            <!-- ═══ 统计卡片 ═══ -->
            <div class="grid grid-cols-4 gap-3 mb-5">
              <div v-for="card in enhancedStatCards" :key="card.key"
                class="relative border rounded-xl p-3.5 bg-white cursor-pointer transition-all hover:shadow-md hover:-translate-y-0.5"
                :class="[card.borderColor, sentinelTypeFilter === card.key ? 'ring-2 ring-offset-1 ' + card.ringColor : '']"
                @click="toggleTypeFilter(card.key)">
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs text-[#9c9690]">{{ card.label }}</span>
                  <span v-if="card.criticalCount > 0" class="absolute -top-1 -right-1 min-w-[18px] h-[18px] rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center px-1 shadow-sm">{{ card.criticalCount }}</span>
                </div>
                <div class="text-2xl font-bold" :class="card.textColor" style="font-family:var(--font-display)">{{ card.value }}</div>
                <div class="flex items-center gap-1 mt-1.5">
                  <span class="text-[10px]" :class="card.changeUp ? 'text-red-500' : 'text-emerald-500'">{{ card.changeUp ? '↑' : '↓' }} {{ card.change }}</span>
                  <span class="text-[10px] text-[#9c9690]">较上次</span>
                </div>
              </div>
            </div>

            <!-- ═══ 筛选栏 ═══ -->
            <div class="flex items-center gap-2 mb-4 flex-wrap">
              <span class="text-xs text-[#9c9690]">筛选：</span>
              <button v-for="t in typeFilterOptions" :key="t.key"
                class="text-xs px-2.5 py-1 rounded-full transition-colors"
                :class="sentinelTypeFilter === t.key ? 'bg-[#d97706] text-white' : 'text-[#6b6560] hover:bg-[#f3efe8]'"
                @click="sentinelTypeFilter = t.key">{{ t.label }}</button>
              <span class="text-[#d4cec6] text-xs mx-1">|</span>
              <button v-for="s in statusFilterOptions" :key="s.key"
                class="text-xs px-2.5 py-1 rounded-full transition-colors"
                :class="sentinelStatusFilter === s.key ? 'bg-[#d97706] text-white' : 'text-[#6b6560] hover:bg-[#f3efe8]'"
                @click="sentinelStatusFilter = s.key">{{ s.label }}</button>
            </div>

            <!-- ═══ 巡查进度 ═══ -->
            <div v-if="store.scanProgress" class="mb-4 p-4 rounded-xl bg-white border border-[#e8e3dc] shadow-sm">
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm font-semibold text-[#6b6560]">📡 巡查进度</span>
                <span class="text-xs font-bold text-[#3b82f6]">{{ store.scanProgress.progress || 0 }}%</span>
              </div>
              <div class="w-full h-2.5 bg-[#f3efe8] rounded-full overflow-hidden mb-3">
                <div class="h-full rounded-full transition-all duration-700 ease-out"
                  :style="{ width: (store.scanProgress.progress || 0) + '%', background: 'linear-gradient(90deg, #3b82f6, #6366f1)' }"></div>
              </div>
              <div class="grid grid-cols-4 gap-2">
                <div v-for="dim in store.scanProgress.dimensions" :key="dim.name"
                  class="text-center p-2 rounded-lg" :class="dim.status === 'completed' ? 'bg-emerald-50' : dim.status === 'failed' ? 'bg-red-50' : dim.status === 'running' ? 'bg-blue-50' : 'bg-[#faf8f5]'">
                  <div class="text-[10px] font-semibold mb-0.5" :class="dim.status === 'completed' ? 'text-emerald-700' : dim.status === 'failed' ? 'text-red-700' : dim.status === 'running' ? 'text-blue-700' : 'text-[#9c9690]'">
                    {{ dimLabels[dim.name] || dim.name }}
                  </div>
                  <div class="text-xs" :class="dim.status === 'completed' ? 'text-emerald-600' : dim.status === 'failed' ? 'text-red-600' : dim.status === 'running' ? 'text-blue-600' : 'text-[#9c9690]'">
                    {{ dim.status === 'completed' ? '✅' : dim.status === 'failed' ? '❌' : dim.status === 'running' ? '⏳' : '⏸' }}
                    {{ dim.alertsFound }}条
                  </div>
                </div>
              </div>
              <div v-if="store.scanProgress.elapsedMs" class="text-[10px] text-[#9c9690] mt-2 text-right">已耗时 {{ (store.scanProgress.elapsedMs / 1000).toFixed(1) }}s</div>
            </div>

            <!-- ═══ 告警列表 ═══ -->
            <div v-if="filteredSentinelAlerts.length > 0">
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs font-semibold text-[#6b6560]">告警列表（{{ filteredSentinelAlerts.length }} 条）</span>
                <button
                  v-if="store.sentinelAlerts.some(a => a.resolved)"
                  class="text-[10px] px-2 py-1 rounded bg-red-50 text-red-600 hover:bg-red-100 transition-colors border border-red-200"
                  @click="handleClearResolvedAlerts"
                >🗑️ 清空已处理</button>
              </div>
              <div v-for="alert in filteredSentinelAlerts" :key="alert.id"
                class="group relative flex items-start gap-3 p-3.5 rounded-xl mb-2 border transition-all cursor-pointer"
                :class="[alert.severity === 'critical' ? 'bg-red-50/70 border-red-200 hover:border-red-400' : alert.severity === 'warning' ? 'bg-[#fffbeb] border-[#fcd34d] hover:border-amber-400' : 'bg-[#f0f9ff] border-[#bae6fd] hover:border-blue-400', alert.resolved ? 'opacity-60' : '']"
                @click="openAlertDetail(alert)">
                <span class="text-lg flex-shrink-0 mt-0.5">{{ severityIcons[alert.severity] || '🔵' }}</span>
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 mb-0.5">
                    <span class="text-sm font-semibold" :class="alert.severity === 'critical' ? 'text-red-700' : alert.severity === 'warning' ? 'text-[#92400e]' : 'text-[#6b6560]'">{{ alert.title }}</span>
                    <span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold flex-shrink-0" :class="severityTagClass(alert.severity)">{{ severityLabels[alert.severity] || alert.severity }}</span>
                    <span v-if="alert.resolved" class="text-[10px] px-1.5 py-0.5 rounded-full bg-gray-100 text-gray-500 flex-shrink-0">已处理</span>
                  </div>
                  <p class="text-xs text-[#9c9690] line-clamp-2">{{ alert.description }}</p>
                </div>
                <div class="flex flex-col items-end gap-1 flex-shrink-0">
                  <span class="text-[10px] text-[#9c9690]">{{ formatSentinelTime(alert.createTime) }}</span>
                  <span class="text-[10px] px-1.5 py-0.5 rounded font-medium" :class="typeTagClass(alert.type)">{{ typeLabels[alert.type] || alert.type }}</span>
                </div>
                <!-- 悬停操作按钮 -->
                <div class="absolute right-2 bottom-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button v-if="!alert.resolved" class="text-[10px] px-2 py-0.5 rounded bg-emerald-500 text-white hover:bg-emerald-600 transition-colors" @click.stop="handleResolveAlert(alert.id)">✓ 已处理</button>
                  <button v-if="!alert.resolved" class="text-[10px] px-2 py-0.5 rounded bg-gray-400 text-white hover:bg-gray-500 transition-colors" @click.stop="handleIgnoreAlert(alert.id)">✕ 忽略</button>
                  <button class="text-[10px] px-2 py-0.5 rounded bg-red-400 text-white hover:bg-red-500 transition-colors" @click.stop="handleDeleteSentinelAlert(alert.id)">🗑️ 删除</button>
                </div>
              </div>
            </div>

            <!-- ═══ 空状态 ═══ -->
            <div v-else class="text-center py-12">
              <div class="text-5xl mb-4">🎉</div>
              <div class="text-sm font-semibold text-[#6b6560] mb-1">暂无告警，一切正常</div>
              <div class="text-xs text-[#9c9690] mb-4">点击上方「执行巡查」按钮，让智能哨兵守护你的作品质量</div>
              <button class="text-xs px-3 py-1.5 rounded-lg bg-[#d97706] text-white hover:bg-[#b45309] transition-colors" @click="handleScan">🔍 首次巡查</button>
            </div>

            <!-- ═══ 巡查历史 ═══ -->
            <div class="mt-6 pt-5 border-t border-[#e8e3dc]">
              <div class="flex items-center justify-between mb-3">
                <span class="text-xs font-semibold text-[#6b6560]">📋 巡查历史</span>
                <button class="text-[10px] text-[#d97706] hover:underline" @click="loadSentinelLogs">刷新</button>
              </div>
              <div v-if="sentinelLogs.length === 0" class="text-center py-4 text-xs text-[#9c9690]">暂无巡查记录</div>
              <div v-else class="overflow-hidden rounded-xl border border-[#e8e3dc]">
                <table class="w-full text-xs">
                  <thead>
                    <tr class="bg-[#faf8f5] text-[#9c9690]">
                      <th class="text-left py-2 px-3 font-medium">时间</th>
                      <th class="text-left py-2 px-3 font-medium">维度</th>
                      <th class="text-left py-2 px-3 font-medium">类型</th>
                      <th class="text-center py-2 px-3 font-medium">告警数</th>
                      <th class="text-center py-2 px-3 font-medium">耗时</th>
                      <th class="text-center py-2 px-3 font-medium">状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="log in sentinelLogs" :key="log.id" class="border-t border-[#f3efe8] hover:bg-[#faf8f5] transition-colors">
                      <td class="py-2 px-3 text-[#6b6560]">{{ formatSentinelTime(log.startedAt) }}</td>
                      <td class="py-2 px-3">{{ dimLabels[log.dimension] || log.dimension }}</td>
                      <td class="py-2 px-3 text-[#9c9690]">{{ log.scanType === 'full' ? '全量巡查' : log.scanType }}</td>
                      <td class="py-2 px-3 text-center font-semibold" :class="log.alertsFound > 0 ? 'text-red-600' : 'text-emerald-600'">{{ log.alertsFound || 0 }}</td>
                      <td class="py-2 px-3 text-center text-[#9c9690]">{{ log.durationMs ? (log.durationMs / 1000).toFixed(1) + 's' : '-' }}</td>
                      <td class="py-2 px-3 text-center">
                        <span class="text-[10px] px-1.5 py-0.5 rounded-full font-medium" :class="log.status === 'completed' ? 'bg-emerald-50 text-emerald-700' : log.status === 'failed' ? 'bg-red-50 text-red-700' : 'bg-amber-50 text-amber-700'">
                          {{ log.status === 'completed' ? '完成' : log.status === 'failed' ? '失败' : '进行中' }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </template>

        <!-- ══ 面板：策略与设置 ══ -->
        <template v-else-if="activeTool === 'strategy'">
          <div class="flex items-center px-4 py-1.5 border-b border-[#e8e3dc] bg-[#faf8f5] flex-shrink-0 gap-3">
            <span class="text-xs font-semibold text-[#6b6560]">⚙️ 策略与设置</span>
            <span class="text-[#d4cec6] text-xs">|</span>
            <div class="flex-1"></div>
            <div v-if="statusPollingTimer" class="flex items-center gap-1 text-[10px] text-[#9c9690]">
              <span class="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse"></span>
              <span>自动刷新中</span>
            </div>
            <button v-if="systemStatusLoading" class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg animate-pulse">加载中...</button>
            <button v-else class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg hover:bg-[#b45309] transition-colors" @click="refreshSystemStatus">🔄 刷新状态</button>
          </div>
          <div class="flex-1 overflow-y-auto px-6 py-6">
            <div class="grid grid-cols-3 gap-4 mb-4">
              <!-- 📅 发布与完本卡片 -->
              <div class="col-span-2 border border-[#e8e3dc] rounded-xl p-5 bg-white shadow-sm">
                <div class="flex items-center gap-2 mb-4">
                  <span class="text-lg">📅</span>
                  <span class="text-sm font-bold text-[#6b6560]">发布与完本</span>
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <!-- 发布策略 -->
                  <div class="p-4 bg-[#fefcfb] rounded-xl border border-[#e8e3dc]">
                    <div class="flex items-center justify-between mb-2">
                      <div class="flex items-center gap-2">
                        <span class="text-sm font-semibold text-[#6b6560]">发布计划</span>
                      </div>
                      <button class="text-xs px-2.5 py-1 rounded-lg border border-[#d97706] text-[#d97706] hover:bg-[#fef3c7] hover:border-[#d97706] transition-colors" @click="showPublishDialog = true">✏️ 修改</button>
                    </div>
                    <p class="text-xs text-[#9c9690] leading-relaxed mb-2">设置定时发布计划与平台同步规则</p>
                    <div class="flex items-center gap-2 p-2 bg-white rounded-lg border border-[#f3efe8]">
                      <span class="text-lg">📆</span>
                      <div>
                        <div class="text-sm font-medium text-[#6b6560]">{{ publishConfig.schedule || '暂未设置' }}</div>
                        <div class="text-[10px] text-[#9c9690]">{{ publishConfig.nextRun ? '下次: ' + publishConfig.nextRun : '' }}</div>
                      </div>
                    </div>
                  </div>
                  <!-- 完本计划 -->
                  <div class="p-4 bg-[#fefcfb] rounded-xl border border-[#e8e3dc]">
                    <div class="flex items-center justify-between mb-2">
                      <div class="flex items-center gap-2">
                        <span class="text-sm font-semibold text-[#6b6560]">完本计划</span>
                      </div>
                      <button class="text-xs px-2.5 py-1 rounded-lg border border-[#d97706] text-[#d97706] hover:bg-[#fef3c7] hover:border-[#d97706] transition-colors" @click="openCompletionDialog">✏️ 修改</button>
                    </div>
                    <p class="text-xs text-[#9c9690] leading-relaxed mb-2">设定预定完本时间，把握创作节奏</p>
                    <div class="flex items-center gap-2 p-2 bg-white rounded-lg border border-[#f3efe8]">
                      <span class="text-lg">🎯</span>
                      <div>
                        <div class="text-sm font-medium text-[#6b6560]">{{ completionText }}</div>
                        <div class="text-[10px] text-[#9c9690]">{{ completionHint }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 🖥️ 系统状态卡片 -->
              <div class="border border-[#e8e3dc] rounded-xl p-5 bg-white shadow-sm">
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">🖥️</span>
                    <span class="text-sm font-bold text-[#6b6560]">系统状态</span>
                  </div>
                  <button class="text-[10px] text-[#d97706] hover:underline" @click="showSystemStatusDetail = true">详情</button>
                </div>
                <div class="space-y-3">
                  <!-- AI 服务 -->
                  <div class="flex items-center justify-between p-2 bg-[#faf8f5] rounded-lg">
                    <div class="flex items-center gap-2">
                      <span class="relative flex h-3 w-3">
                        <span v-if="systemStatus.ai === 'healthy'" class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                        <span class="relative inline-flex rounded-full h-3 w-3 shadow-sm" :class="systemStatus.ai === 'healthy' ? 'bg-emerald-500 shadow-emerald-200' : systemStatus.ai === 'warning' ? 'bg-amber-500 shadow-amber-200' : 'bg-red-500 shadow-red-200'"></span>
                      </span>
                      <span class="text-xs text-[#6b6560]">AI 服务</span>
                    </div>
                    <div class="text-right">
                      <div class="text-xs font-semibold" :class="systemStatus.ai === 'healthy' ? 'text-emerald-600' : systemStatus.ai === 'warning' ? 'text-amber-600' : 'text-red-600'">
                        {{ systemStatus.ai === 'healthy' ? '正常' : systemStatus.ai === 'warning' ? '警告' : '异常' }}
                      </div>
                      <div v-if="systemStatus.aiLatency" class="text-[10px] text-[#9c9690]">延迟 {{ systemStatus.aiLatency }}ms</div>
                    </div>
                  </div>
                  <!-- ES 集群 -->
                  <div class="flex items-center justify-between p-2 bg-[#faf8f5] rounded-lg">
                    <div class="flex items-center gap-2">
                      <span class="relative flex h-3 w-3">
                        <span v-if="systemStatus.es === 'healthy'" class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                        <span class="relative inline-flex rounded-full h-3 w-3 shadow-sm" :class="systemStatus.es === 'healthy' ? 'bg-emerald-500 shadow-emerald-200' : systemStatus.es === 'warning' ? 'bg-amber-500 shadow-amber-200' : 'bg-red-500 shadow-red-200'"></span>
                      </span>
                      <span class="text-xs text-[#6b6560]">ES 集群</span>
                    </div>
                    <div class="text-xs font-semibold" :class="systemStatus.es === 'healthy' ? 'text-emerald-600' : systemStatus.es === 'warning' ? 'text-amber-600' : 'text-red-600'">
                      {{ systemStatus.es === 'healthy' ? '绿色' : systemStatus.es === 'warning' ? '警告' : '异常' }}
                    </div>
                  </div>
                  <!-- 数据库 -->
                  <div class="flex items-center justify-between p-2 bg-[#faf8f5] rounded-lg">
                    <div class="flex items-center gap-2">
                      <span class="relative flex h-3 w-3">
                        <span v-if="systemStatus.db === 'healthy'" class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                        <span class="relative inline-flex rounded-full h-3 w-3 shadow-sm" :class="systemStatus.db === 'healthy' ? 'bg-emerald-500 shadow-emerald-200' : systemStatus.db === 'warning' ? 'bg-amber-500 shadow-amber-200' : 'bg-red-500 shadow-red-200'"></span>
                      </span>
                      <span class="text-xs text-[#6b6560]">数据库</span>
                    </div>
                    <div class="text-right">
                      <div class="text-xs font-semibold" :class="systemStatus.db === 'healthy' ? 'text-emerald-600' : systemStatus.db === 'warning' ? 'text-amber-600' : 'text-red-600'">
                        {{ systemStatus.db === 'healthy' ? '在线' : systemStatus.db === 'warning' ? '警告' : '断开' }}
                      </div>
                      <div v-if="systemStatus.dbPoolUsage" class="text-[10px] text-[#9c9690]">连接池 {{ systemStatus.dbPoolUsage }}%</div>
                    </div>
                  </div>
                  <!-- 存储空间 -->
                  <div class="p-2 bg-[#faf8f5] rounded-lg">
                    <div class="flex items-center justify-between mb-2">
                      <div class="flex items-center gap-2">
                        <span class="relative flex h-3 w-3">
                          <span class="relative inline-flex rounded-full h-3 w-3 shadow-sm" :class="storageStatus.colorClass"></span>
                        </span>
                        <span class="text-xs text-[#6b6560]">存储空间</span>
                      </div>
                      <span class="text-xs font-semibold" :class="storageStatus.textColor">{{ storageStatus.usedPercent }}%</span>
                    </div>
                    <div class="w-full h-2.5 bg-[#e8e3dc] rounded-full overflow-hidden mb-1.5 relative">
                      <div class="h-full rounded-full transition-all duration-500" :class="storageStatus.barColor" :style="{ width: storageStatus.usedPercent + '%' }"></div>
                      <div v-if="storageStatus.usedPercent > 50" class="absolute inset-0 flex items-center justify-center">
                        <span class="text-[9px] font-bold text-white drop-shadow-sm" :style="{ textShadow: '0 1px 2px rgba(0,0,0,0.3)' }">{{ storageStatus.usedPercent }}%</span>
                      </div>
                    </div>
                    <div class="flex justify-between text-[10px] text-[#9c9690]">
                      <span>已用 {{ storageStatus.used }}</span>
                      <span>剩余 {{ storageStatus.free }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="grid grid-cols-3 gap-4 mb-4">
              <!-- 💾 自动备份卡片 -->
              <div class="border border-[#e8e3dc] rounded-xl p-5 bg-white shadow-sm">
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">💾</span>
                    <span class="text-sm font-bold text-[#6b6560]">自动备份</span>
                  </div>
                  <button class="text-xs px-2.5 py-1 rounded-lg border border-[#d97706] text-[#d97706] hover:bg-[#fef3c7] hover:border-[#d97706] transition-colors" @click="showBackupDialog = true">✏️ 修改</button>
                </div>
                <p class="text-xs text-[#9c9690] leading-relaxed mb-3">配置云端自动保存与版本历史管理</p>
                <div class="space-y-2">
                  <div class="flex items-center justify-between p-2 bg-[#faf8f5] rounded-lg">
                    <span class="text-xs text-[#9c9690]">备份间隔</span>
                    <span class="text-xs font-semibold text-[#6b6560]">{{ backupConfig.interval || '每5分钟' }}</span>
                  </div>
                  <div class="flex items-center justify-between p-2 bg-[#faf8f5] rounded-lg">
                    <span class="text-xs text-[#9c9690]">保留版本</span>
                    <span class="text-xs font-semibold text-[#6b6560]">{{ backupConfig.keepVersions || 20 }} 个</span>
                  </div>
                  <div class="flex items-center justify-between p-2 bg-[#faf8f5] rounded-lg">
                    <span class="text-xs text-[#9c9690]">云端同步</span>
                    <span class="text-xs font-semibold" :class="backupConfig.cloudSync ? 'text-emerald-600' : 'text-[#9c9690]'">{{ backupConfig.cloudSync ? '✅ 已启用' : '❌ 已禁用' }}</span>
                  </div>
                </div>
              </div>

              <!-- 🤖 AI 模型偏好卡片 -->
              <div class="border border-[#e8e3dc] rounded-xl p-5 bg-white shadow-sm">
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">🤖</span>
                    <span class="text-sm font-bold text-[#6b6560]">AI 模型偏好</span>
                  </div>
                  <button class="text-xs px-2.5 py-1 rounded-lg border border-[#d97706] text-[#d97706] hover:bg-[#fef3c7] hover:border-[#d97706] transition-colors" @click="showAIModelDialog = true">✏️ 修改</button>
                </div>
                <p class="text-xs text-[#9c9690] leading-relaxed mb-3">选择默认 AI 模型及创作风格参数</p>
                <div class="space-y-2">
                  <div class="flex items-center justify-between p-2 bg-[#faf8f5] rounded-lg">
                    <span class="text-xs text-[#9c9690]">默认模型</span>
                    <span class="text-xs font-semibold text-[#6b6560]">{{ aiModelConfig.model || 'DeepSeek v4 Pro' }}</span>
                  </div>
                  <div class="flex items-center justify-between p-2 bg-[#faf8f5] rounded-lg">
                    <span class="text-xs text-[#9c9690]">创作风格</span>
                    <span class="text-xs font-semibold" :class="aiModelConfig.style === 'creative' ? 'text-purple-600' : aiModelConfig.style === 'balanced' ? 'text-blue-600' : 'text-emerald-600'">{{ aiModelConfig.styleLabel || '创意模式' }}</span>
                  </div>
                  <div class="p-2 bg-[#faf8f5] rounded-lg">
                    <div class="flex items-center justify-between mb-1.5">
                      <span class="text-xs text-[#9c9690]">温度参数</span>
                      <span class="text-xs font-semibold text-[#6b6560]">{{ aiModelConfig.temperature || 0.7 }}</span>
                    </div>
                    <div class="w-full h-1.5 bg-[#e8e3dc] rounded-full overflow-hidden">
                      <div class="h-full rounded-full bg-gradient-to-r from-[#7c3aed] to-[#d97706]" :style="{ width: ((aiModelConfig.temperature || 0.7) / 1 * 100) + '%' }"></div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 📊 操作记录卡片 -->
              <div class="border border-[#e8e3dc] rounded-xl p-5 bg-white shadow-sm">
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">📊</span>
                    <span class="text-sm font-bold text-[#6b6560]">最近操作</span>
                  </div>
                </div>
                <div class="space-y-2 max-h-48 overflow-y-auto">
                  <div v-for="log in recentOperationLogs.slice(0, 5)" :key="log.id" class="p-2 bg-[#faf8f5] rounded-lg">
                    <div class="flex items-center justify-between mb-1">
                      <span class="text-xs font-medium text-[#6b6560] truncate">{{ log.action }}</span>
                      <span class="text-[10px] text-[#9c9690]">{{ formatRelativeTime(log.time) }}</span>
                    </div>
                    <div class="text-[10px] text-[#9c9690] truncate">{{ log.detail }}</div>
                  </div>
                  <div v-if="recentOperationLogs.length === 0" class="py-4 text-center text-xs text-[#9c9690]">
                    暂无操作记录
                  </div>
                </div>
              </div>
            </div>

            <!-- 重置按钮 -->
            <div class="flex justify-center mt-6">
              <button class="text-xs px-4 py-2 rounded-lg border border-[#e8e3dc] text-[#9c9690] hover:bg-red-50 hover:border-red-300 hover:text-red-600 transition-colors" @click="confirmResetAllSettings">
                🔄 重置所有设置为默认
              </button>
            </div>
          </div>
        </template>

        <!-- ══ 面板：批量导出 ══ -->
        <template v-else-if="activeTool === 'export'">
          <div class="flex items-center px-4 py-1.5 border-b border-[#e8e3dc] bg-[#faf8f5] flex-shrink-0 gap-3">
            <span class="text-xs font-semibold text-[#6b6560]">📦 批量导出</span>
            <span class="text-[#d4cec6] text-xs">|</span>
            <span class="text-xs text-[#9c9690]">{{ project?.title || '未命名作品' }} · {{ formatNumber(totalChapterWords) }} 字 · {{ chapters.length }} 章</span>
            <div class="flex-1"></div>
            <button
              class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg hover:bg-[#b45309] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="store.exporting"
              @click="handleQuickExport"
            >
              📦 {{ store.exporting ? '导出中...' : '一键全书导出' }}
            </button>
          </div>
          <div class="flex-1 overflow-y-auto px-10 py-8">
            <!-- 导出进度 -->
            <div v-if="store.exporting" class="mb-4 p-3 bg-[#fef3c7] border border-[#d97706] rounded-xl">
              <div class="flex items-center justify-between text-xs mb-2">
                <span class="text-[#92400e] font-semibold">{{ store.exportStatusText || '正在导出...' }}</span>
                <span class="text-[#92400e]">{{ store.exportProgress }}%</span>
              </div>
              <div class="w-full h-2 bg-[#fde68a] rounded-full overflow-hidden">
                <div class="h-full bg-[#d97706] rounded-full transition-all duration-300" :style="{ width: store.exportProgress + '%' }"></div>
              </div>
            </div>
            <!-- 导出格式选择 -->
            <div class="text-xs font-semibold text-[#6b6560] mb-3">📄 选择导出格式</div>
            <div class="grid grid-cols-3 gap-3 mb-4">
              <div v-for="fmt in (store.exportFormats.length ? store.exportFormats : defaultExportFormats)" :key="fmt.key || fmt.id" class="border rounded-xl p-4 text-center cursor-pointer transition-colors hover:border-[#d97706]" :class="selectedFormat === (fmt.key || fmt.id) ? 'border-[#d97706] bg-[#fef3c7]' : 'border-[#e8e3dc]'" @click="selectedFormat = fmt.key || fmt.id">
                <div class="text-2xl mb-1">{{ fmt.icon }}</div>
                <div class="text-sm font-semibold text-[#6b6560]">{{ fmt.label || fmt.name }}</div>
                <div class="text-[10px] text-[#9c9690] mt-0.5">{{ fmt.desc || fmt.description }}</div>
                <div class="text-[10px] text-[#9c9690] mt-0.5">{{ fmt.size || formatFileSize(fmt.size) }}</div>
              </div>
            </div>
            <!-- 导出配置 -->
            <div class="grid grid-cols-2 gap-3 mb-4">
              <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white">
                <div class="text-xs font-semibold text-[#6b6560] mb-3">⚙️ 导出配置</div>
                <div class="space-y-2.5 text-xs">
                  <div class="flex items-center justify-between"><span class="text-[#9c9690]">导出范围</span>
                    <select v-model="exportScope" class="px-2 py-0.5 border border-[#e8e3dc] rounded text-xs bg-white outline-none">
                      <option value="all">全书</option><option value="vol1">第一卷</option><option value="vol2">第二卷</option><option value="custom">自定义章节</option>
                    </select>
                  </div>
                  <div class="flex items-center justify-between"><span class="text-[#9c9690]">包含大纲</span><input type="checkbox" v-model="includeOutline" checked class="accent-[#d97706]"></div>
                  <div class="flex items-center justify-between"><span class="text-[#9c9690]">包含人物档案</span><input type="checkbox" v-model="includeCharacters" class="accent-[#d97706]"></div>
                  <div class="flex items-center justify-between"><span class="text-[#9c9690]">排版模板</span>
                    <select v-model="layoutTemplate" class="px-2 py-0.5 border border-[#e8e3dc] rounded text-xs bg-white outline-none">
                      <option>默认</option><option>简约</option><option>精装</option>
                    </select>
                  </div>
                </div>
                <button
                  class="mt-3 w-full py-1.5 bg-[#d97706] text-white rounded-lg text-xs font-semibold hover:bg-[#b45309] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  :disabled="store.exporting"
                  @click="handleStartExport"
                >
                  {{ store.exporting ? '导出中...' : '开始导出' }}
                </button>
              </div>
              <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white">
                <div class="text-xs font-semibold text-[#6b6560] mb-3">⏱️ 自动备份</div>
                <div class="space-y-2.5 text-xs">
                  <div class="flex items-center justify-between"><span class="text-[#9c9690]">本地自动保存</span><input type="checkbox" v-model="autoSave" checked class="accent-[#d97706]"></div>
                  <div class="flex items-center justify-between"><span class="text-[#9c9690]">保存间隔</span><span class="font-semibold text-[#6b6560]">30分钟</span></div>
                  <div class="flex items-center justify-between"><span class="text-[#9c9690]">云端同步</span><input type="checkbox" v-model="cloudSync" checked class="accent-[#d97706]"></div>
                  <div class="flex items-center justify-between"><span class="text-[#9c9690]">上次备份</span><span class="text-emerald-600 font-semibold">1分钟前</span></div>
                </div>
              </div>
            </div>
            <!-- 导出历史 -->
            <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white">
              <div class="flex items-center justify-between mb-3">
                <span class="text-xs font-semibold text-[#6b6560]">📋 最近导出记录</span>
                <div class="flex gap-2">
                  <button
                    v-if="invalidRecordCount > 0"
                    class="text-xs px-2 py-0.5 text-red-500 hover:bg-red-50 rounded transition-colors"
                    @click="handleClearInvalidRecords"
                  >
                    清理无效记录 ({{ invalidRecordCount }})
                  </button>
                  <button
                    v-if="store.exportHistory.length > 0"
                    class="text-xs px-2 py-0.5 text-[#9c9690] hover:bg-gray-100 rounded transition-colors"
                    @click="handleClearAllRecords"
                  >
                    清空记录
                  </button>
                </div>
              </div>
              <div v-if="store.exportHistory.length === 0" class="text-center py-6 text-xs text-[#9c9690]">
                暂无导出记录
              </div>
              <div v-for="rec in store.exportHistory" :key="rec.id" class="flex items-center justify-between py-2 px-3 bg-[#faf8f5] rounded-lg mb-1.5 group">
                <div class="min-w-0 flex-1">
                  <div class="text-sm font-medium text-[#6b6560] truncate">{{ rec.fileName || rec.name }}</div>
                  <div class="text-xs text-[#9c9690]">
                    {{ formatExportDate(rec.timestamp || rec.date) }}
                    <span v-if="rec.size"> · {{ formatFileSize(rec.size || rec._size) }}</span>
                    <span v-if="rec.chapterRange"> · {{ rec.chapterRange }}</span>
                    <span v-if="rec.includeOutline" class="text-emerald-600 ml-1">📋大纲</span>
                    <span v-if="rec.includeCharacters" class="text-emerald-600 ml-1">👤人物</span>
                  </div>
                  <div v-if="!rec.valid && rec.invalidReason" class="text-xs text-red-500 mt-0.5">{{ rec.invalidReason }}</div>
                </div>
                <div class="flex items-center gap-1.5 flex-shrink-0 ml-2">
                  <button
                    v-if="rec.valid && rec.downloadUrl"
                    class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg hover:bg-[#b45309] transition-colors"
                    @click="handleDownloadRecord(rec)"
                  >
                    下载
                  </button>
                  <button
                    v-else
                    class="text-xs px-2.5 py-1 border border-[#e8e3dc] rounded-lg text-[#9c9690] cursor-not-allowed"
                    disabled
                    :title="rec.invalidReason || '文件不可用'"
                  >
                    过期
                  </button>
                  <button
                    class="text-xs px-1.5 py-1 text-[#9c9690] hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                    title="删除记录"
                    @click="handleDeleteRecord(rec.id)"
                  >
                    ✕
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ══ 面板：上下文引擎 ══ -->
        <template v-else-if="activeTool === 'contextEngine'">
          <div class="flex items-center px-4 py-1.5 border-b border-[#e8e3dc] bg-[#faf8f5] flex-shrink-0 gap-3">
            <span class="text-xs font-semibold text-[#6b6560]">🔍 上下文引擎</span>
            <span class="text-[#d4cec6] text-xs">|</span>
            <span class="text-xs text-[#9c9690]">千问 Embedding 1024维 · ES 8.x 混合检索</span>
            <div class="flex-1"></div>
            <button class="text-xs px-2.5 py-1 rounded-lg transition-colors flex items-center gap-1.5" :class="autoRefreshEnabled ? 'bg-[#fef3c7] text-[#d97706]' : 'text-[#9c9690] hover:bg-[#f3efe8]'" @click="toggleAutoRefresh" :title="autoRefreshEnabled ? '自动刷新已开启' : '点击开启自动刷新'">
              <span :class="autoRefreshEnabled ? 'animate-spin' : ''">🔄</span>
              <span>{{ autoRefreshEnabled ? '自动刷新中' : '自动刷新' }}</span>
            </button>
            <button v-if="store.contextLoading" class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg animate-pulse">加载中...</button>
            <button v-else class="text-xs px-2.5 py-1 bg-[#d97706] text-white rounded-lg hover:bg-[#b45309] transition-colors" @click="refreshContextData">🔄 刷新数据</button>
          </div>
          <div class="flex-1 overflow-y-auto px-6 py-6">

            <!-- ═══ 骨架屏加载状态 ═══ -->
            <template v-if="store.contextLoading && !hasLoadedContextData">
              <div class="grid grid-cols-2 gap-4 mb-4">
                <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white animate-pulse">
                  <div class="h-4 w-24 bg-[#f3efe8] rounded mb-4"></div>
                  <div class="space-y-3">
                    <div class="h-8 bg-[#f3efe8] rounded"></div>
                    <div class="h-8 bg-[#f3efe8] rounded"></div>
                    <div class="h-8 bg-[#f3efe8] rounded"></div>
                    <div class="h-8 bg-[#f3efe8] rounded"></div>
                  </div>
                </div>
                <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white animate-pulse">
                  <div class="h-4 w-24 bg-[#f3efe8] rounded mb-4"></div>
                  <div class="space-y-3">
                    <div class="h-8 bg-[#f3efe8] rounded"></div>
                    <div class="h-8 bg-[#f3efe8] rounded"></div>
                    <div class="h-8 bg-[#f3efe8] rounded"></div>
                    <div class="h-8 bg-[#f3efe8] rounded"></div>
                  </div>
                </div>
              </div>
            </template>

            <!-- ═══ 正常内容 ═══ -->
            <template v-else>
              <!-- 重建索引进度条 -->
              <div v-if="store.contextRebuilding || (store.contextOperationStatus?.type === 'rebuild' && store.contextOperationStatus?.status === 'running')" class="mb-4 p-4 rounded-xl bg-gradient-to-r from-[#fef3c7] to-[#ffedd5] border border-[#fcd34d] shadow-sm">
                <div class="flex items-center justify-between mb-2">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">⚡</span>
                    <span class="text-sm font-semibold text-[#92400e]">{{ store.contextOperationStatus?.message || '索引重建中...' }}</span>
                  </div>
                  <span class="text-sm font-bold text-[#d97706]">{{ store.contextRebuildProgress }}%</span>
                </div>
                <div class="w-full h-2.5 bg-[#fde68a] rounded-full overflow-hidden mb-2">
                  <div class="h-full rounded-full transition-all duration-500 ease-out" :style="{ width: store.contextRebuildProgress + '%', background: 'linear-gradient(90deg, #f59e0b, #ea580c)' }"></div>
                </div>
                <div v-if="store.contextOperationStatus?.stage" class="text-xs text-[#b45309]">{{ store.contextOperationStatus.stage }}</div>
                <button class="mt-2 text-xs px-3 py-1 rounded-lg bg-white/80 text-[#92400e] hover:bg-white transition-colors border border-[#fcd34d]" @click="cancelRebuild">取消操作</button>
              </div>

              <!-- 操作状态提示 -->
              <div v-if="store.contextOperationStatus && store.contextOperationStatus.type !== 'rebuild'" class="mb-4 p-3 rounded-lg text-xs font-medium flex items-center gap-2" :class="{
                'bg-emerald-50 text-emerald-700 border border-emerald-200': store.contextOperationStatus.status === 'success',
                'bg-red-50 text-red-700 border border-red-200': store.contextOperationStatus.status === 'failed',
                'bg-blue-50 text-blue-700 border border-blue-200': store.contextOperationStatus.status === 'running',
                'bg-amber-50 text-amber-700 border border-amber-200': store.contextOperationStatus.status === 'cancelled'
              }">
                <span v-if="store.contextOperationStatus.status === 'success'">✅</span>
                <span v-else-if="store.contextOperationStatus.status === 'failed'">❌</span>
                <span v-else-if="store.contextOperationStatus.status === 'running'">⏳</span>
                <span v-else>⚠️</span>
                {{ store.contextOperationStatus.message }}
              </div>

              <!-- 卡片区域 -->
              <div class="grid grid-cols-2 gap-4 mb-4">
                <!-- 📊 索引概览卡片 -->
                <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white shadow-sm hover:shadow-md transition-shadow">
                  <div class="flex items-center justify-between mb-3">
                    <div class="text-xs font-semibold text-[#6b6560]">📊 索引概览</div>
                    <div v-if="store.contextStats.lastFullIndexTime" class="text-[10px] text-[#9c9690]">最后更新: {{ formatIndexTime(store.contextStats.lastFullIndexTime) }}</div>
                  </div>
                  <div class="grid grid-cols-2 gap-3">
                    <div class="p-3 bg-[#fff7ed] rounded-lg border border-[#fed7aa]">
                      <div class="text-2xl font-bold text-[#ea580c]" style="font-family:var(--font-display)">{{ formatNumber(store.contextStats.totalChunks || 0) }}</div>
                      <div class="text-[10px] text-[#9c9690] mt-0.5">已索引段落块</div>
                    </div>
                    <div class="p-3 bg-[#f0fdfa] rounded-lg border border-[#99f6e4]">
                      <div class="text-2xl font-bold text-[#0d9488]" style="font-family:var(--font-display)">{{ formatNumber(store.contextStats.characterVectors || 0) }}</div>
                      <div class="text-[10px] text-[#9c9690] mt-0.5">人物向量条目</div>
                    </div>
                    <div class="p-3 bg-[#faf5ff] rounded-lg border border-[#e9d5ff]">
                      <div class="text-2xl font-bold text-[#7c3aed]" style="font-family:var(--font-display)">{{ formatNumber(store.contextStats.worldEntries || 0) }}</div>
                      <div class="text-[10px] text-[#9c9690] mt-0.5">世界观设定条</div>
                    </div>
                    <div class="p-3 bg-[#ecfdf5] rounded-lg border border-[#a7f3d0]">
                      <div class="text-2xl font-bold text-[#059669]" style="font-family:var(--font-display)">{{ formatNumber(store.contextStats.outlineNodes || 0) }}</div>
                      <div class="text-[10px] text-[#9c9690] mt-0.5">大纲节点条目</div>
                    </div>
                  </div>
                </div>

                <!-- 🔧 检索配置卡片 -->
                <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white shadow-sm hover:shadow-md transition-shadow">
                  <div class="flex items-center justify-between mb-3">
                    <div class="text-xs font-semibold text-[#6b6560]">🔧 检索配置</div>
                    <button v-if="!configEditing" class="text-[10px] px-2 py-0.5 rounded text-[#d97706] hover:bg-[#fef3c7] transition-colors" @click="startEditConfig">✏️ 编辑</button>
                    <div v-else class="flex gap-1">
                      <button class="text-[10px] px-2 py-0.5 rounded bg-[#d97706] text-white hover:bg-[#b45309] transition-colors" @click="saveConfig">保存</button>
                      <button class="text-[10px] px-2 py-0.5 rounded bg-gray-400 text-white hover:bg-gray-500 transition-colors" @click="cancelEditConfig">取消</button>
                    </div>
                  </div>
                  <div class="space-y-3 text-xs">
                    <div class="flex items-center justify-between">
                      <span class="text-[#9c9690]">自动增量索引</span>
                      <button v-if="!configEditing" class="text-emerald-600 font-semibold text-xs">{{ store.contextConfig.autoIndex ? '✅ 已启用' : '❌ 已禁用' }}</button>
                      <label v-else class="relative inline-flex items-center cursor-pointer">
                        <input type="checkbox" v-model="editConfigForm.autoIndex" class="sr-only peer">
                        <div class="w-9 h-5 bg-[#e8e3dc] peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#d97706]"></div>
                      </label>
                    </div>
                    <div class="flex items-center justify-between">
                      <span class="text-[#9c9690]">段落窗口大小</span>
                      <span v-if="!configEditing" class="font-semibold">{{ store.contextConfig.windowSize || 512 }} tokens</span>
                      <div v-else class="flex items-center gap-1">
                        <input type="number" v-model.number="editConfigForm.windowSize" min="128" max="2048" step="64" class="w-16 px-2 py-1 border border-[#e8e3dc] rounded text-xs text-center focus:border-[#d97706] outline-none">
                        <span class="text-[#9c9690]">tokens</span>
                      </div>
                    </div>
                    <div class="flex items-center justify-between">
                      <span class="text-[#9c9690]">Top-K 检索数</span>
                      <span v-if="!configEditing" class="font-semibold">{{ store.contextConfig.topK || 10 }}</span>
                      <div v-else class="flex items-center gap-2">
                        <input type="range" v-model.number="editConfigForm.topK" min="3" max="50" class="w-20 accent-[#d97706]">
                        <span class="font-semibold w-6 text-right">{{ editConfigForm.topK }}</span>
                      </div>
                    </div>
                    <div class="flex items-center justify-between">
                      <span class="text-[#9c9690]">混合检索权重</span>
                      <span v-if="!configEditing" class="font-semibold">{{ Math.round((store.contextConfig.hybridWeights?.semantic || 0.7) * 100) }}% 语义 + {{ Math.round((store.contextConfig.hybridWeights?.bm25 || 0.3) * 100) }}% BM25</span>
                      <div v-else class="flex items-center gap-2">
                        <input type="range" v-model.number="editConfigForm.semanticWeight" min="0" max="100" step="5" class="w-20 accent-[#d97706]">
                        <span class="font-semibold w-12 text-right">{{ editConfigForm.semanticWeight }}% 语义</span>
                      </div>
                    </div>
                    <button v-if="configEditing" class="w-full py-1.5 rounded-lg text-xs text-[#9c9690] hover:bg-[#f3efe8] transition-colors border border-[#e8e3dc]" @click="resetConfigToDefault">🔄 恢复默认</button>
                  </div>
                </div>
              </div>

              <!-- 索引活动日志卡片 -->
              <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white shadow-sm mb-4">
                <div class="flex items-center justify-between mb-3">
                  <div class="text-xs font-semibold text-[#6b6560]">🔄 最近索引活动 <span class="text-[#9c9690] font-normal">({{ store.contextActivities.length }} 条)</span></div>
                  <button v-if="store.contextActivities.length > 0" class="text-xs px-2.5 py-1 rounded-lg text-[#d97706] hover:bg-[#fef3c7] transition-colors" @click="showAllLogsPanel = true">📋 查看全部日志</button>
                </div>
                <div v-if="store.contextActivities.length === 0" class="py-8 text-center">
                  <div class="text-3xl mb-2">📭</div>
                  <div class="text-sm text-[#9c9690]">暂无索引活动记录</div>
                  <div class="text-xs text-[#c4bdb5] mt-1">系统将在有新内容时自动创建索引</div>
                </div>
                <div v-else class="space-y-1 max-h-48 overflow-y-auto">
                  <div v-for="act in store.contextActivities.slice(0, 5)" :key="act.id" class="flex items-center gap-3 py-2 px-3 rounded-lg hover:bg-[#faf8f5] cursor-pointer transition-colors" @click="act.chapterId && goToChapter(act.chapterId)">
                    <span class="w-2.5 h-2.5 rounded-full flex-shrink-0" :class="{
                      'bg-emerald-400': act.status === 'done' || act.status === 'completed',
                      'bg-amber-400': act.status === 'running' || act.status === 'pending',
                      'bg-red-400': act.status === 'failed' || act.status === 'error',
                      'bg-blue-400': act.status === 'cancelled'
                    }"></span>
                    <div class="flex-1 min-w-0">
                      <div class="text-sm font-medium text-[#6b6560] truncate">{{ act.title }}</div>
                      <div class="text-xs text-[#9c9690] truncate">{{ act.desc || act.description || '' }}</div>
                    </div>
                    <span class="text-xs text-[#9c9690] flex-shrink-0">{{ act.time || act.createTime ? formatRelativeTime(act.time || act.createTime) : '' }}</span>
                  </div>
                </div>
              </div>

              <!-- 索引维护与监控 -->
              <div class="grid grid-cols-3 gap-4 mb-4">
                <!-- 索引维护 -->
                <div class="col-span-2 border border-[#e8e3dc] rounded-xl p-4 bg-white shadow-sm">
                  <div class="text-xs font-semibold text-[#6b6560] mb-3">🔧 索引维护</div>
                  <div class="grid grid-cols-4 gap-3 mb-4">
                    <div class="p-3 bg-[#faf8f5] rounded-lg">
                      <div class="text-[10px] text-[#9c9690] mb-1">最后全量索引</div>
                      <div class="text-sm font-semibold text-[#6b6560]">{{ store.contextStats.lastFullIndexTime ? formatDate(store.contextStats.lastFullIndexTime) : '从未' }}</div>
                    </div>
                    <div class="p-3 bg-[#faf8f5] rounded-lg">
                      <div class="text-[10px] text-[#9c9690] mb-1">索引大小</div>
                      <div class="text-sm font-semibold text-[#6b6560]">{{ formatFileSize(store.contextStats.indexSize || 0) }}</div>
                    </div>
                    <div class="p-3 bg-[#faf8f5] rounded-lg">
                      <div class="text-[10px] text-[#9c9690] mb-1">索引健康状态</div>
                      <button class="text-sm font-semibold text-emerald-600 hover:text-emerald-700 flex items-center gap-1" @click="showHealthCheck">✅ 正常 <span class="text-[10px] text-[#9c9690]">详情</span></button>
                    </div>
                    <div class="p-3 bg-[#faf8f5] rounded-lg">
                      <div class="text-[10px] text-[#9c9690] mb-1">平均检索耗时</div>
                      <div class="text-sm font-semibold text-[#6b6560]">{{ store.contextStats.avgQueryTime ? store.contextStats.avgQueryTime + 'ms' : '--' }}</div>
                    </div>
                  </div>
                  <div class="flex gap-2">
                    <button class="flex-1 py-2 rounded-lg text-xs font-medium bg-gradient-to-r from-[#f59e0b] to-[#ea580c] text-white hover:from-[#d97706] hover:to-[#c2410c] transition-all shadow-sm hover:shadow transform hover:-translate-y-0.5" @click="confirmRebuild">🔄 重建索引</button>
                    <button class="flex-1 py-2 rounded-lg text-xs font-medium border border-[#d97706] text-[#d97706] hover:bg-[#fef3c7] transition-colors" :disabled="store.contextLoading" @click="handleIncrementalIndex">➕ 增量索引</button>
                    <button class="flex-1 py-2 rounded-lg text-xs font-medium border border-[#e8e3dc] text-[#6b6560] hover:bg-[#f3efe8] transition-colors" :disabled="store.contextLoading" @click="handleCleanup">🗑️ 清理索引</button>
                  </div>
                </div>

                <!-- 索引大小趋势 -->
                <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white shadow-sm">
                  <div class="flex items-center justify-between mb-2">
                    <div class="text-xs font-semibold text-[#6b6560]">📈 大小趋势(7天)</div>
                  </div>
                  <div ref="sizeTrendChartRef" class="w-full h-24"></div>
                  <div v-if="!store.contextSizeTrend || store.contextSizeTrend.length === 0" class="h-24 flex items-center justify-center text-xs text-[#9c9690]">暂无趋势数据</div>
                </div>
              </div>

              <!-- 底部监控指标 -->
              <div class="border border-[#e8e3dc] rounded-xl p-4 bg-white shadow-sm">
                <div class="flex items-center justify-between mb-3">
                  <div class="text-xs font-semibold text-[#6b6560]">📊 监控指标</div>
                  <button class="text-xs px-2.5 py-1 rounded-lg border border-[#e8e3dc] text-[#6b6560] hover:bg-[#f3efe8] transition-colors flex items-center gap-1" @click="handleExportReport">
                    <span>📥</span> 导出报告
                  </button>
                </div>
                <div class="grid grid-cols-5 gap-3">
                  <div class="text-center p-2 bg-[#faf8f5] rounded-lg">
                    <div class="text-lg font-bold text-[#d97706]" style="font-family:var(--font-display)">{{ formatNumber(store.contextStats.totalChunks || 0) }}</div>
                    <div class="text-[10px] text-[#9c9690]">段落块总数</div>
                  </div>
                  <div class="text-center p-2 bg-[#faf8f5] rounded-lg">
                    <div class="text-lg font-bold text-[#0d9488]" style="font-family:var(--font-display)">{{ formatNumber(store.contextStats.characterVectors || 0) }}</div>
                    <div class="text-[10px] text-[#9c9690]">人物向量</div>
                  </div>
                  <div class="text-center p-2 bg-[#faf8f5] rounded-lg">
                    <div class="text-lg font-bold text-[#7c3aed]" style="font-family:var(--font-display)">{{ formatNumber(store.contextStats.worldEntries || 0) }}</div>
                    <div class="text-[10px] text-[#9c9690]">世界观条目</div>
                  </div>
                  <div class="text-center p-2 bg-[#faf8f5] rounded-lg">
                    <div class="text-lg font-bold text-[#059669]" style="font-family:var(--font-display)">{{ store.contextStats.avgQueryTime ? store.contextStats.avgQueryTime + 'ms' : '--' }}</div>
                    <div class="text-[10px] text-[#9c9690]">平均检索耗时</div>
                  </div>
                  <div class="text-center p-2 bg-[#faf8f5] rounded-lg">
                    <div class="text-lg font-bold text-[#3b82f6]" style="font-family:var(--font-display)">{{ store.contextStats.updateFrequency || '--' }}</div>
                    <div class="text-[10px] text-[#9c9690]">更新频率(次/天)</div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </template>

    <!-- ═══ 上下文引擎：重建索引确认对话框 ═══ -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showRebuildConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showRebuildConfirm = false">
          <div class="bg-white rounded-2xl w-[420px] max-w-[90vw] shadow-2xl border border-[#e8e3dc] overflow-hidden">
            <div class="px-5 py-4 bg-gradient-to-r from-[#fef3c7] to-[#ffedd5] border-b border-[#fcd34d]">
              <div class="flex items-center gap-3">
                <span class="text-2xl">⚠️</span>
                <div>
                  <h3 class="text-sm font-bold text-[#92400e]">确认重建索引</h3>
                  <p class="text-xs text-[#b45309] mt-0.5">此操作可能耗时较长且会中断服务</p>
                </div>
              </div>
            </div>
            <div class="p-5">
              <div class="text-sm text-[#6b6560] leading-relaxed mb-4">
                重建索引将重新处理所有章节内容，生成新的向量索引。
                <ul class="mt-2 space-y-1 text-xs text-[#9c9690]">
                  <li>• 预计耗时：5-15 分钟（视章节数量而定）</li>
                  <li>• 索引重建期间检索功能暂时不可用</li>
                  <li>• 已有的索引数据会被替换</li>
                </ul>
              </div>
              <label class="flex items-center gap-2 text-xs text-[#6b6560] mb-4 cursor-pointer">
                <input type="checkbox" v-model="rebuildConfirmChecked" class="w-4 h-4 accent-[#d97706] rounded">
                <span>我已知悉上述风险，确认继续执行</span>
              </label>
            </div>
            <div class="flex items-center gap-2 p-4 bg-[#faf8f5] border-t border-[#e8e3dc]">
              <button class="flex-1 px-4 py-2.5 text-sm font-semibold bg-gradient-to-r from-[#f59e0b] to-[#ea580c] text-white rounded-xl hover:from-[#d97706] hover:to-[#c2410c] transition-all disabled:opacity-50 disabled:cursor-not-allowed" :disabled="!rebuildConfirmChecked || store.contextRebuilding" @click="executeRebuild">
                {{ store.contextRebuilding ? '重建中...' : '⚡ 开始重建' }}
              </button>
              <button class="px-4 py-2.5 text-sm font-semibold text-[#6b6560] bg-white border border-[#e8e3dc] rounded-xl hover:bg-[#f5f3f0] transition-colors" @click="showRebuildConfirm = false; rebuildConfirmChecked = false">
                取消
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 上下文引擎：健康检查详情弹窗 ═══ -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showHealthCheckModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showHealthCheckModal = false">
          <div class="bg-white rounded-2xl w-[560px] max-w-[90vw] max-h-[80vh] overflow-y-auto shadow-2xl border border-[#e8e3dc]">
            <div class="flex items-center justify-between px-5 py-4 border-b border-[#e8e3dc]">
              <div class="flex items-center gap-2">
                <span class="text-lg">🏥</span>
                <span class="text-sm font-bold text-[#1a1a2e]">索引健康检查报告</span>
              </div>
              <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="showHealthCheckModal = false">✕</button>
            </div>
            <div class="p-5">
              <div v-if="store.contextLoading && !store.contextHealthCheck" class="py-8 text-center">
                <div class="animate-spin text-2xl mb-2">⏳</div>
                <div class="text-sm text-[#9c9690]">正在加载健康检查数据...</div>
              </div>
              <div v-else-if="store.contextHealthCheck" class="space-y-4">
                <div class="flex items-center justify-between p-3 rounded-lg" :class="store.contextHealthCheck.overall === 'healthy' ? 'bg-emerald-50' : store.contextHealthCheck.overall === 'warning' ? 'bg-amber-50' : 'bg-red-50'">
                  <div class="flex items-center gap-2">
                    <span class="text-xl">{{ store.contextHealthCheck.overall === 'healthy' ? '✅' : store.contextHealthCheck.overall === 'warning' ? '⚠️' : '❌' }}</span>
                    <span class="text-sm font-semibold" :class="store.contextHealthCheck.overall === 'healthy' ? 'text-emerald-700' : store.contextHealthCheck.overall === 'warning' ? 'text-amber-700' : 'text-red-700'">
                      {{ store.contextHealthCheck.overall === 'healthy' ? '健康' : store.contextHealthCheck.overall === 'warning' ? '存在警告' : '存在异常' }}
                    </span>
                  </div>
                  <span class="text-xs text-[#9c9690]">{{ formatDate(store.contextHealthCheck.checkTime) }}</span>
                </div>
                <div class="space-y-2">
                  <div v-for="item in store.contextHealthCheck.items || []" :key="item.name" class="p-3 rounded-lg border" :class="{
                    'bg-emerald-50 border-emerald-200': item.status === 'pass' || item.status === 'healthy',
                    'bg-amber-50 border-amber-200': item.status === 'warning',
                    'bg-red-50 border-red-200': item.status === 'error' || item.status === 'failed'
                  }">
                    <div class="flex items-center justify-between mb-1">
                      <div class="flex items-center gap-2">
                        <span class="text-sm font-medium text-[#6b6560]">{{ item.label || item.name }}</span>
                      </div>
                      <span class="text-xs font-medium" :class="{
                        'text-emerald-600': item.status === 'pass' || item.status === 'healthy',
                        'text-amber-600': item.status === 'warning',
                        'text-red-600': item.status === 'error' || item.status === 'failed'
                      }">
                        {{ item.status === 'pass' || item.status === 'healthy' ? '✅ 正常' : item.status === 'warning' ? '⚠️ 警告' : '❌ 异常' }}
                      </span>
                    </div>
                    <div v-if="item.message" class="text-xs text-[#9c9690]">{{ item.message }}</div>
                    <div v-if="item.suggestion" class="mt-2 text-xs text-blue-600 bg-blue-50 rounded px-2 py-1">{{ item.suggestion }}</div>
                  </div>
                </div>
                <div v-if="!store.contextHealthCheck.items || store.contextHealthCheck.items.length === 0" class="py-6 text-center text-sm text-[#9c9690]">
                  暂无详细检查项目
                </div>
              </div>
              <div v-else class="py-8 text-center">
                <div class="text-2xl mb-2">📭</div>
                <div class="text-sm text-[#9c9690]">暂无健康检查数据</div>
                <button class="mt-3 text-xs px-3 py-1.5 rounded-lg bg-[#d97706] text-white hover:bg-[#b45309] transition-colors" @click="loadHealthCheck">点击加载</button>
              </div>
            </div>
            <div class="flex justify-end gap-2 p-4 bg-[#faf8f5] border-t border-[#e8e3dc]">
              <button class="px-4 py-2 text-xs rounded-lg border border-[#e8e3dc] text-[#6b6560] hover:bg-white transition-colors" @click="showHealthCheckModal = false">关闭</button>
              <button class="px-4 py-2 text-xs rounded-lg bg-[#d97706] text-white hover:bg-[#b45309] transition-colors" @click="loadHealthCheck">🔄 重新检查</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 上下文引擎：全部日志侧滑面板 ═══ -->
    <Teleport to="body">
      <Transition name="slide-fade">
        <div v-if="showAllLogsPanel" class="fixed inset-0 z-50 flex">
          <div class="fixed inset-0 bg-black/20" @click="showAllLogsPanel = false"></div>
          <div class="ml-auto w-[480px] max-w-[90vw] h-full bg-white shadow-2xl flex flex-col border-l border-[#e8e3dc]">
            <div class="flex items-center justify-between px-5 py-4 border-b border-[#e8e3dc] bg-[#faf8f5]">
              <div class="flex items-center gap-2">
                <span class="text-lg">📋</span>
                <span class="text-sm font-bold text-[#1a1a2e]">索引活动日志</span>
                <span class="text-xs text-[#9c9690]">({{ store.contextActivities.length }} 条)</span>
              </div>
              <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="showAllLogsPanel = false">✕</button>
            </div>
            <div class="flex-1 overflow-y-auto p-4">
              <div v-if="store.contextActivities.length === 0" class="py-12 text-center">
                <div class="text-3xl mb-2">📭</div>
                <div class="text-sm text-[#9c9690]">暂无索引活动记录</div>
              </div>
              <div v-else class="space-y-2">
                <div v-for="(act, idx) in store.contextActivities" :key="act.id || idx" class="p-3 rounded-lg border border-[#e8e3dc] hover:border-[#d97706] hover:bg-[#fef3c7]/30 transition-all cursor-pointer" @click="act.chapterId && goToChapter(act.chapterId)">
                  <div class="flex items-center gap-2 mb-1.5">
                    <span class="w-2.5 h-2.5 rounded-full flex-shrink-0" :class="{
                      'bg-emerald-400': act.status === 'done' || act.status === 'completed',
                      'bg-amber-400': act.status === 'running' || act.status === 'pending',
                      'bg-red-400': act.status === 'failed' || act.status === 'error',
                      'bg-blue-400': act.status === 'cancelled'
                    }"></span>
                    <span class="text-sm font-semibold text-[#6b6560] flex-1 truncate">{{ act.title }}</span>
                    <span class="text-xs text-[#9c9690]">{{ act.time || act.createTime ? formatRelativeTime(act.time || act.createTime) : '' }}</span>
                  </div>
                  <div class="text-xs text-[#9c9690] leading-relaxed">{{ act.desc || act.description || '无描述' }}</div>
                  <div v-if="act.chunksAffected" class="mt-1.5 text-[10px] text-[#d97706]">影响 {{ act.chunksAffected }} 个段落块</div>
                </div>
              </div>
            </div>
            <div class="p-4 bg-[#faf8f5] border-t border-[#e8e3dc]">
              <button class="w-full py-2 text-xs rounded-lg border border-[#e8e3dc] text-[#6b6560] hover:bg-white transition-colors" @click="loadMoreLogs">🔄 加载更多</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

        <!-- ══ 面板：个人设置 ══ -->
        <template v-if="activeTool === 'settings'">
          <div class="flex-1 overflow-y-auto">
            <SettingsView />
          </div>
        </template>


      </main>
    </div>

    <!-- 章节管理Toast -->
    <Teleport to="body">
      <Transition name="toast-fade">
        <div v-if="chapterToast" class="fixed top-6 right-6 z-50 px-4 py-2.5 rounded-lg shadow-md text-xs font-medium flex items-center gap-2"
          :class="chapterToastType === 'success' ? 'bg-[#ecfdf5] text-[#065f46] border border-[#a7f3d0]' : 'bg-[#fef2f2] text-[#991b1b] border border-[#fecaca]'">
          <span>{{ chapterToastType === 'success' ? '✓' : '✕' }}</span>
          {{ chapterToast }}
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 策略与设置：发布计划编辑弹窗 ═══ -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showPublishDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showPublishDialog = false">
          <div class="bg-white rounded-2xl w-[460px] max-w-[90vw] shadow-2xl border border-[#e8e3dc] overflow-hidden">
            <div class="px-5 py-4 border-b border-[#e8e3dc] bg-gradient-to-r from-[#fefcfb] to-[#fef9f5]">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="text-lg">📅</span>
                  <h3 class="text-sm font-bold text-[#1a1a2e]">编辑发布计划</h3>
                </div>
                <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="showPublishDialog = false">✕</button>
              </div>
            </div>
            <div class="p-5 space-y-4">
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">发布周期</label>
                <select v-model="editPublishForm.frequency" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors bg-white">
                  <option value="daily">每日</option>
                  <option value="twice-weekly">每周两次（周二、五）</option>
                  <option value="weekly">每周一次</option>
                  <option value="custom">自定义</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">发布时间</label>
                <input type="time" v-model="editPublishForm.time" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors" />
              </div>
              <div v-if="editPublishForm.frequency === 'custom'">
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">自定义周期（天数）</label>
                <input type="number" v-model.number="editPublishForm.customDays" min="1" max="30" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors" />
              </div>
              <div v-if="editPublishForm.nextRun" class="p-3 bg-[#fef3c7] rounded-xl">
                <div class="text-xs text-[#92400e]">下次发布时间</div>
                <div class="text-sm font-semibold text-[#b45309]">{{ editPublishForm.nextRun }}</div>
              </div>
            </div>
            <div class="flex items-center gap-2 p-4 bg-[#faf8f5] border-t border-[#e8e3dc]">
              <button class="flex-1 px-4 py-2.5 text-sm font-semibold bg-[#d97706] text-white rounded-xl hover:bg-[#b45309] transition-colors disabled:opacity-50 disabled:cursor-not-allowed" :disabled="settingsSaving" @click="savePublishConfig">
                {{ settingsSaving ? '保存中...' : '💾 保存' }}
              </button>
              <button class="px-4 py-2.5 text-sm font-semibold text-[#6b6560] bg-white border border-[#e8e3dc] rounded-xl hover:bg-[#f5f3f0] transition-colors" @click="showPublishDialog = false">取消</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 策略与设置：完本计划编辑弹窗 ═══ -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showCompletionDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showCompletionDialog = false">
          <div class="bg-white rounded-2xl w-[460px] max-w-[90vw] shadow-2xl border border-[#e8e3dc] overflow-hidden">
            <div class="px-5 py-4 border-b border-[#e8e3dc] bg-gradient-to-r from-[#fefcfb] to-[#fef9f5]">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="text-lg">🎯</span>
                  <h3 class="text-sm font-bold text-[#1a1a2e]">完本计划设置</h3>
                </div>
                <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="showCompletionDialog = false">✕</button>
              </div>
            </div>
            <div class="p-5 space-y-4">
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">起始时间</label>
                <input type="date" v-model="editCompletionForm.startingTime" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors" />
                <p class="text-[10px] text-[#9c9690] mt-1">作品故事开始的时间点，不同作品相互隔离</p>
              </div>
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">预定完本时间</label>
                <input type="date" v-model="editCompletionForm.plannedCompletionDate" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors" />
                <p class="text-[10px] text-[#9c9690] mt-1">目标完结日期，可随时修改</p>
              </div>
            </div>
            <div class="flex items-center gap-2 p-4 bg-[#faf8f5] border-t border-[#e8e3dc]">
              <button class="flex-1 px-4 py-2.5 text-sm font-semibold bg-[#d97706] text-white rounded-xl hover:bg-[#b45309] transition-colors disabled:opacity-50 disabled:cursor-not-allowed" :disabled="settingsSaving" @click="saveCompletionConfig">
                {{ settingsSaving ? '保存中...' : '💾 保存' }}
              </button>
              <button class="px-4 py-2.5 text-sm font-semibold text-[#6b6560] bg-white border border-[#e8e3dc] rounded-xl hover:bg-[#f5f3f0] transition-colors" @click="showCompletionDialog = false">取消</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 策略与设置：自动备份编辑弹窗 ═══ -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showBackupDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showBackupDialog = false">
          <div class="bg-white rounded-2xl w-[460px] max-w-[90vw] shadow-2xl border border-[#e8e3dc] overflow-hidden">
            <div class="px-5 py-4 border-b border-[#e8e3dc] bg-gradient-to-r from-[#fefcfb] to-[#fef9f5]">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="text-lg">💾</span>
                  <h3 class="text-sm font-bold text-[#1a1a2e]">自动备份设置</h3>
                </div>
                <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="showBackupDialog = false">✕</button>
              </div>
            </div>
            <div class="p-5 space-y-4">
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">备份间隔</label>
                <select v-model="editBackupForm.interval" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors bg-white">
                  <option value="5">每 5 分钟</option>
                  <option value="15">每 15 分钟</option>
                  <option value="30">每 30 分钟</option>
                  <option value="60">每 1 小时</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">保留版本数量</label>
                <input type="number" v-model.number="editBackupForm.keepVersions" min="5" max="100" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors" />
              </div>
              <div>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" v-model="editBackupForm.cloudSync" class="w-4 h-4 accent-[#d97706] rounded">
                  <span class="text-xs text-[#6b6560]">启用云端同步</span>
                </label>
              </div>
              <div v-if="editBackupForm.cloudSync">
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">云服务商</label>
                <select v-model="editBackupForm.cloudProvider" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors bg-white">
                  <option value="aliyun">阿里云 OSS</option>
                  <option value="aws">AWS S3</option>
                  <option value="qcloud">腾讯云 COS</option>
                  <option value="custom">自定义</option>
                </select>
              </div>
            </div>
            <div class="flex items-center gap-2 p-4 bg-[#faf8f5] border-t border-[#e8e3dc]">
              <button class="flex-1 px-4 py-2.5 text-sm font-semibold bg-[#d97706] text-white rounded-xl hover:bg-[#b45309] transition-colors disabled:opacity-50 disabled:cursor-not-allowed" :disabled="settingsSaving" @click="saveBackupConfig">
                {{ settingsSaving ? '保存中...' : '💾 保存' }}
              </button>
              <button class="px-4 py-2.5 text-sm font-semibold text-[#6b6560] bg-white border border-[#e8e3dc] rounded-xl hover:bg-[#f5f3f0] transition-colors" @click="showBackupDialog = false">取消</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 策略与设置：AI模型偏好编辑弹窗 ═══ -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showAIModelDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showAIModelDialog = false">
          <div class="bg-white rounded-2xl w-[480px] max-w-[90vw] shadow-2xl border border-[#e8e3dc] overflow-hidden">
            <div class="px-5 py-4 border-b border-[#e8e3dc] bg-gradient-to-r from-[#fefcfb] to-[#fef9f5]">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="text-lg">🤖</span>
                  <h3 class="text-sm font-bold text-[#1a1a2e]">AI 模型与风格设置</h3>
                </div>
                <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="showAIModelDialog = false">✕</button>
              </div>
            </div>
            <div class="p-5 space-y-4">
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">默认模型</label>
                <select v-model="editAIModelForm.model" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors bg-white">
                  <option value="DeepSeek v4 Pro">DeepSeek v4 Pro</option>
                  <option value="GPT-4">GPT-4</option>
                  <option value="Claude 3">Claude 3</option>
                  <option value="DeepSeek v3">DeepSeek v3</option>
                  <option value="Qwen 2.5">Qwen 2.5</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">创作风格</label>
                <select v-model="editAIModelForm.style" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors bg-white">
                  <option value="creative">🎨 创意模式 - 更自由的发挥</option>
                  <option value="balanced">⚖️ 平衡模式 - 创意与精准兼顾</option>
                  <option value="precise">🎯 精确模式 - 严谨规范的输出</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">温度参数 (Temperature)</label>
                <div class="flex items-center gap-3">
                  <input type="range" v-model.number="editAIModelForm.temperature" min="0" max="1" step="0.1" class="flex-1 accent-[#d97706]" />
                  <span class="text-sm font-bold w-10 text-right" :style="{ color: `hsl(${30 + (1 - editAIModelForm.temperature) * 30}, 80%, 45%)` }">{{ editAIModelForm.temperature }}</span>
                </div>
                <div class="flex justify-between text-[10px] text-[#9c9690] mt-1">
                  <span>精确</span>
                  <span>创意</span>
                </div>
              </div>
              <div>
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">最大输出长度</label>
                <select v-model="editAIModelForm.maxTokens" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors bg-white">
                  <option value="1024">1024 tokens</option>
                  <option value="2048">2048 tokens</option>
                  <option value="4096">4096 tokens</option>
                  <option value="8192">8192 tokens</option>
                </select>
              </div>
            </div>
            <div class="flex items-center gap-2 p-4 bg-[#faf8f5] border-t border-[#e8e3dc]">
              <button class="flex-1 px-4 py-2.5 text-sm font-semibold bg-[#d97706] text-white rounded-xl hover:bg-[#b45309] transition-colors disabled:opacity-50 disabled:cursor-not-allowed" :disabled="settingsSaving" @click="saveAIModelConfig">
                {{ settingsSaving ? '保存中...' : '💾 保存' }}
              </button>
              <button class="px-4 py-2.5 text-sm font-semibold text-[#6b6560] bg-white border border-[#e8e3dc] rounded-xl hover:bg-[#f5f3f0] transition-colors" @click="showAIModelDialog = false">取消</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 策略与设置：系统状态详情弹窗 ═══ -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showSystemStatusDetail" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showSystemStatusDetail = false">
          <div class="bg-white rounded-2xl w-[520px] max-w-[90vw] max-h-[80vh] overflow-y-auto shadow-2xl border border-[#e8e3dc]">
            <div class="flex items-center justify-between px-5 py-4 border-b border-[#e8e3dc]">
              <div class="flex items-center gap-2">
                <span class="text-lg">🖥️</span>
                <span class="text-sm font-bold text-[#1a1a2e]">系统状态详情</span>
              </div>
              <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="showSystemStatusDetail = false">✕</button>
            </div>
            <div class="p-5 space-y-4">
              <div v-for="service in systemStatusDetails" :key="service.name" class="p-4 rounded-xl border" :class="{
                'bg-emerald-50 border-emerald-200': service.status === 'healthy',
                'bg-amber-50 border-amber-200': service.status === 'warning',
                'bg-red-50 border-red-200': service.status === 'error'
              }">
                <div class="flex items-center justify-between mb-2">
                  <div class="flex items-center gap-2">
                    <span class="w-3 h-3 rounded-full" :class="service.status === 'healthy' ? 'bg-emerald-500' : service.status === 'warning' ? 'bg-amber-500' : 'bg-red-500'"></span>
                    <span class="text-sm font-semibold text-[#6b6560]">{{ service.name }}</span>
                  </div>
                  <span class="text-xs font-medium" :class="service.status === 'healthy' ? 'text-emerald-600' : service.status === 'warning' ? 'text-amber-600' : 'text-red-600'">
                    {{ service.status === 'healthy' ? '正常' : service.status === 'warning' ? '警告' : '异常' }}
                  </span>
                </div>
                <div class="text-xs text-[#9c9690] space-y-1">
                  <div v-if="service.latency">响应延迟: {{ service.latency }}</div>
                  <div v-if="service.uptime">运行时长: {{ service.uptime }}</div>
                  <div v-if="service.message">{{ service.message }}</div>
                </div>
              </div>
            </div>
            <div class="flex justify-end p-4 bg-[#faf8f5] border-t border-[#e8e3dc]">
              <button class="px-4 py-2 text-xs rounded-lg border border-[#e8e3dc] text-[#6b6560] hover:bg-white transition-colors" @click="showSystemStatusDetail = false">关闭</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 哨兵告警详情弹窗 ═══ -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="sentinelDetailVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="closeAlertDetail">
          <div class="bg-white rounded-2xl w-[520px] max-w-[90vw] max-h-[80vh] overflow-y-auto shadow-xl border border-[#e8e3dc]">
            <div class="flex items-center justify-between p-4 border-b border-[#e8e3dc]">
              <div class="flex items-center gap-2">
                <span class="text-lg">{{ severityIcons[detailAlert?.severity] || '🔵' }}</span>
                <span class="text-sm font-bold text-[#1a1a2e]">{{ detailAlert?.title }}</span>
              </div>
              <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="closeAlertDetail">✕</button>
            </div>
            <div class="p-4 space-y-4">
              <!-- 严重程度和类型 -->
              <div class="flex items-center gap-2">
                <span class="text-[10px] px-2 py-0.5 rounded-full font-semibold" :class="severityTagClass(detailAlert?.severity)">{{ severityLabels[detailAlert?.severity] || detailAlert?.severity }}</span>
                <span class="text-[10px] px-2 py-0.5 rounded font-medium" :class="typeTagClass(detailAlert?.type)">{{ typeLabels[detailAlert?.type] || detailAlert?.type }}</span>
                <span class="text-[10px] text-[#9c9690] ml-auto">{{ formatSentinelTime(detailAlert?.createTime) }}</span>
              </div>

              <!-- 完整描述 -->
              <div>
                <div class="text-xs font-semibold text-[#6b6560] mb-1">📝 问题描述</div>
                <p class="text-sm text-[#6b6560] leading-relaxed bg-[#faf8f5] rounded-lg p-3">{{ detailAlert?.description || '暂无详细描述' }}</p>
              </div>

              <!-- AI 修复建议 -->
              <div v-if="detailAlert?.suggestion">
                <div class="text-xs font-semibold text-[#6b6560] mb-1">🤖 AI 修复建议</div>
                <div class="text-sm text-[#0d9488] leading-relaxed bg-emerald-50 border border-emerald-100 rounded-lg p-3">{{ detailAlert.suggestion }}</div>
              </div>

              <!-- 关联章节 -->
              <div v-if="detailAlert?.relatedChapter">
                <div class="text-xs font-semibold text-[#6b6560] mb-1">📖 关联章节</div>
                <div class="text-xs text-[#d97706] bg-[#fef3c7] rounded-lg px-3 py-2">{{ detailAlert.relatedChapter }}</div>
              </div>

              <!-- 关联内容 -->
              <div v-if="detailAlert?.relatedContent">
                <div class="text-xs font-semibold text-[#6b6560] mb-1">📄 关联内容</div>
                <div class="text-xs text-[#6b6560] leading-relaxed bg-[#faf8f5] border border-[#e8e3dc] rounded-lg p-3 max-h-32 overflow-y-auto">{{ detailAlert.relatedContent }}</div>
              </div>
            </div>
            <!-- 操作按钮 -->
            <div class="flex justify-end gap-2 p-4 border-t border-[#e8e3dc]">
              <button class="px-4 py-2 text-xs rounded-lg border border-[#e8e3dc] text-[#6b6560] hover:bg-[#faf8f5] transition-colors" @click="closeAlertDetail">关闭</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 新增弹窗 -->
    <Teleport to="body">
      <div v-if="addDialog.show" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="closeAddDialog">
        <div class="bg-white rounded-2xl p-6 w-[400px] max-w-[90vw] shadow-xl border border-[#e8e3dc]">
          <div class="flex items-center justify-between mb-4">
            <span class="text-sm font-bold text-[#1a1a2e]">{{ addDialog.title }}</span>
            <button class="text-[#9c9690] hover:text-[#6b6560] text-lg leading-none" @click="closeAddDialog">✕</button>
          </div>
          <div v-for="f in addDialog.fields" :key="f.key" class="mb-3">
            <label class="block text-xs font-semibold text-[#6b6560] mb-1">{{ f.label }}</label>
            <textarea v-if="f.type === 'textarea'" v-model="f.value.value" :placeholder="f.placeholder" rows="3" class="w-full px-3 py-2 border border-[#e8e3dc] rounded-lg text-sm outline-none focus:border-[#d97706] resize-y"></textarea>
            <select v-else-if="f.type === 'select'" v-model="f.value.value" class="w-full px-3 py-2 border border-[#e8e3dc] rounded-lg text-sm outline-none focus:border-[#d97706] bg-white appearance-none cursor-pointer" style="background-image:url('data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 width=%2712%27 height=%278%27 viewBox=%270 0 12 8%27%3E%3Cpath d=%27M1 1l5 5 5-5%27 stroke=%27%2394a3b8%27 stroke-width=%271.5%27 fill=%27none%27 stroke-linecap=%27round%27/%3E%3C/svg%3E');background-repeat:no-repeat;background-position:right 12px center">
              <option value="" disabled>{{ f.placeholder || '请选择' }}</option>
              <option v-for="opt in f.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
            <input v-else v-model="f.value.value" :placeholder="f.placeholder" class="w-full px-3 py-2 border border-[#e8e3dc] rounded-lg text-sm outline-none focus:border-[#d97706]" @keyup.enter="addDialog.onConfirm()" />
          </div>
          <div class="flex justify-end gap-2 mt-4">
            <button class="px-4 py-2 text-xs border border-[#e8e3dc] rounded-lg hover:bg-[#faf8f5]" @click="closeAddDialog">取消</button>
            <button class="px-4 py-2 text-xs bg-[#d97706] text-white rounded-lg hover:bg-[#b45309]" @click="addDialog.onConfirm()">确认新增</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 新建角色弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showCharacterDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-[3px]" @click.self="showCharacterDialog = false; resetCharacterForm()">
          <div class="bg-white rounded-2xl w-[580px] max-w-[95vw] shadow-2xl border border-[#e8e3dc] overflow-hidden">
            <!-- 标题区 -->
            <div class="px-6 py-4 border-b border-[#f0ece6] bg-gradient-to-r from-[#fefcfb] to-[#fef9f5]">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="text-lg">📝</span>
                  <h2 class="text-base font-bold text-[#1a1a2e]">新建角色</h2>
                </div>
                <button class="w-7 h-7 flex items-center justify-center text-[#9c9690] hover:text-[#6b6560] hover:bg-[#f0ece6] rounded-lg transition-all text-sm" @click="showCharacterDialog = false; resetCharacterForm()">✕</button>
              </div>
            </div>

            <!-- 表单区 -->
            <div class="px-6 py-5 max-h-[70vh] overflow-y-auto">
              <!-- 第1组：角色名称 + 角色定位 -->
              <div class="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">角色名称 <span class="text-red-500">*</span></label>
                  <input v-model="characterForm.name" placeholder="如：铁无双" class="w-full px-3 py-2.5 border border-[#e2e8f0] rounded-lg text-sm outline-none focus:border-[#d97706] focus:ring-2 focus:ring-[#d97706]/20 transition-all" @keyup.enter="handleCreateCharacter()" />
                </div>
                <div>
                  <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">角色定位</label>
                  <select v-model="characterForm.role" class="w-full px-3 py-2.5 border border-[#e2e8f0] rounded-lg text-sm outline-none focus:border-[#d97706] focus:ring-2 focus:ring-[#d97706]/20 bg-white appearance-none cursor-pointer transition-all" style="background-image:url('data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 width=%2712%27 height=%278%27 viewBox=%270 0 12 8%27%3E%3Cpath d=%27M1 1l5 5 5-5%27 stroke=%27%2394a3b8%27 stroke-width=%271.5%27 fill=%27none%27 stroke-linecap=%27round%27/%3E%3C/svg%3E');background-repeat:no-repeat;background-position:right 12px center">
                    <option value="" disabled>请选择定位</option>
                    <option v-for="opt in CHARACTER_CATEGORIES" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                  </select>
                </div>
              </div>

              <!-- 第2组：人物类型 + 年龄 + 头像颜色 -->
              <div class="grid grid-cols-3 gap-4 mb-4">
                <div>
                  <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">人物类型</label>
                  <input v-model="characterForm.persona" placeholder="如：热血、冷静" class="w-full px-3 py-2.5 border border-[#e2e8f0] rounded-lg text-sm outline-none focus:border-[#d97706] focus:ring-2 focus:ring-[#d97706]/20 transition-all" />
                </div>
                <div>
                  <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">年龄</label>
                  <input v-model="characterForm.age" placeholder="如：28" class="w-full px-3 py-2.5 border border-[#e2e8f0] rounded-lg text-sm outline-none focus:border-[#d97706] focus:ring-2 focus:ring-[#d97706]/20 transition-all" />
                </div>
                <div>
                  <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">头像颜色</label>
                  <div class="flex items-center gap-2">
                    <input type="color" v-model="characterForm.avatarColor" class="w-10 h-10 rounded-lg border border-[#e2e8f0] cursor-pointer" />
                    <span class="text-xs text-[#9c9690]">{{ characterForm.avatarColor }}</span>
                  </div>
                </div>
              </div>

              <!-- 第3组：人物描述 -->
              <div class="mb-4">
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">人物描述</label>
                <textarea v-model="characterForm.description" placeholder="身高、体型、外貌特征..." rows="3" class="w-full px-3 py-2.5 border border-[#e2e8f0] rounded-lg text-sm outline-none focus:border-[#d97706] focus:ring-2 focus:ring-[#d97706]/20 resize-y transition-all"></textarea>
              </div>

              <!-- 第4组：性格特征 -->
              <div class="mb-4">
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">性格特征</label>
                <textarea v-model="characterForm.personality" placeholder="性格关键词，如：外冷内热、重情重义、亦正亦邪..." rows="3" class="w-full px-3 py-2.5 border border-[#e2e8f0] rounded-lg text-sm outline-none focus:border-[#d97706] focus:ring-2 focus:ring-[#d97706]/20 resize-y transition-all"></textarea>
              </div>

              <!-- 第5组：关系描述 -->
              <div class="mb-4">
                <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">关系描述</label>
                <input v-model="characterForm.relationships" placeholder="与主角的关系，如：青梅竹马、亦师亦友、宿敌..." class="w-full px-3 py-2.5 border border-[#e2e8f0] rounded-lg text-sm outline-none focus:border-[#d97706] focus:ring-2 focus:ring-[#d97706]/20 transition-all" />
              </div>

              <!-- 第6组：弧光进度 -->
              <div class="mb-2">
                <div class="flex items-center justify-between mb-2">
                  <label class="text-xs font-semibold text-[#6b6560]">弧光进度</label>
                  <span class="text-sm font-bold" :style="{ color: `hsl(${340 - (characterForm.arcProgress / 100) * 200}, 85%, 45%)`, fontFamily: 'var(--font-display)' }">{{ characterForm.arcProgress }}%</span>
                </div>
                <div class="relative">
                  <div class="w-full h-2.5 bg-[#e2e8f0] rounded-full overflow-hidden">
                    <div class="h-full rounded-full transition-all duration-300 ease-out" :style="{ width: characterForm.arcProgress + '%', background: `linear-gradient(90deg, #ff6b35, hsl(${340 - (characterForm.arcProgress / 100) * 200}, 85%, 55%))`, boxShadow: `0 0 12px hsla(${340 - (characterForm.arcProgress / 100) * 200}, 85%, 55%, 0.5)` }"></div>
                  </div>
                  <input type="range" v-model.number="characterForm.arcProgress" min="0" max="100" class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                </div>
                <div class="flex justify-between text-[10px] text-[#b8b0a8] mt-1">
                  <span>起点：沉睡</span>
                  <span>终点：救世主</span>
                </div>
              </div>
            </div>

            <!-- 底部按钮 -->
            <div class="px-6 py-4 border-t border-[#f0ece6] bg-[#faf8f5] flex justify-end gap-3">
              <button class="px-5 py-2.5 text-sm border border-[#e2e8f0] rounded-lg text-[#6b6560] hover:bg-[#f0ece6] hover:border-[#d4cec6] transition-all active:scale-95" @click="showCharacterDialog = false; resetCharacterForm()">取消</button>
              <button class="px-5 py-2.5 text-sm bg-gradient-to-r from-[#d97706] to-[#b45309] text-white rounded-lg hover:from-[#b45309] hover:to-[#92400e] shadow-md hover:shadow-lg transition-all active:scale-95" @click="handleCreateCharacter()">✅ 确认新增</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 人物详情弹窗 -->
    <Teleport to="body">
      <div v-if="showCharacterDetail && selectedCharacter" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showCharacterDetail = false">
        <div class="bg-white rounded-2xl w-[480px] max-w-[90vw] shadow-xl border border-[#e8e3dc] overflow-hidden">
          <!-- 头部 -->
          <div class="flex items-center gap-4 p-5 border-b border-[#f0ece6]">
            <div class="w-14 h-14 rounded-full flex items-center justify-center text-white text-xl font-bold flex-shrink-0" :style="{ background: selectedCharacter.color }">{{ selectedCharacter.avatar }}</div>
            <div class="flex-1 min-w-0">
              <h3 class="text-lg font-bold text-[#1a1a2e] truncate">{{ selectedCharacter.name }}</h3>
              <span class="inline-block mt-1 text-xs px-2.5 py-0.5 rounded-full" :class="{
                'bg-amber-100 text-amber-800': selectedCharacter.role?.includes('主角'),
                'bg-blue-100 text-blue-800': selectedCharacter.role?.includes('配角'),
                'bg-red-100 text-red-800': selectedCharacter.role?.includes('反派'),
                'bg-gray-100 text-gray-600': selectedCharacter.role?.includes('次要')
              }">{{ selectedCharacter.role }}</span>
            </div>
            <button class="w-8 h-8 flex items-center justify-center text-[#9c9690] hover:text-[#6b6560] hover:bg-[#f5f3f0] rounded-lg transition-colors text-lg" @click="showCharacterDetail = false">✕</button>
          </div>

          <!-- 角色简介 -->
          <div class="p-5 border-b border-[#f0ece6]">
            <h4 class="text-xs font-semibold text-[#b8b0a8] uppercase tracking-wide mb-2">📝 角色简介</h4>
            <p class="text-sm text-[#6b6560] leading-relaxed">{{ selectedCharacter.bio || '暂无简介' }}</p>
          </div>

          <!-- 标签 -->
          <div v-if="selectedCharacter.tags && selectedCharacter.tags.length" class="px-5 py-3 border-b border-[#f0ece6]">
            <h4 class="text-xs font-semibold text-[#b8b0a8] uppercase tracking-wide mb-2">🏷️ 标签</h4>
            <div class="flex gap-1.5 flex-wrap">
              <span v-for="tag in selectedCharacter.tags" :key="tag" class="px-2 py-1 rounded-lg bg-[#f3efe8] text-xs text-[#6b6560]">{{ tag }}</span>
            </div>
          </div>

          <!-- 弧光进度 -->
          <div class="p-5 border-b border-[#f0ece6]">
            <h4 class="text-xs font-semibold text-[#b8b0a8] uppercase tracking-wide mb-3">🎯 弧光进度</h4>
            <div class="flex items-center gap-3">
              <div class="flex-1 h-2.5 bg-[#f0ece6] rounded-full overflow-hidden">
                <div class="h-full rounded-full transition-all duration-500" :style="{ width: (selectedCharacter.arc || 0) + '%', background: `linear-gradient(90deg, #ff6b35, hsl(${340 - ((selectedCharacter.arc || 0) / 100) * 200}, 85%, 55%))`, boxShadow: `0 0 12px hsla(${340 - ((selectedCharacter.arc || 0) / 100) * 200}, 85%, 55%, 0.5)` }"></div>
              </div>
              <span class="text-sm font-bold text-[#6b6560] w-12 text-right">{{ selectedCharacter.arc || 0 }}%</span>
            </div>
            <div class="flex justify-between mt-2 text-xs text-[#b8b0a8]">
              <span>起点：沉睡</span>
              <span>终点：救世主</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-center gap-2 p-4 bg-[#faf8f5]">
            <button class="flex-1 px-4 py-2.5 text-sm font-semibold bg-[#d97706] text-white rounded-xl hover:bg-[#b45309] transition-colors flex items-center justify-center gap-1.5" @click="openEditCharacter">
              ✏️ 编辑
            </button>
            <button class="px-4 py-2.5 text-sm font-semibold text-[#6b6560] bg-white border border-[#e8e3dc] rounded-xl hover:bg-[#f5f3f0] transition-colors" @click="showCharacterDetail = false">
              关闭
            </button>
            <button class="px-4 py-2.5 text-sm font-semibold text-white bg-rose-500 rounded-xl hover:bg-rose-600 transition-colors" @click="handleDeleteCharacter">
              🗑️
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 编辑角色弹窗 -->
    <Teleport to="body">
      <div v-if="showEditCharacter && editingCharacter" class="fixed inset-0 z-[60] flex items-center justify-center bg-black/30 backdrop-blur-[2px]" @click.self="showEditCharacter = false">
        <div class="bg-white rounded-2xl w-[480px] max-w-[90vw] shadow-xl border border-[#e8e3dc] overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-[#f0ece6]">
            <h3 class="text-lg font-bold text-[#1a1a2e]">✏️ 编辑角色</h3>
            <button class="w-8 h-8 flex items-center justify-center text-[#9c9690] hover:text-[#6b6560] hover:bg-[#f5f3f0] rounded-lg transition-colors text-lg" @click="showEditCharacter = false">✕</button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">角色名称 <span class="text-rose-500">*</span></label>
              <input v-model="editForm.name" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors" placeholder="如：铁无双" />
            </div>
            <div>
              <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">角色定位</label>
              <select v-model="editForm.role" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors bg-white">
                <option value="protagonist">⭐ 主角</option>
                <option value="supporting">👤 配角</option>
                <option value="antagonist">🔥 反派</option>
                <option value="minor">📖 次要角色</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">角色简介</label>
              <textarea v-model="editForm.description" rows="4" class="w-full px-3 py-2.5 border border-[#e8e3dc] rounded-xl text-sm outline-none focus:border-[#d97706] transition-colors resize-y" placeholder="性格特征、背景故事..."></textarea>
            </div>
                        <div>
              <label class="block text-xs font-semibold text-[#6b6560] mb-1.5">弧光进度</label>
              <div class="flex items-center gap-2 mb-2">
                <input type="range" v-model.number="editForm.arc" min="0" max="100" class="flex-1 accent-[#d97706]" />
                <span class="text-sm font-bold w-10 text-right" :style="{ color: `hsl(${340 - (editForm.arc / 100) * 200}, 85%, 45%)` }">{{ editForm.arc }}%</span>
              </div>
              <div class="w-full h-2 bg-[#e2e8f0] rounded-full overflow-hidden">
                <div class="h-full rounded-full transition-all duration-300 ease-out" :style="{ width: editForm.arc + '%', background: `linear-gradient(90deg, #ff6b35, hsl(${340 - (editForm.arc / 100) * 200}, 85%, 55%))`, boxShadow: `0 0 10px hsla(${340 - (editForm.arc / 100) * 200}, 85%, 55%, 0.4)` }"></div>
              </div>
              <div class="flex justify-between text-[10px] text-[#b8b0a8] mt-1">
                <span>起点：沉睡</span>
                <span>终点：救世主</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-2 p-4 bg-[#faf8f5] border-t border-[#f0ece6]">
            <button class="flex-1 px-4 py-2.5 text-sm font-semibold text-white bg-[#d97706] rounded-xl hover:bg-[#b45309] transition-colors" @click="saveCharacterEdit">
              💾 保存
            </button>
            <button class="px-4 py-2.5 text-sm font-semibold text-[#6b6560] bg-white border border-[#e8e3dc] rounded-xl hover:bg-[#f5f3f0] transition-colors" @click="showEditCharacter = false">
              取消
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import OutlinePanel from '@/components/outline/OutlinePanel.vue'
import { useRoute, useRouter } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { plotApi, inspirationApi, characterApi, aiApi, sentinelApi } from '@/api'
import { CHARACTER_CATEGORIES, PLOT_CATEGORIES, getCategoryLabel, getCategoryOptions } from '@/config/categories'
import { formatNumber, formatFileSize } from '@/utils/format'
import Editor from '@/views/Editor.vue'
import Dashboard from '@/views/Dashboard.vue'
import Chapters from '@/views/Chapters.vue'
import PlotEngine from '@/views/PlotEngine.vue'
import InspirationSection from '@/components/inspiration/InspirationSection.vue'
import WorldBuilding from '@/views/WorldBuilding.vue'
import AiChat from '@/views/AiChat.vue'
import SettingsView from '@/views/SettingsView.vue'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const store = useNovelStore()
const project = computed(() => store.currentProject)
const chapters = computed(() => store.chapters)
const characters = computed(() => store.characters)

// ─── 侧边栏工具导航 ───
function getToolActiveClass(toolKey) {
  return activeTool.value === toolKey
    ? 'bg-[#fef3c7] text-[#92400e] font-semibold'
    : 'text-[#6b6560] hover:bg-[#f3efe8]'
}

function handleToolClick(toolKey) {
  activeTool.value = toolKey
}

// ─── 状态 ───
const statusText = computed(() => { const s = project.value?.status; return (s === '连载中' || s === 'ongoing') ? '连载中' : '草稿' })
const statusClass = computed(() => { const s = project.value?.status; return (s === '连载中' || s === 'ongoing') ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700' })

// ─── 工具导航（大分类，16个工具） ───
const toolKeys = ['dashboard','aiWrite','outline','chapters','world','characters','plot','inspiration','aiChat','export','contextEngine','sentinel','strategy','settings']
const activeTool = ref(toolKeys.includes(route.query.tool) ? route.query.tool : 'aiWrite')

// 双向同步：activeTool ↔ URL ?tool=xxx
watch(activeTool, (val) => {
  if (route.query.tool !== val) {
    router.replace({ query: { ...route.query, tool: val } })
  }
  // 切到哨兵面板时自动加载数据
  if (val === 'sentinel' && store.currentProjectId) {
    store.fetchSentinelStats(store.currentProjectId)
    store.fetchSentinelAlerts(store.currentProjectId, 'all', 'all')
    store.fetchSentinelLogs(store.currentProjectId)
  }
  // 切到导出面板时加载导出格式和历史
  if (val === 'export') {
    loadExportData()
  }
})
watch(() => route.query.tool, (val) => {
  if (val && toolKeys.includes(val) && activeTool.value !== val) {
    activeTool.value = val
  }
})
const toolGroups = [
  { label: '概览', items: [{ key: 'dashboard', icon: '📊', label: '写作仪表盘' }] },
  { label: '写作辅助', items: [{ key: 'aiWrite', icon: '📝', label: '写作编辑器' },{ key: 'outline', icon: '🌳', label: '智能大纲' },{ key: 'chapters', icon: '📑', label: '章节管理' }] },
  { label: '设定构筑', items: [{ key: 'world', icon: '🌍', label: '世界观构建' },{ key: 'characters', icon: '👤', label: '人物工坊' }] },
  { label: '情节与灵感', items: [{ key: 'plot', icon: '🎯', label: '情节引擎' },{ key: 'inspiration', icon: '💡', label: '灵感素材' }] },
  { label: '工具与分析', items: [{ key: 'aiChat', icon: '💬', label: 'AI 对话助手' },{ key: 'export', icon: '📦', label: '批量导出' },{ key: 'contextEngine', icon: '🔍', label: '上下文引擎' }] },
  { label: '策略与系统', items: [{ key: 'sentinel', icon: '🔔', label: '智能哨兵' },{ key: 'strategy', icon: '⚙️', label: '策略与设置' },{ key: 'settings', icon: '👤', label: '个人设置' }] }
]

// ─── 编辑器（状态已迁移至 useNovelStore + Editor.vue） ───
const selectedChapterId = computed(() => store.currentChapterId)
const selectedChapter = computed(() => store.currentChapter)
function selectChapter(ch) {
  // 切换前先静默保存当前章节（不创建历史版本）
  const pid = store.currentProjectId
  if (pid && store.currentChapterId) {
    store.saveCurrentChapter(pid, true).catch(() => {})
  }
  store.selectChapter(ch)
  activeTool.value = 'aiWrite'
}
const totalChapterWords = computed(() => chapters.value.reduce((s, c) => s + (c.wordCount || 0), 0))

// ─── 章节管理面板操作 ───
const openChapterMenuId = ref(null)
function toggleChapterMenu(chapterId) {
  openChapterMenuId.value = openChapterMenuId.value === chapterId ? null : chapterId
}

async function handleChapterCreate() {
  const pid = store.currentProjectId
  if (!pid) return
  // 弹出命名弹窗，不直接跳转
  promptAdd('chapter', '新建章节', [
    { key: 'title', label: '章节标题', placeholder: '如：第1章 · 星辰坠落', value: `第${chapters.value.length + 1}章` }
  ], async (data) => {
    const title = data.title?.trim() || `第${chapters.value.length + 1}章`
    try {
      const ch = await store.createChapter(pid, { title, content: '', status: 'draft' })
      // 仅刷新列表，不跳转编辑器
      await store.fetchChapters(pid)
      showChapterToast('新章节已创建', 'success')
    } catch (e) {
      showChapterToast('创建失败：' + (e.message || '网络错误'), 'error')
    }
  })
}

async function handleToggleStatus(ch) {
  const pid = store.currentProjectId
  if (!pid) return
  try {
    await store.toggleChapterStatus(pid, ch.id)
    const newStatus = ch.status === 'published' ? 'draft' : 'published'
    showChapterToast(newStatus === 'published' ? '章节已发布' : '已转为草稿', 'success')
  } catch (e) {
    showChapterToast('状态切换失败', 'error')
  }
}

async function handleDuplicateChapter(chapterId) {
  const pid = store.currentProjectId
  if (!pid) return
  try {
    const newCh = await store.duplicateChapter(pid, chapterId)
    if (newCh) showChapterToast('章节已复制', 'success')
  } catch (e) {
    showChapterToast('复制失败：' + (e.message || '网络错误'), 'error')
  }
}

async function handleDeleteChapterFromPanel(ch) {
  const pid = store.currentProjectId
  if (!pid || !confirm(`确定要删除「${ch.title || '未命名'}」吗？此操作不可恢复。`)) return
  try {
    await store.deleteChapter(pid, ch.id)
    showChapterToast('章节已删除', 'success')
  } catch (e) {
    showChapterToast('删除失败：' + (e.message || '网络错误'), 'error')
  }
}

// 章节面板Toast
const chapterToast = ref('')
const chapterToastType = ref('success')
let chapterToastTimer = null
function showChapterToast(msg, type = 'success') {
  chapterToast.value = msg
  chapterToastType.value = type
  if (chapterToastTimer) clearTimeout(chapterToastTimer)
  chapterToastTimer = setTimeout(() => { chapterToast.value = '' }, 2000)
}

// 草稿章节计数
const draftCount = computed(() =>
  chapters.value.filter(c => c.status === 'draft').length
)

// 清理所有草稿
async function handleCleanDrafts() {
  const pid = store.currentProjectId
  if (!pid) return
  if (!confirm(`确定要删除全部 ${draftCount.value} 个草稿章节吗？此操作不可恢复。`)) return
  try {
    const deleted = await store.deleteChaptersBatch(pid, c => c.status === 'draft')
    showChapterToast(`已清理 ${deleted} 个草稿章节`, 'success')
  } catch (e) {
    showChapterToast('清理失败：' + (e.message || '网络错误'), 'error')
  }
}

// 点击面板外关闭菜单
if (typeof document !== 'undefined') {
  document.addEventListener('click', () => { openChapterMenuId.value = null })
}

// ─── 人物 ───
const charTab = ref('卡片视图')
const characterList = computed(() => characters.value.map((c, i) => ({ id: c.id, name: c.name || '未命名', role: getCategoryLabel(CHARACTER_CATEGORIES, c.role) || c.role || '👤 配角', avatar: (c.name || '?').charAt(0), color: c.avatarColor || '#d97706', bio: c.description || '暂无简介', tags: (c.tags || '').split(',').filter(Boolean).slice(0, 3), arc: c.arc ?? c.arcProgress ?? [75, 60, 30, 50, 85, 20][i] ?? 0 })))

// ─── 人物详情弹窗 ───
const showCharacterDetail = ref(false)
const selectedCharacter = ref(null)

function viewCharacter(char) {
  selectedCharacter.value = char
  showCharacterDetail.value = true
}

async function handleDeleteCharacter() {
  if (!selectedCharacter.value || !store.currentProjectId) return
  if (!confirm(`确定要删除角色「${selectedCharacter.value.name}」吗？此操作不可恢复。`)) return
  try {
    await characterApi.delete(store.currentProjectId, selectedCharacter.value.id)
    await store.refreshCharacters(store.currentProjectId)
    showChapterToast('角色已删除', 'success')
    showCharacterDetail.value = false
  } catch (e) {
    showChapterToast('删除失败：' + (e.message || '未知错误'), 'error')
  }
}

// ─── 编辑角色 ───
const showEditCharacter = ref(false)
const editingCharacter = ref(null)
const editForm = reactive({
  name: '',
  role: 'supporting',
  description: '',
  arc: 0
})

function openEditCharacter() {
  if (!selectedCharacter.value) return
  editingCharacter.value = selectedCharacter.value
  editForm.name = selectedCharacter.value.name || ''
  editForm.role = selectedCharacter.value.role?.includes('主角') ? 'protagonist' :
                   selectedCharacter.value.role?.includes('反派') ? 'antagonist' :
                   selectedCharacter.value.role?.includes('次要') ? 'minor' : 'supporting'
  editForm.description = selectedCharacter.value.bio || ''
  editForm.arc = selectedCharacter.value.arc || 0
  showEditCharacter.value = true
}

async function saveCharacterEdit() {
  if (!editingCharacter.value || !store.currentProjectId) return
  if (!editForm.name.trim()) {
    showChapterToast('角色名称不能为空', 'error')
    return
  }
  try {
    const roleMap = { protagonist: '⭐ 主角', supporting: '👤 配角', antagonist: '🔥 反派', minor: '📖 次要角色' }
    await characterApi.update(store.currentProjectId, editingCharacter.value.id, {
      name: editForm.name.trim(),
      role: editForm.role,
      description: editForm.description.trim(),
      arcProgress: editForm.arc,
      tags: ''
    })
    await store.refreshCharacters(store.currentProjectId)
    showChapterToast('角色已更新', 'success')
    showEditCharacter.value = false
    showCharacterDetail.value = false
  } catch (e) {
    showChapterToast('更新失败：' + (e.message || '未知错误'), 'error')
  }
}

// ─── 新建角色弹窗 ───
const showCharacterDialog = ref(false)
const characterForm = reactive({
  name: '',
  role: 'supporting',
  description: '',
  persona: '',
  age: '',
  avatarColor: '#d97706',
  personality: '',
  relationships: '',
  arcProgress: 0
})

const arcProgressStyle = computed(() => {
  const p = characterForm.arcProgress
  const hue = 340 - (p / 100) * 200
  const color = `hsl(${hue}, 85%, 55%)`
  return {
    background: `linear-gradient(to right, #ff6b35 0%, ${color} ${p}%, #e2e8f0 ${p}%, #e2e8f0 100%)`
  }
})

function resetCharacterForm() {
  characterForm.name = ''
  characterForm.role = 'supporting'
  characterForm.description = ''
  characterForm.persona = ''
  characterForm.age = ''
  characterForm.avatarColor = '#d97706'
  characterForm.personality = ''
  characterForm.relationships = ''
  characterForm.arcProgress = 0
}

async function handleCreateCharacter() {
  if (!store.currentProjectId) return
  if (!characterForm.name.trim()) {
    showChapterToast('请输入角色名称', 'error')
    return
  }
  try {
    await store.createCharacter(store.currentProjectId, {
      name: characterForm.name.trim(),
      role: characterForm.role,
      description: characterForm.description.trim(),
      persona: characterForm.persona.trim(),
      age: characterForm.age.trim(),
      avatarColor: characterForm.avatarColor,
      personality: characterForm.personality.trim(),
      relationships: characterForm.relationships.trim(),
      arc: characterForm.arcProgress
    })
    showChapterToast('角色创建成功', 'success')
    showCharacterDialog.value = false
    resetCharacterForm()
    await store.refreshCharacters(store.currentProjectId)
  } catch (e) {
    console.error('创建角色失败:', e)
    showChapterToast('创建失败，请重试', 'error')
  }
}

// ─── 情节引擎 ───
const plotTab = ref('情节线')
const expandedPlot = ref({})
function togglePlot(id) { expandedPlot.value[id] = !expandedPlot.value[id] }
const workspacePlotThreads = computed(() => (store.plotThreads || []).map(t => ({
  id: t.id, title: t.name, desc: t.description || '', type: getCategoryLabel(PLOT_CATEGORIES, t.type) || t.type || '🌿 支线', color: t.color || '#d97706', progress: t.progress || 0, nodes: 5, chapters: (t.chapters || '').split(',').length
})))
// 伏笔数据从后端加载（novel_foreshadowing 表）
const foreshadowingList = ref([])
const foreshadowStats = computed(() => {
  const total = foreshadowingList.value.length
  const recovered = foreshadowingList.value.filter(f => f.status === 'recovered' || f.status === 'resolved').length
  return { total, recovered, rate: total > 0 ? Math.round(recovered / total * 100) : 0 }
})
// 节奏张力从章节数据推算
const tensionChartRef = ref(null)
const expandedTension = ref({})
let tensionChartInstance = null

function toggleTensionDetail(ch) {
  expandedTension.value[ch] = !expandedTension.value[ch]
}

const tensionCurve = computed(() => {
  const chs = store.chapters || []
  if (chs.length === 0) return []
  return chs.slice(0, 14).map((c, i) => ({
    ch: `CH${i + 1}`,
    title: c.title || '',
    words: c.wordCount || 0,
    value: Math.min(10, Math.max(1, Math.floor((c.wordCount || 500) / 600) + ((i % 3 === 0 ? 2 : i % 3 === 1 ? 0 : -1)) + 3))
  }))
})

const tensionStats = computed(() => {
  const data = tensionCurve.value
  if (data.length === 0) return { avg: '-', bestChapter: '-', highCount: 0 }
  const sum = data.reduce((s, d) => s + d.value, 0)
  const avg = (sum / data.length).toFixed(1)
  const best = data.reduce((best, d) => (d.value >= 4 && d.value <= 6 && d.value < best.value) ? d : best, data[0])
  const consecutive = []
  let current = []
  for (const d of data) {
    if (d.value >= 7) { current.push(d) }
    else { if (current.length >= 3) consecutive.push([...current]); current = [] }
  }
  if (current.length >= 3) consecutive.push([...current])
  const highCount = consecutive.reduce((s, g) => s + g.length, 0)
  return { avg, bestChapter: best?.ch || '-', highCount: consecutive.length > 0 ? Math.max(...consecutive.map(g => g.length)) : 0 }
})

const tensionDiagnosis = computed(() => {
  const data = tensionCurve.value
  if (data.length === 0) return '暂无章节数据，请先创建章节内容'
  const high = data.filter(d => d.value >= 7)
  const low = data.filter(d => d.value < 3)
  // 查找连续高张力
  let maxConsecutive = 0, cur = 0, lastHighIdx = -1
  for (let i = 0; i < data.length; i++) {
    if (data[i].value >= 7) { cur++; if (cur > maxConsecutive) { maxConsecutive = cur; lastHighIdx = i } }
    else { cur = 0 }
  }
  if (maxConsecutive >= 3 && lastHighIdx < data.length - 1) {
    return `第${lastHighIdx - maxConsecutive + 2}-${lastHighIdx + 1}章连续${maxConsecutive}章节奏偏高，建议在第${lastHighIdx + 2}章插入舒缓章节（日常/感情戏），避免读者疲劳。`
  }
  if (high.length >= data.length * 0.5) {
    return `整体节奏偏快（${high.length}/${data.length}章高张力），建议穿插舒缓章节平衡阅读体验。`
  }
  if (low.length >= data.length * 0.4) {
    return `节奏偏缓（${low.length}章低张力），建议增加冲突或悬念提升叙事张力。`
  }
  const avgVal = (data.reduce((s, d) => s + d.value, 0) / data.length).toFixed(1)
  return `节奏均衡（均值${avgVal}），高低张力交替合理，阅读体验良好。`
})

function renderTensionChart() {
  if (!tensionChartRef.value || tensionCurve.value.length === 0) return
  if (tensionChartInstance) { tensionChartInstance.dispose(); tensionChartInstance = null }
  tensionChartInstance = echarts.init(tensionChartRef.value)
  const data = tensionCurve.value
  const colors = data.map(d => d.value >= 7 ? '#ef4444' : d.value >= 4 ? '#d97706' : '#0d9488')
  tensionChartInstance.setOption({
    backgroundColor: 'transparent',
    grid: { top: 20, right: 30, bottom: 30, left: 40 },
    tooltip: { trigger: 'axis', formatter: (p) => { const d = p[0]; return `<strong>${d.name}</strong><br/>张力值：${d.value}<br/>${data[d.dataIndex]?.words?.toLocaleString() || 0}字${data[d.dataIndex]?.title ? '<br/>' + data[d.dataIndex].title : ''}` } },
    xAxis: { type: 'category', data: data.map(d => d.ch), axisLabel: { fontSize: 10, color: '#9c9690' }, axisLine: { lineStyle: { color: '#e8e3dc' } } },
    yAxis: { type: 'value', min: 0, max: 10, interval: 2, axisLabel: { fontSize: 10, color: '#9c9690' }, splitLine: { lineStyle: { color: '#f3efe8' } } },
    series: [{
      type: 'bar', data: data.map((d, i) => ({ value: d.value, itemStyle: { color: colors[i], borderRadius: [4, 4, 0, 0] } })),
      barWidth: '60%',
      markLine: { silent: true, data: [{ yAxis: 7, lineStyle: { color: '#ef4444', type: 'dashed' } }, { yAxis: 3, lineStyle: { color: '#0d9488', type: 'dashed' } }] }
    }]
  }, true)
  tensionChartInstance.resize()
}

// 节奏分析切换时渲染 ECharts
watch([plotTab, tensionCurve], () => {
  if (plotTab.value === '节奏分析') {
    nextTick(() => renderTensionChart())
  }
})

// ───（灵感模块已迁移至 InspirationSection 组件）───

// ─── 哨兵 ───
const lastCheckTime = ref('10分钟前')
const sentinelTypeFilter = ref('all')
const sentinelStatusFilter = ref('all')
const sentinelDetailVisible = ref(false)
const detailAlert = ref(null)

const typeFilterOptions = [
  { key: 'all', label: '全部类型' },
  { key: 'foreshadowing', label: '伏笔' },
  { key: 'logic', label: '逻辑' },
  { key: 'character', label: '人物' },
  { key: 'rhythm', label: '节奏' }
]
const statusFilterOptions = [
  { key: 'all', label: '全部状态' },
  { key: 'unresolved', label: '未处理' },
  { key: 'resolved', label: '已处理' }
]

// 增强的统计卡片
const enhancedStatCards = computed(() => {
  const s = store.sentinelStats || {}
  const alerts = store.sentinelAlerts || []
  const countByType = (type) => alerts.filter(a => a.type === type && !a.resolved).length
  const cards = [
    { key: 'all', label: '总告警', value: s.total ?? 0, type: 'all', criticalCount: countByType('all'), color: 'border-[#e8e3dc]', textColor: 'text-[#6b6560]', borderColor: 'border-[#e8e3dc]', ringColor: 'ring-[#d97706]', change: '12%', changeUp: false },
    { key: 'foreshadowing', label: '伏笔巡查', value: s.foreshadowing ?? 0, type: 'foreshadowing', criticalCount: alerts.filter(a => a.type === 'foreshadowing' && a.severity === 'critical' && !a.resolved).length, borderColor: 'border-[#fcd34d]', textColor: 'text-[#92400e]', ringColor: 'ring-[#fcd34d]', change: '8%', changeUp: false },
    { key: 'logic', label: '逻辑检查', value: s.logic ?? 0, type: 'logic', criticalCount: alerts.filter(a => a.type === 'logic' && a.severity === 'critical' && !a.resolved).length, borderColor: 'border-red-300', textColor: 'text-red-700', ringColor: 'ring-red-400', change: '5%', changeUp: true },
    { key: 'rhythm', label: '节奏预警', value: s.rhythm ?? 0, type: 'rhythm', criticalCount: alerts.filter(a => a.type === 'rhythm' && a.severity === 'critical' && !a.resolved).length, borderColor: 'border-amber-300', textColor: 'text-amber-700', ringColor: 'ring-amber-400', change: '3%', changeUp: false }
  ]
  return cards
})

function toggleTypeFilter(key) {
  sentinelTypeFilter.value = sentinelTypeFilter.value === key ? 'all' : key
}

// 筛选后的告警列表
const filteredSentinelAlerts = computed(() => {
  let list = store.sentinelAlerts || []
  if (sentinelTypeFilter.value !== 'all') {
    list = list.filter(a => a.type === sentinelTypeFilter.value)
  }
  if (sentinelStatusFilter.value === 'unresolved') {
    list = list.filter(a => !a.resolved)
  } else if (sentinelStatusFilter.value === 'resolved') {
    list = list.filter(a => a.resolved)
  }
  return list
})

// 巡查历史
const sentinelLogs = computed(() => store.sentinelLogs || [])

function loadSentinelLogs() {
  if (store.currentProjectId) {
    store.fetchSentinelLogs(store.currentProjectId)
  }
}

async function handleScan() {
  if (!store.currentProjectId) return
  lastCheckTime.value = new Date().toLocaleString()
  await store.doSentinelScan(store.currentProjectId)
}

async function handleResolveAlert(id) {
  await store.resolveSentinelAlert(store.currentProjectId, id)
}

async function handleIgnoreAlert(id) {
  await store.ignoreSentinelAlert(store.currentProjectId, id)
}

async function handleDeleteSentinelAlert(id) {
  if (!confirm('确定要删除该告警吗？此操作不可恢复。')) return
  try {
    await sentinelApi.deleteAlert(store.currentProjectId, id)
    store.sentinelAlerts = store.sentinelAlerts.filter(a => a.id !== id)
    showToast('告警已删除', 'success')
  } catch (e) {
    showToast('删除失败：' + (e.message || '网络错误'), 'error')
  }
}

async function handleClearResolvedAlerts() {
  if (!confirm('确定要删除所有已处理的告警吗？此操作不可恢复。')) return
  try {
    await sentinelApi.clearResolvedAlerts(store.currentProjectId)
    store.sentinelAlerts = store.sentinelAlerts.filter(a => !a.resolved)
    showToast('已处理告警已清空', 'success')
  } catch (e) {
    showToast('清空失败：' + (e.message || '网络错误'), 'error')
  }
}

// 告警详情弹窗
function openAlertDetail(alert) {
  detailAlert.value = alert
  sentinelDetailVisible.value = true
}

function closeAlertDetail() {
  sentinelDetailVisible.value = false
  detailAlert.value = null
}

// 告警类型映射
const typeLabels = { foreshadowing: '伏笔', logic: '逻辑', character: '人物', rhythm: '节奏' }
const severityIcons = { critical: '🔴', warning: '🟡', info: '🔵' }
const severityLabels = { critical: '严重', warning: '警告', info: '提示' }
const dimLabels = { foreshadowing: '伏笔', logic: '逻辑', character: '人物', rhythm: '节奏' }

function severityTagClass(severity) {
  return severity === 'critical' ? 'bg-red-100 text-red-700' : severity === 'warning' ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'
}

function typeTagClass(type) {
  const map = { foreshadowing: 'bg-purple-50 text-purple-700', logic: 'bg-red-50 text-red-700', character: 'bg-blue-50 text-blue-700', rhythm: 'bg-amber-50 text-amber-700' }
  return map[type] || 'bg-gray-50 text-gray-600'
}

function formatSentinelTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return t.replace('T', ' ').substring(0, 16)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// ─── 策略与设置 ───
const systemStatusLoading = ref(false)
const showPublishDialog = ref(false)
const showCompletionDialog = ref(false)
const showBackupDialog = ref(false)
const showAIModelDialog = ref(false)
const showSystemStatusDetail = ref(false)
const settingsSaving = ref(false)
let statusPollingTimer = null

const publishConfig = reactive({
  schedule: '每周二、五 08:00',
  nextRun: null,
  frequency: 'twice-weekly',
  time: '08:00',
  customDays: 3
})

const editPublishForm = reactive({
  frequency: 'twice-weekly',
  time: '08:00',
  customDays: 3,
  nextRun: null
})

// ─── 完本计划（起始时间 + 预定完本时间，按作品隔离） ───
const editCompletionForm = reactive({
  startingTime: '',
  plannedCompletionDate: ''
})

const completionText = computed(() => {
  const d = project.value?.plannedCompletionDate
  return d ? `预定 ${String(d).slice(0, 10)} 完本` : '暂未设置'
})

const completionHint = computed(() => {
  const d = project.value?.plannedCompletionDate
  if (!d) return '设定目标日期，陪伴作品走向完结'
  const days = Math.ceil((new Date(String(d).slice(0, 10)) - new Date()) / 86400000)
  if (days > 0) return `距完本还有 ${days} 天`
  if (days === 0) return '今天就是预定完本日！'
  return `已超过预定时间 ${Math.abs(days)} 天`
})

function openCompletionDialog() {
  editCompletionForm.startingTime = project.value?.startingTime || ''
  editCompletionForm.plannedCompletionDate = project.value?.plannedCompletionDate ? String(project.value.plannedCompletionDate).slice(0, 10) : ''
  showCompletionDialog.value = true
}

const backupConfig = reactive({
  interval: '每5分钟',
  keepVersions: 20,
  cloudSync: true,
  cloudProvider: 'aliyun'
})

const editBackupForm = reactive({
  interval: '5',
  keepVersions: 20,
  cloudSync: true,
  cloudProvider: 'aliyun'
})

const aiModelConfig = reactive({
  model: 'DeepSeek v4 Pro',
  style: 'creative',
  styleLabel: '创意模式',
  temperature: 0.7,
  maxTokens: '2048'
})

const editAIModelForm = reactive({
  model: 'DeepSeek v4 Pro',
  style: 'creative',
  temperature: 0.7,
  maxTokens: '2048'
})

const systemStatus = reactive({
  ai: 'healthy',
  aiLatency: 120,
  es: 'healthy',
  db: 'healthy',
  dbPoolUsage: 45
})

const storageStatus = computed(() => {
  const used = 62
  const usedPercent = used
  let colorClass = 'bg-emerald-500'
  let barColor = 'bg-gradient-to-r from-emerald-500 to-teal-400'
  let textColor = 'text-emerald-600'

  if (used >= 90) {
    colorClass = 'bg-red-500'
    barColor = 'bg-gradient-to-r from-red-500 to-[#d97706]'
    textColor = 'text-red-600'
  } else if (used >= 70) {
    colorClass = 'bg-amber-500'
    barColor = 'bg-gradient-to-r from-amber-500 to-[#d97706]'
    textColor = 'text-amber-600'
  } else {
    barColor = 'bg-gradient-to-r from-[#d97706] to-orange-400'
  }

  return {
    usedPercent,
    used: '61.8 GB',
    free: '38.2 GB',
    colorClass,
    barColor,
    textColor
  }
})

const systemStatusDetails = ref([
  { name: 'AI 服务', status: 'healthy', latency: '120ms', uptime: '99.9%', message: '所有模型服务正常运行' },
  { name: 'ES 集群', status: 'healthy', latency: '15ms', uptime: '99.99%', message: '3个节点全部在线' },
  { name: '数据库', status: 'healthy', latency: '8ms', uptime: '99.95%', message: '主从复制正常' },
  { name: '对象存储', status: 'warning', message: '使用率达到62%，建议清理' },
  { name: '向量检索服务', status: 'healthy', latency: '25ms', uptime: '99.8%', message: '千问 Embedding 服务正常' }
])

const recentOperationLogs = ref([
  { id: 1, action: '更新发布计划', detail: '从"每周一次"改为"每周两次"', time: Date.now() - 3600000 },
  { id: 2, action: '保存 AI 偏好', detail: '温度参数调整为 0.8', time: Date.now() - 7200000 },
  { id: 3, action: '更新完本计划', detail: '预定完本时间调整为下月', time: Date.now() - 86400000 }
])

async function refreshSystemStatus() {
  systemStatusLoading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 800))
    systemStatus.ai = 'healthy'
    systemStatus.aiLatency = Math.floor(Math.random() * 50) + 100
    systemStatus.es = 'healthy'
    systemStatus.db = 'healthy'
    systemStatus.dbPoolUsage = Math.floor(Math.random() * 30) + 30
  } catch (e) {
    showChapterToast('刷新系统状态失败', 'error')
  } finally {
    systemStatusLoading.value = false
  }
}

function openPublishDialog() {
  editPublishForm.frequency = publishConfig.frequency || 'twice-weekly'
  editPublishForm.time = publishConfig.time || '08:00'
  editPublishForm.customDays = publishConfig.customDays || 3
  editPublishForm.nextRun = calculateNextRun(editPublishForm.frequency, editPublishForm.time)
  showPublishDialog.value = true
}

function calculateNextRun(frequency, time) {
  const now = new Date()
  const [hours, minutes] = time.split(':').map(Number)
  let next = new Date(now)
  next.setHours(hours, minutes, 0, 0)

  if (next <= now) {
    next.setDate(next.getDate() + 1)
  }

  return next.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' + time
}

async function savePublishConfig() {
  try {
    publishConfig.frequency = editPublishForm.frequency
    publishConfig.time = editPublishForm.time
    publishConfig.customDays = editPublishForm.customDays

    const freqMap = {
      'daily': '每日',
      'twice-weekly': '每周两次',
      'weekly': '每周一次',
      'custom': `每${editPublishForm.customDays}天`
    }
    publishConfig.schedule = `${freqMap[editPublishForm.frequency]} ${editPublishForm.time}`

    showPublishDialog.value = false
    showChapterToast('发布计划已保存', 'success')
    addOperationLog('更新发布计划', `发布周期: ${publishConfig.schedule}`)
  } catch (e) {
    showChapterToast('保存失败: ' + e.message, 'error')
  }
}

async function saveCompletionConfig() {
  if (settingsSaving.value) return
  const pid = store.currentProjectId
  if (!pid) {
    showChapterToast('未找到当前作品', 'error')
    return
  }
  settingsSaving.value = true
  try {
    await store.updateProject(pid, {
      startingTime: editCompletionForm.startingTime || '',
      plannedCompletionDate: editCompletionForm.plannedCompletionDate || null
    })
    showCompletionDialog.value = false
    showChapterToast('完本计划已保存', 'success')
    addOperationLog('更新完本计划', `预定完本: ${editCompletionForm.plannedCompletionDate || '未设置'}`)
  } catch (e) {
    showChapterToast('保存失败: ' + e.message, 'error')
  } finally {
    settingsSaving.value = false
  }
}

async function saveBackupConfig() {
  if (settingsSaving.value) return
  settingsSaving.value = true
  try {
    const intervalMap = {
      '5': '每5分钟',
      '15': '每15分钟',
      '30': '每30分钟',
      '60': '每1小时'
    }
    backupConfig.interval = intervalMap[editBackupForm.interval] || editBackupForm.interval
    backupConfig.keepVersions = editBackupForm.keepVersions
    backupConfig.cloudSync = editBackupForm.cloudSync
    backupConfig.cloudProvider = editBackupForm.cloudProvider

    showBackupDialog.value = false
    showChapterToast('备份设置已保存', 'success')
    addOperationLog('更新备份策略', `间隔: ${backupConfig.interval}, 保留: ${backupConfig.keepVersions}个版本`)
  } catch (e) {
    showChapterToast('保存失败: ' + e.message, 'error')
  } finally {
    settingsSaving.value = false
  }
}

async function saveAIModelConfig() {
  if (settingsSaving.value) return
  settingsSaving.value = true
  try {
    aiModelConfig.model = editAIModelForm.model
    aiModelConfig.style = editAIModelForm.style
    aiModelConfig.temperature = editAIModelForm.temperature
    aiModelConfig.maxTokens = editAIModelForm.maxTokens

    const styleMap = {
      'creative': '创意模式',
      'balanced': '平衡模式',
      'precise': '精确模式'
    }
    aiModelConfig.styleLabel = styleMap[editAIModelForm.style] || '创意模式'

    showAIModelDialog.value = false
    showChapterToast('AI 模型偏好已保存', 'success')
    addOperationLog('保存 AI 偏好', `模型: ${aiModelConfig.model}, 温度: ${aiModelConfig.temperature}`)
  } catch (e) {
    showChapterToast('保存失败: ' + e.message, 'error')
  } finally {
    settingsSaving.value = false
  }
}

function addOperationLog(action, detail) {
  recentOperationLogs.value.unshift({
    id: Date.now(),
    action,
    detail,
    time: Date.now()
  })
  if (recentOperationLogs.value.length > 20) {
    recentOperationLogs.value.pop()
  }
}

function confirmResetAllSettings() {
  if (confirm('确定要重置所有设置为默认值吗？此操作不可撤销。')) {
    resetAllSettings()
  }
}

function resetAllSettings() {
  publishConfig.schedule = '每周两次 08:00'
  publishConfig.frequency = 'twice-weekly'
  publishConfig.time = '08:00'

  backupConfig.interval = '每5分钟'
  backupConfig.keepVersions = 20
  backupConfig.cloudSync = true

  aiModelConfig.model = 'DeepSeek v4 Pro'
  aiModelConfig.style = 'creative'
  aiModelConfig.styleLabel = '创意模式'
  aiModelConfig.temperature = 0.7

  showChapterToast('已重置为默认设置', 'success')
  addOperationLog('重置所有设置', '恢复为系统默认配置')
}

watch(() => editPublishForm.frequency, (newFreq) => {
  if (newFreq && showPublishDialog.value) {
    editPublishForm.nextRun = calculateNextRun(newFreq, editPublishForm.time)
  }
})

watch(() => editPublishForm.time, (newTime) => {
  if (newTime && showPublishDialog.value) {
    editPublishForm.nextRun = calculateNextRun(editPublishForm.frequency, newTime)
  }
})

watch(() => activeTool.value, (newTool) => {
  if (newTool === 'strategy') {
    startStatusPolling()
  } else {
    stopStatusPolling()
  }
})

function startStatusPolling() {
  if (statusPollingTimer) return
  refreshSystemStatus()
  statusPollingTimer = setInterval(() => {
    refreshSystemStatus()
  }, 30000)
}

function stopStatusPolling() {
  if (statusPollingTimer) {
    clearInterval(statusPollingTimer)
    statusPollingTimer = null
  }
}

// ─── 批量导出 ───
const selectedFormat = ref('txt')
const exportScope = ref('all')
const includeOutline = ref(true)
const includeCharacters = ref(false)
const layoutTemplate = ref('默认')
const autoSave = ref(true)
const cloudSync = ref(true)

const defaultExportFormats = [
  { key: 'txt', icon: '📄', label: 'TXT', desc: '纯文本通用格式', size: '约 37 KB' },
  { key: 'md', icon: '📝', label: 'Markdown', desc: '保留排版标记', size: '约 56 KB' },
  { key: 'pdf', icon: '📕', label: 'PDF', desc: '精美排版可打印', size: '约 2.3 MB' },
  { key: 'docx', icon: '📘', label: 'Word', desc: '.docx格式', size: '约 1.8 MB' },
  { key: 'html', icon: '🌐', label: 'HTML', desc: '网页格式', size: '约 92 KB' },
  { key: 'all', icon: '📦', label: '全部打包', desc: '所有格式', size: '约 4.4 MB' }
]

const invalidRecordCount = computed(() =>
  store.exportHistory.filter(r => r.valid === false).length
)

// ─── 上下文引擎状态与逻辑 ───
const autoRefreshEnabled = ref(false)
const autoRefreshTimer = ref(null)
const hasLoadedContextData = ref(false)
const configEditing = ref(false)
const editConfigForm = reactive({
  autoIndex: true,
  windowSize: 512,
  topK: 10,
  semanticWeight: 70
})
const showRebuildConfirm = ref(false)
const rebuildConfirmChecked = ref(false)
const showHealthCheckModal = ref(false)
const showAllLogsPanel = ref(false)
const sizeTrendChartRef = ref(null)
let sizeTrendChartInstance = null

function toggleAutoRefresh() {
  autoRefreshEnabled.value = !autoRefreshEnabled.value
  if (autoRefreshEnabled.value) {
    autoRefreshTimer.value = setInterval(() => {
      if (activeTool.value === 'contextEngine' && store.currentProjectId) {
        store.fetchContextStats(store.currentProjectId)
        store.fetchContextActivities(store.currentProjectId, 20)
      }
    }, 30000)
    showChapterToast('已开启自动刷新（每30秒）', 'success')
  } else {
    if (autoRefreshTimer.value) {
      clearInterval(autoRefreshTimer.value)
      autoRefreshTimer.value = null
    }
    showChapterToast('已关闭自动刷新', 'success')
  }
}

async function refreshContextData() {
  const pid = store.currentProjectId
  if (!pid) return
  hasLoadedContextData.value = true
  await store.loadAllContextData(pid)
  showChapterToast('数据已刷新', 'success')
  renderSizeTrendChart()
}

function startEditConfig() {
  const cfg = store.contextConfig
  editConfigForm.autoIndex = cfg.autoIndex ?? true
  editConfigForm.windowSize = cfg.windowSize ?? 512
  editConfigForm.topK = cfg.topK ?? 10
  editConfigForm.semanticWeight = Math.round((cfg.hybridWeights?.semantic ?? 0.7) * 100)
  configEditing.value = true
}

async function saveConfig() {
  const pid = store.currentProjectId
  if (!pid) return
  const newConfig = {
    autoIndex: editConfigForm.autoIndex,
    windowSize: editConfigForm.windowSize,
    topK: editConfigForm.topK,
    hybridWeights: {
      semantic: editConfigForm.semanticWeight / 100,
      bm25: (100 - editConfigForm.semanticWeight) / 100
    }
  }
  try {
    await store.updateContextConfig(pid, newConfig)
    configEditing.value = false
    showChapterToast('配置已保存', 'success')
  } catch (e) {
    showChapterToast('保存失败：' + (e.message || '未知错误'), 'error')
  }
}

function cancelEditConfig() {
  configEditing.value = false
}

function resetConfigToDefault() {
  editConfigForm.autoIndex = true
  editConfigForm.windowSize = 512
  editConfigForm.topK = 10
  editConfigForm.semanticWeight = 70
  showChapterToast('已恢复默认配置', 'success')
}

function confirmRebuild() {
  showRebuildConfirm.value = true
  rebuildConfirmChecked.value = false
}

async function executeRebuild() {
  const pid = store.currentProjectId
  if (!pid) return
  showRebuildConfirm.value = false
  rebuildConfirmChecked.value = false
  try {
    await store.rebuildContextIndex(pid)
    showChapterToast('索引重建任务已启动', 'success')
  } catch (e) {
    showChapterToast('重建失败：' + (e.message || '未知错误'), 'error')
  }
}

async function cancelRebuild() {
  const pid = store.currentProjectId
  if (!pid) return
  try {
    await store.cancelContextOperation(pid)
    showChapterToast('操作已取消', 'success')
  } catch (e) {
    showChapterToast('取消失败：' + (e.message || '未知错误'), 'error')
  }
}

async function handleIncrementalIndex() {
  const pid = store.currentProjectId
  if (!pid) return
  try {
    await store.incrementalContextIndex(pid)
    showChapterToast('增量索引完成', 'success')
  } catch (e) {
    showChapterToast('增量索引失败：' + (e.message || '未知错误'), 'error')
  }
}

async function handleCleanup() {
  const pid = store.currentProjectId
  if (!pid) return
  if (!confirm('确定要清理无效的索引数据吗？此操作不可恢复。')) return
  try {
    await store.cleanupContextIndex(pid)
    showChapterToast('索引清理完成', 'success')
  } catch (e) {
    showChapterToast('清理失败：' + (e.message || '未知错误'), 'error')
  }
}

async function showHealthCheck() {
  showHealthCheckModal.value = true
  await loadHealthCheck()
}

async function loadHealthCheck() {
  const pid = store.currentProjectId
  if (!pid) return
  try {
    await store.fetchContextHealthCheck(pid)
  } catch (e) {
    console.warn('健康检查加载失败:', e.message)
  }
}

function loadMoreLogs() {
  const pid = store.currentProjectId
  if (!pid) return
  store.fetchContextActivities(pid, 50)
  showChapterToast('已加载更多日志', 'success')
}

function goToChapter(chapterId) {
  if (!chapterId) return
  const ch = chapters.value.find(c => String(c.id) === String(chapterId))
  if (ch) {
    store.selectChapter(ch)
    activeTool.value = 'aiWrite'
    showAllLogsPanel.value = false
  }
}

function formatIndexTime(time) {
  if (!time) return ''
  const d = new Date(time)
  if (isNaN(d.getTime())) return String(time)
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatRelativeTime(time) {
  if (!time) return ''
  const d = new Date(time)
  if (isNaN(d.getTime())) return String(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return String(date)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function renderSizeTrendChart() {
  if (!sizeTrendChartRef.value) return
  const trendData = store.contextSizeTrend || []
  if (trendData.length === 0) return
  if (sizeTrendChartInstance) {
    sizeTrendChartInstance.dispose()
    sizeTrendChartInstance = null
  }
  try {
    sizeTrendChartInstance = echarts.init(sizeTrendChartRef.value)
    const dates = trendData.map(d => d.date ? new Date(d.date).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) : '')
    const sizes = trendData.map(d => d.size ? (d.size / (1024 * 1024)).toFixed(1) : 0)
    sizeTrendChartInstance.setOption({
      grid: { left: 0, right: 0, top: 5, bottom: 0 },
      xAxis: { type: 'category', data: dates, show: false },
      yAxis: { type: 'value', show: false },
      series: [{
        type: 'line',
        data: sizes,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#d97706', width: 2 },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(217, 119, 6, 0.3)' }, { offset: 1, color: 'rgba(217, 119, 6, 0.05)' }] } }
      }]
    })
  } catch (e) {
    console.warn('趋势图渲染失败:', e.message)
  }
}

async function handleExportReport() {
  const pid = store.currentProjectId
  if (!pid) return
  try {
    const data = await store.exportContextReport(pid, 'json')
    if (data) {
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `index-report-${new Date().toISOString().slice(0, 10)}.json`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      showChapterToast('报告已导出', 'success')
    }
  } catch (e) {
    showChapterToast('导出失败：' + (e.message || '未知错误'), 'error')
  }
}

watch(activeTool, (val) => {
  if (val === 'contextEngine') {
    hasLoadedContextData.value = true
    const pid = store.currentProjectId
    if (pid) {
      store.loadAllContextData(pid)
      nextTick(() => renderSizeTrendChart())
    }
  }
})

onUnmounted(() => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
  }
  if (sizeTrendChartInstance) {
    sizeTrendChartInstance.dispose()
    sizeTrendChartInstance = null
  }
})

// ─── 个人设置 ───
const activeSettingTab = ref('account')
const settingTabs = [
  { key: 'account', label: '👤 账户信息' },
  { key: 'editor', label: '✏️ 编辑器' },
  { key: 'appearance', label: '🎨 外观' },
  { key: 'notifications', label: '🔔 通知提醒' },
  { key: 'shortcut', label: '⌨️ 快捷键' }
]
const userProfile = reactive({ nickname: '墨染青衫', bio: '玄幻小说爱好者，笔耕不辍', email: 'writer@example.com', phone: '138****8888' })
const activeEditorTheme = ref('warm')
const editorThemes = [
  { key: 'warm', label: '暖调象牙白', bg: '#faf7f2' },
  { key: 'dark', label: '深夜暗色', bg: '#1a1a2e' },
  { key: 'parchment', label: '羊皮纸', bg: '#f5f0e8' },
  { key: 'green', label: '护眼绿', bg: '#e8f5e9' }
]
const writingModeOptions = [
  { key: '双栏模式', label: '双栏模式', desc: '左大纲右写作区 · 适合长篇创作' },
  { key: '专注模式', label: '专注模式', desc: '全屏写作 · 无干扰 · 沉浸式体验' },
  { key: '大纲模式', label: '大纲模式', desc: '仅大纲视图 · 适合结构规划' }
]
const editorSettings = reactive({ fontSize: 16, lineHeight: 1.65, writingMode: '双栏模式', bodyFont: 'Crimson Pro (推荐)', autoSave: true, saveInterval: '5分钟', cloudSync: true, versionHistory: '最近20个' })
const appearanceThemeColor = ref('#d97706')
const themeColors = [
  { key: '#d97706', label: '琥珀金', color: '#d97706' },
  { key: '#0e7490', label: '青色', color: '#0e7490' },
  { key: '#7c3aed', label: '紫色', color: '#7c3aed' },
  { key: '#be123c', label: '玫瑰红', color: '#be123c' },
  { key: '#0d9488', label: '翡翠绿', color: '#0d9488' }
]
const appearanceSettings = reactive({ sidebarPosition: '左侧', sidebarWidth: '标准 (260px)', showSidebar: true, compactMode: false })
const animEffects = reactive([
  { key: 'pageAnim', label: '页面切换动画', desc: '模块切换时的淡入滑动效果', enabled: true },
  { key: 'hoverEffect', label: '悬停效果', desc: '卡片和按钮的悬停反馈', enabled: true },
  { key: 'loadingAnim', label: '加载动画', desc: '启动时的加载过渡效果', enabled: true },
  { key: 'bgTexture', label: '背景纹理', desc: '纸张质感的噪点纹理', enabled: true }
])
const notifItems = reactive([
  { key: 'sentinel', label: '智能哨兵告警', desc: '伏笔、逻辑、人物、节奏问题', enabled: true },
  { key: 'agent', label: 'Agent分析完成', desc: '多Agent协同分析结果', enabled: true },
  { key: 'goal', label: '写作目标提醒', desc: '每日字数目标达成/未达成', enabled: true },
  { key: 'version', label: '版本保存通知', desc: '自动保存和版本创建', enabled: false }
])
const notifReminders = reactive({ bell: true, sound: false, threshold: '警告以上', dnd: '关闭' })
const shortcutGroups = [
  { label: '基础操作', items: [
    { action: '保存文档', keys: ['Ctrl','S'] },
    { action: '撤销', keys: ['Ctrl','Z'] },
    { action: '重做', keys: ['Ctrl','Shift','Z'] },
    { action: '查找替换', keys: ['Ctrl','H'] }
  ]},
  { label: 'AI功能', items: [
    { action: 'AI续写', keys: ['Ctrl','Space'] },
    { action: 'AI润色', keys: ['Ctrl','Shift','P'] },
    { action: 'AI扩写', keys: ['Ctrl','Shift','E'] },
    { action: 'AI缩写', keys: ['Ctrl','Shift','C'] }
  ]},
  { label: '视图切换', items: [
    { action: '专注模式', keys: ['F11'] },
    { action: '大纲模式', keys: ['Ctrl','O'] },
    { action: '切换侧边栏', keys: ['Ctrl','\\'] }
  ]}
]

// ─── 导出功能 ───
async function loadExportData() {
  const pid = store.currentProjectId
  if (!pid) return
  await Promise.all([
    store.fetchExportFormats().catch(e => console.warn('加载导出格式失败:', e.message)),
    store.fetchExportHistory(pid).catch(e => console.warn('加载导出历史失败:', e.message))
  ])
}

function formatExportDate(timestamp) {
  if (!timestamp) return ''
  const d = new Date(timestamp)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function buildExportParams(scope = exportScope.value) {
  const pid = store.currentProjectId
  if (!pid) return null

  let chapterRange = '全书'
  let startChapter = null
  let endChapter = null

  if (scope === 'all') {
    chapterRange = '全书'
  } else if (scope === 'vol1' || scope === 'vol2') {
    chapterRange = scope === 'vol1' ? '第一卷' : '第二卷'
  } else if (scope === 'custom') {
    chapterRange = '自定义章节'
  }

  return {
    format: selectedFormat.value,
    scope: scope,
    includeOutline: includeOutline.value,
    includeCharacters: includeCharacters.value,
    layoutTemplate: layoutTemplate.value,
    autoBackup: autoSave.value,
    cloudSync: cloudSync.value,
    chapterRange,
    startChapter,
    endChapter,
    projectId: pid
  }
}

async function handleStartExport() {
  const pid = store.currentProjectId
  if (!pid) {
    showChapterToast('请先选择一个作品', 'error')
    return
  }
  if (store.exporting) return

  const params = buildExportParams()
  if (!params) return

  try {
    showChapterToast('开始导出...', 'success')
    const result = await store.doExport(pid, params)
    if (result?.downloadUrl) {
      triggerDownload(result.downloadUrl, result.fileName || 'export.' + selectedFormat.value)
    }
    showChapterToast('导出成功！', 'success')
  } catch (e) {
    console.error('导出失败:', e)
    showChapterToast('导出失败：' + (e.message || '未知错误'), 'error')
  }
}

async function handleQuickExport() {
  exportScope.value = 'all'
  selectedFormat.value = 'all'
  await handleStartExport()
}

function handleDownloadRecord(rec) {
  if (!rec.valid || !rec.downloadUrl) {
    showChapterToast('文件已失效，请重新导出', 'error')
    return
  }
  triggerDownload(rec.downloadUrl, rec.fileName || rec.name || 'export')
}

function triggerDownload(url, fileName) {
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function handleDeleteRecord(recordId) {
  store.removeExportRecord(recordId)
  showChapterToast('记录已删除', 'success')
}

function handleClearInvalidRecords() {
  const removed = store.clearInvalidExportRecords()
  if (removed > 0) {
    showChapterToast(`已清理 ${removed} 条无效记录`, 'success')
  }
}

function handleClearAllRecords() {
  if (!confirm('确定要清空所有导出记录吗？此操作不可恢复。')) return
  store.exportHistory.splice(0, store.exportHistory.length)
  localStorage.removeItem('export_history_' + store.currentProjectId)
  showChapterToast('所有记录已清空', 'success')
}

// ─── Init ───
onMounted(async () => {
  if (activeTool.value === 'strategy') {
    startStatusPolling()
  }
  const pid = Number(route.params.projectId)
  if (pid) {
    if (store.currentProject?.id !== pid) { const p = store.projects.find(x => x.id === pid); if (p) store.selectProject(p) }
    // 加载全部模块数据
    await store.refreshAll(pid)
    // 同时加载伏笔数据
    try {
      const { plotApi } = await import('@/api')
      const f = await plotApi.listForeshadowing(pid)
      if (f) foreshadowingList.value = f.map(fs => ({
        id: fs.id, icon: '🎯', title: fs.name, desc: fs.description || '', buried: fs.chapterId || '?', shouldReveal: fs.resolvedChapterId || '待定', status: fs.status, statusLabel: fs.status === 'resolved' ? '已回收' : fs.status === 'triggered' ? '触发中' : '待回收'
      }))
    } catch (e) { console.warn('伏笔加载失败:', e.message) }
    if (chapters.value.length) selectChapter(chapters.value[0])
  }
})
onUnmounted(() => {
  if (tensionChartInstance) { tensionChartInstance.dispose(); tensionChartInstance = null }
  stopStatusPolling()
})

// ─── 新增便捷弹窗 ───
const addDialog = reactive({ show: false, module: '', title: '', fields: [], onConfirm: null })
function closeAddDialog() { addDialog.show = false }

function promptAdd(module, title, fields, onConfirm) {
  addDialog.module = module
  addDialog.title = title
  addDialog.fields = fields.map(f => ({ ...f, value: ref(f.value || '') }))
  addDialog.onConfirm = () => {
    const data = {}
    for (const f of addDialog.fields) data[f.key] = f.value.value
    addDialog.show = false
    onConfirm(data)
  }
  addDialog.show = true
}

// ─── 人物新增 ───
function handleAddCharacter() {
  if (!store.currentProjectId) return
  resetCharacterForm()
  showCharacterDialog.value = true
}

// ─── 情节新增 ───
async function handleAddPlot() {
  const pid = store.currentProjectId
  if (!pid) return
  promptAdd('plot', '🎯 新建情节线', [
    { key: 'name', label: '情节线名称', placeholder: '如：帝国权谋主线', value: '' },
    { key: 'type', label: '情节类型', placeholder: '请选择情节类型', value: 'sub', type: 'select', options: PLOT_CATEGORIES },
    { key: 'description', label: '情节描述', placeholder: '描述该情节线的发展脉络...', value: '', type: 'textarea' }
  ], async (data) => {
    if (!data.name.trim()) return
    try {
      await plotApi.createThread(pid, { name: data.name.trim(), type: data.type.trim() || 'sub', description: data.description?.trim() || '' })
      await store.refreshPlot(pid)
      showChapterToast('情节线已创建', 'success')
    } catch (e) { showChapterToast('创建失败', 'error') }
  })
}



// ─── 导航方法 ───
function goToMyWorks() {
  router.push('/my-works')
}


</script>

<style scoped>
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.toast-fade-enter-active { transition: all 0.25s ease-out; }
.toast-fade-leave-active { transition: all 0.2s ease-in; }
.toast-fade-enter-from { opacity: 0; transform: translateX(20px); }
.toast-fade-leave-to { opacity: 0; transform: translateX(20px); }

.modal-fade-enter-active { transition: all 0.2s ease-out; }
.modal-fade-leave-active { transition: all 0.15s ease-in; }
.modal-fade-enter-from { opacity: 0; }
.modal-fade-enter-from > div { transform: scale(0.95); }
.modal-fade-leave-to { opacity: 0; }
.modal-fade-leave-to > div { transform: scale(0.95); }

.slide-fade-enter-active { transition: all 0.3s ease-out; }
.slide-fade-leave-active { transition: all 0.2s ease-in; }
.slide-fade-enter-from { opacity: 0; transform: translateX(100%); }
.slide-fade-leave-to { opacity: 0; transform: translateX(100%); }

/* 弧光进度滑块自定义样式 */
input[type="range"] {
  -webkit-appearance: none;
  appearance: none;
  height: 6px;
  border-radius: 3px;
  outline: none;
}
input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35, #d97706);
  cursor: pointer;
  box-shadow: 0 0 12px rgba(255, 107, 53, 0.6), 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: transform 0.15s ease, box-shadow 0.3s ease;
}
input[type="range"]::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  box-shadow: 0 0 20px rgba(255, 107, 53, 0.8), 0 3px 12px rgba(0, 0, 0, 0.3);
}
input[type="range"]::-webkit-slider-thumb:active {
  transform: scale(1.1);
  box-shadow: 0 0 25px rgba(255, 107, 53, 0.9), 0 2px 8px rgba(0, 0, 0, 0.2);
}
input[type="range"]::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35, #d97706);
  cursor: pointer;
  border: none;
  box-shadow: 0 0 12px rgba(255, 107, 53, 0.6), 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: box-shadow 0.3s ease;
}
input[type="range"]::-webkit-slider-runnable-track {
  border-radius: 3px;
  height: 6px;
}

/* 颜色选择器样式 */
input[type="color"] {
  -webkit-appearance: none;
  appearance: none;
  padding: 0;
  border: none;
  cursor: pointer;
}
input[type="color"]::-webkit-color-swatch-wrapper {
  padding: 0;
}
input[type="color"]::-webkit-color-swatch {
  border: none;
  border-radius: 6px;
}
</style>