<template>
  <div class="editor-root flex flex-col h-full" :class="[
    `editor-theme-${settingsStore.editorTheme}`,
    `mode-${settingsStore.writingMode}`
  ]" @keydown="handleKeydown">

    <!-- ═══ 顶部工具栏 ═══ -->
    <div class="flex items-center px-4 py-2 border-b border-[#e8e3dc] bg-[#faf8f5] flex-shrink-0 gap-2">
      <!-- 章节编号 + 标题（可编辑） -->
      <div class="flex items-center gap-2 flex-1 min-w-0">
        <span class="text-xs text-[#9c9690] flex-shrink-0">📝</span>
        <!-- 章节序号 -->
        <span v-if="currentChapterIndex >= 0 && store.currentChapter"
          class="text-xs text-[#9c9690] bg-[#f3efe8] px-1.5 py-0.5 rounded flex-shrink-0 font-medium"
          style="font-family:var(--font-mono)">
          CH.{{ currentChapterIndex + 1 }}
        </span>
        <input
          v-if="store.currentChapter"
          :value="store.chapterTitle"
          @input="onTitleChange"
          class="text-sm font-semibold text-[#1a1815] bg-transparent outline-none border-b border-transparent hover:border-[#d4cec6] focus:border-[#d97706] transition-colors px-1 min-w-0 flex-1"
          placeholder="章节标题..."
          style="font-family:var(--font-display)"
        >
        <span v-else class="text-sm font-semibold text-[#9c9690]" style="font-family:var(--font-display)">未选择章节</span>
        <!-- 状态标签（可点击切换） -->
        <span v-if="store.currentChapter"
          class="text-xs px-1.5 py-0.5 rounded flex-shrink-0 cursor-pointer transition-colors hover:opacity-80"
          :class="store.currentChapter.status === 'published' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'"
          @click="handleToggleStatus"
          :title="store.currentChapter.status === 'published' ? '点击切换为草稿' : '点击切换为已发布'">
          {{ store.currentChapter.status === 'published' ? '已发布' : '草稿' }}
        </span>
      </div>

      <!-- 分隔 -->
      <span class="text-[#d4cec6] text-xs">|</span>

      <!-- AI扩写按钮 -->
      <button
        class="text-xs px-2.5 py-1 rounded transition-colors flex items-center gap-1"
        :class="store.aiExpandLoading ? 'bg-[#fef3c7] text-[#92400e] cursor-wait' : 'bg-[#d97706] text-white hover:bg-[#b45309]'"
        :disabled="store.aiExpandLoading || !store.currentChapter"
        @click="handleAiExpand"
        title="AI续写当前章节"
      >
        <span v-if="store.aiExpandLoading" class="inline-block w-3 h-3 border-2 border-[#d97706] border-t-transparent rounded-full animate-spin"></span>
        <span v-else>✨</span>
        AI 扩写
      </button>

      <!-- AI润色 -->
      <button
        class="text-xs px-2.5 py-1 rounded transition-colors border border-[#e8e3dc] text-[#6b6560] hover:border-[#d97706] hover:text-[#d97706] flex items-center gap-1"
        :disabled="!store.currentChapter || !selectedText"
        @click="handleAiPolish"
        title="润色选中文本"
      >
        🎨 润色
      </button>

      <!-- 字数进度 -->
      <span class="text-[#d4cec6] text-xs">|</span>
      <span class="text-xs text-[#9c9690] whitespace-nowrap">
        {{ formatNumber(store.currentWordCount) }} / {{ formatNumber(store.targetWords) }} 字
      </span>
      <div class="w-20 h-1.5 bg-[#f3efe8] rounded-full overflow-hidden">
        <div
          class="h-full rounded-full transition-all duration-500 ease-out"
          :style="{ width: store.wordProgress + '%', background: progressBarColor }"
        ></div>
      </div>
      <span class="text-xs font-semibold" :style="{ color: progressBarColor }">{{ store.wordProgress }}%</span>
    </div>

    <!-- ═══ 三栏主体 ═══ -->
    <div class="flex flex-1 overflow-hidden">

      <!-- ══ 左栏：章节列表 (15%) ══ -->
      <aside v-show="settingsStore.writingMode !== 'focus'" class="w-56 bg-[#faf8f5] border-r border-[#e8e3dc] overflow-y-auto flex-shrink-0 flex flex-col">
        <div class="p-3 flex-1">
          <div class="flex items-center justify-between mb-2.5">
            <span class="text-xs font-bold text-[#9c9690] uppercase tracking-wider">📑 章节列表</span>
            <button
              class="w-7 h-7 rounded-md bg-[#fef3c7] hover:bg-[#d97706] hover:text-white flex items-center justify-center text-sm text-[#d97706] font-bold transition-colors"
              title="新建章节"
              @click="handleCreateChapter"
            >
              +
            </button>
          </div>

          <!-- 空状态 -->
          <div v-if="store.chapters.length === 0" class="text-xs text-[#9c9690] py-8 text-center">
            <div class="text-2xl mb-2">📖</div>
            <div>暂无章节</div>
            <button
              class="mt-2.5 text-[#d97706] hover:underline text-xs font-medium"
              @click="handleCreateChapter"
            >创建第一个章节</button>
          </div>

          <!-- 章节列表 -->
          <div
            v-for="(ch, i) in store.chapters"
            :key="ch.id"
            class="group flex items-center gap-2 px-2.5 py-2 rounded-lg cursor-pointer transition-colors mb-0.5 relative"
            :class="store.currentChapterId === ch.id ? 'bg-[#fef3c7]' : 'hover:bg-[#f3efe8]'"
            @click="handleSelectChapter(ch)"
          >
            <span class="text-xs w-5 flex-shrink-0"
              :class="store.currentChapterId === ch.id ? 'text-[#d97706] font-semibold' : 'text-[#9c9690]'"
            >{{ i + 1 }}</span>
            <span class="flex-1 text-xs truncate"
              :class="store.currentChapterId === ch.id ? 'text-[#92400e] font-semibold' : 'text-[#6b6560]'"
            >{{ ch.title || '未命名' }}</span>
            <!-- 状态圆点 -->
            <span
              class="w-1.5 h-1.5 rounded-full flex-shrink-0"
              :class="ch.status === 'published' ? 'bg-emerald-400' : 'bg-amber-400'"
              :title="ch.status === 'published' ? '已发布' : '草稿'"
            ></span>
            <!-- 删除按钮（悬停显示） -->
            <button
              class="absolute right-1 top-1/2 -translate-y-1/2 w-6 h-6 rounded-md items-center justify-center text-xs text-[#9c9690] hover:text-[#be123c] hover:bg-[#fee2e2] transition-colors hidden group-hover:flex"
              @click.stop="handleDeleteChapter(ch)"
              title="删除章节"
            >×</button>
          </div>
        </div>

        <!-- 左栏底部：章节统计 -->
        <div class="px-3 py-2 border-t border-[#e8e3dc] text-xs text-[#9c9690]">
          <div class="flex justify-between">
            <span>共 {{ store.chapters.length }} 章</span>
            <span>{{ store.totalWordCount.toLocaleString() }} 字</span>
          </div>
        </div>
      </aside>

      <!-- ══ 中栏：写作主区域 ══ -->
      <main v-show="settingsStore.writingMode !== 'outline'" class="flex-1 flex flex-col overflow-hidden bg-white">
        <!-- 正文编辑区 -->
        <div class="flex-1 overflow-y-auto px-8 py-5">
          <textarea
            v-if="store.currentChapter"
            :value="store.editorContent"
            @input="onContentChange"
            class="w-full h-full outline-none resize-none text-base bg-transparent"
            :style="{ fontFamily: settingsStore.fontFamily || 'var(--font-body)', fontSize: editorFontSize + 'px', lineHeight: editorLineHeight, minHeight: '50vh' }"
            :placeholder="placeholderText"
            ref="textareaRef"
          ></textarea>
          <div v-else class="flex items-center justify-center h-full text-[#9c9690]">
            <div class="text-center">
              <div class="text-4xl mb-3">📝</div>
              <div class="text-sm font-semibold mb-1">选择一个章节开始写作</div>
              <div class="text-xs">点击左侧章节列表或新建一个章节</div>
            </div>
          </div>
        </div>

        <!-- 底部状态栏 -->
        <div class="flex items-center gap-4 px-6 py-2 border-t border-[#e8e3dc] text-xs text-[#9c9690] bg-white flex-shrink-0">
          <span>📝 字数：{{ formatNumber(store.currentWordCount) }}</span>
          <span class="text-[#d4cec6]">|</span>
          <span>段落：{{ store.paragraphCount }}</span>
          <span class="text-[#d4cec6]">|</span>
          <span>⏱ 阅读时间：约 {{ store.readingTimeMinutes }} 分钟</span>
          <div class="flex-1"></div>
          <!-- 保存状态 -->
          <span class="flex items-center gap-1" :style="{ color: store.saveStatusColor }">
            <span v-if="store.isSaving" class="inline-block w-3 h-3 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
            <span v-else-if="store.lastSavedAt">✓</span>
            {{ store.saveStatusText }}
          </span>
          <!-- 手动保存按钮 -->
          <span class="text-[#d4cec6]">|</span>
          <button
            class="px-2.5 py-1 rounded text-xs font-medium transition-colors flex items-center gap-1"
            :class="store.isSaving ? 'bg-[#fef3c7] text-[#92400e] cursor-wait' : 'bg-[#d97706] text-white hover:bg-[#b45309]'"
            :disabled="store.isSaving || !store.currentChapter"
            @click="handleManualSave"
            title="保存 (Ctrl+S)"
          >
            💾 {{ store.isSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </main>

      <!-- ══ 右栏：AI 写作助手 (15%) ══ -->
      <aside class="w-64 bg-[#faf8f5] border-l border-[#e8e3dc] flex flex-col flex-shrink-0">
        <div class="px-2 py-2.5 border-b border-[#e8e3dc]">
          <span class="text-xs font-bold text-[#9c9690] uppercase tracking-wider">🤖 AI 写作助手</span>
        </div>

        <!-- Tab切换 -->
        <div class="flex border-b border-[#e8e3dc] gap-0">
          <button
            v-for="tab in aiTabs"
            :key="tab.key"
            class="flex-1 px-1 py-1 text-[10px] font-medium transition-colors whitespace-nowrap flex-shrink-0 overflow-hidden"
            :class="activeAiTab === tab.key ? 'text-[#d97706] border-b-2 border-[#d97706] bg-white' : 'text-[#9c9690] hover:text-[#6b6560]'"
            @click="activeAiTab = tab.key"
          >{{ tab.label }}</button>
        </div>

        <!-- Tab内容区 -->
        <div class="flex-1 overflow-y-auto p-2">
          <!-- 智能提示 -->
          <template v-if="activeAiTab === 'suggest'">
            <div class="text-xs text-[#9c9690] mb-2">
              AI 将自动分析当前内容并提供实时建议
            </div>
            <!-- 加载中 -->
            <div v-if="store.aiSuggestLoading" class="space-y-2">
              <div v-for="n in 3" :key="n" class="p-2 rounded-lg border border-[#e8e3dc] animate-pulse">
                <div class="h-2.5 bg-[#e8e3dc] rounded w-3/4 mb-2"></div>
                <div class="h-2 bg-[#f3efe8] rounded w-full mb-1"></div>
                <div class="h-2 bg-[#f3efe8] rounded w-2/3"></div>
              </div>
            </div>
            <!-- 建议列表 -->
            <div v-for="tip in aiTips" :key="tip.id" class="mb-2 p-2 rounded-lg border transition-all overflow-hidden"
              :class="tip.applied ? 'opacity-50' : ''"
              :style="{ background: tipLevelBg(tip.level), borderColor: tipLevelBorder(tip.level) }">
              <div class="flex items-start gap-1.5">
                <span class="text-sm flex-shrink-0">{{ tip.icon }}</span>
                <div class="min-w-0 break-words">
                  <div class="text-xs font-semibold" :style="{ color: tipLevelColor(tip.level) }">{{ tip.title }}</div>
                  <p class="text-xs leading-relaxed mt-0.5 break-words" :style="{ color: tipLevelColor(tip.level) }">{{ tip.content || '' }}</p>
                </div>
              </div>
              <div class="flex gap-1.5 mt-1.5">
                <button
                  class="text-[11px] px-2 py-0.5 rounded font-medium transition-colors"
                  :class="tip.applied ? 'bg-[#e8e3dc] text-[#9c9690] cursor-default' : 'bg-white border border-[#d97706] text-[#d97706] hover:bg-[#fef3c7]'"
                  :disabled="tip.applied"
                  @click="handleApplySuggestion(tip)"
                >
                  {{ tip.applied ? '✓ 已应用' : '应用建议' }}
                </button>
                <button
                  class="text-[11px] px-2 py-0.5 rounded text-[#9c9690] hover:text-[#6b6560] hover:bg-[#e8e3dc] transition-colors"
                  @click="handleDismissSuggestion(tip)"
                >忽略</button>
              </div>
            </div>
            <!-- 空状态 -->
            <div v-if="!store.aiSuggestLoading && !aiTips.length" class="text-xs text-[#9c9690] text-center py-8">
              <div class="text-2xl mb-2">💡</div>
              <div>开始写作后，AI 将自动分析并在这里提供实时建议</div>
            </div>
            <!-- 刷新按钮 -->
            <button
              v-if="store.currentChapter"
              class="w-full mt-1.5 py-1 text-[11px] rounded font-medium border border-[#e8e3dc] text-[#6b6560] hover:border-[#d97706] hover:text-[#d97706] transition-colors"
              :disabled="store.aiSuggestLoading"
              @click="handleRefreshSuggestions"
            >
              {{ store.aiSuggestLoading ? '分析中...' : '🔄 刷新建议' }}
            </button>
          </template>

          <!-- 对话 -->
          <template v-else-if="activeAiTab === 'chat'">
            <div class="flex flex-col h-full" style="min-height: 200px;">
              <div class="flex-1 space-y-2 mb-2 overflow-y-auto" style="max-height: 300px;" ref="chatMsgsRef">
                <div v-for="(msg, i) in chatMessages" :key="i" class="text-xs" :class="msg.role === 'user' ? 'text-right' : ''">
                  <div class="inline-block max-w-[90%] p-2 rounded-lg"
                    :class="msg.role === 'user' ? 'bg-[#d97706] text-white rounded-br-sm' : 'bg-white text-[#6b6560] rounded-bl-sm border border-[#e8e3dc]'"
                  >{{ msg.text || '' }}</div>
                </div>
                <!-- AI加载中 -->
                <div v-if="chatLoading" class="text-xs">
                  <div class="inline-block max-w-[90%] p-2 rounded-lg bg-white text-[#6b6560] rounded-bl-sm border border-[#e8e3dc]">
                    <span class="inline-flex gap-1">
                      <span class="w-1.5 h-1.5 bg-[#d97706] rounded-full animate-bounce" style="animation-delay: 0ms"></span>
                      <span class="w-1.5 h-1.5 bg-[#d97706] rounded-full animate-bounce" style="animation-delay: 150ms"></span>
                      <span class="w-1.5 h-1.5 bg-[#d97706] rounded-full animate-bounce" style="animation-delay: 300ms"></span>
                    </span>
                  </div>
                </div>
              </div>
              <div class="flex gap-1.5">
                <input
                  v-model="chatInput"
                  class="flex-1 px-2.5 py-1.5 border border-[#e8e3dc] rounded-lg text-xs bg-white outline-none focus:border-[#d97706]"
                  placeholder="输入指令，如'帮我写一段打斗场景'..."
                  @keyup.enter="handleSendChat"
                  :disabled="chatLoading"
                >
                <button
                  @click="handleSendChat"
                  class="px-3 py-1.5 bg-[#d97706] text-white rounded-lg text-xs font-semibold hover:bg-[#b45309] disabled:opacity-50 transition-colors"
                  :disabled="chatLoading || !chatInput.trim()"
                >发送</button>
              </div>
            </div>
          </template>

          <!-- 协同创作 -->
          <template v-else-if="activeAiTab === 'cowrite'">
            <AICoWriter
              :project-id="store.currentProjectId"
              :chapter-index="currentChapterIndex + 1"
              :current-content="store.editorContent"
              @generated="onContentGenerated"
            />
          </template>

          <!-- 哨兵监测 -->
          <template v-else-if="activeAiTab === 'sentinel'">
            <div class="flex flex-col gap-2">
              <div class="flex items-center justify-between">
                <span class="text-xs font-semibold text-[#6b6560]">本章哨兵告警</span>
                <div class="flex items-center gap-2">
                  <button
                    class="px-2 py-1 text-[11px] rounded bg-[#fee2e2] text-[#be123c] hover:bg-[#fecaca] transition-colors font-medium border border-[#fecaca]"
                    @click="handleClearResolvedAlerts"
                  >
                    清空已处理
                  </button>
                  <button
                    class="px-2 py-1 text-[11px] rounded bg-[#dcfce7] text-[#16a34a] hover:bg-[#bbf7d0] transition-colors font-medium"
                    :disabled="chapterAlertsLoading"
                    @click="handleScanChapter"
                  >
                    {{ chapterAlertsLoading ? '扫描中...' : '🔍 扫描本章' }}
                  </button>
                </div>
              </div>

              <!-- 加载状态 -->
              <div v-if="chapterAlertsLoading" class="space-y-1.5">
                <div v-for="n in 2" :key="n" class="p-2 rounded-lg border border-[#e8e3dc] animate-pulse">
                  <div class="h-3 bg-[#e8e3dc] rounded w-2/3 mb-2"></div>
                  <div class="h-2 bg-[#f3efe8] rounded w-full mb-1"></div>
                  <div class="h-2 bg-[#f3efe8] rounded w-3/4"></div>
                </div>
              </div>

              <!-- 告警列表 -->
              <div v-else-if="chapterAlerts.length > 0" class="space-y-1.5">
                <div v-for="alert in chapterAlerts" :key="alert.id" class="p-2 rounded-lg border border-red-200 text-xs bg-white">
                  <div class="flex items-start gap-2">
                    <span :class="['ni-cat', sentinelTypeClass(alert.type)]">{{ sentinelTypeLabel(alert.type) }}</span>
                    <div class="flex-1 min-w-0">
                      <div class="font-semibold text-gray-700">{{ alert.title }}</div>
                      <p class="text-gray-500 mt-1 leading-relaxed">{{ alert.description }}</p>
                      <div class="mt-2 flex gap-2 flex-wrap">
                        <button
                          v-if="alert.suggestion"
                          class="px-2 py-1 text-[11px] rounded bg-amber-100 text-amber-700 hover:bg-amber-200 transition-colors font-medium"
                          @click="handleAIFixAlert(alert)"
                        >🤖 AI修复</button>
                        <button
                          class="px-2 py-1 text-[11px] rounded bg-red-50 text-red-600 hover:bg-red-100 transition-colors font-medium border border-red-200"
                          @click="handleDeleteAlert(alert)"
                        >🗑️ 删除</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-else class="text-center py-8">
                <div class="text-3xl mb-2">✅</div>
                <div class="text-xs text-[#9c9690]">本章通过哨兵检查</div>
                <div class="text-[11px] text-[#d1d5db] mt-1">点击上方按钮重新扫描</div>
              </div>
            </div>
          </template>

          <!-- 伏笔回收 -->
          <template v-else-if="activeAiTab === 'foreshadow'">
            <div class="flex flex-col gap-2">
              <div class="flex items-center justify-between">
                <span class="text-xs font-semibold text-[#6b6560]">待回收伏笔</span>
                <button
                  class="px-2 py-1 text-[11px] rounded bg-[#dcfce7] text-[#16a34a] hover:bg-[#bbf7d0] transition-colors font-medium"
                  :disabled="pendingForeshadowingsLoading"
                  @click="fetchPendingForeshadowings"
                >
                  {{ pendingForeshadowingsLoading ? '加载中...' : '🔄 刷新' }}
                </button>
              </div>

              <!-- 加载状态 -->
              <div v-if="pendingForeshadowingsLoading" class="space-y-1.5">
                <div v-for="n in 2" :key="n" class="p-2 rounded-lg border border-[#e8e3dc] animate-pulse">
                  <div class="h-3 bg-[#e8e3dc] rounded w-2/3 mb-2"></div>
                  <div class="h-2 bg-[#f3efe8] rounded w-full mb-1"></div>
                  <div class="h-2 bg-[#f3efe8] rounded w-3/4"></div>
                </div>
              </div>

              <!-- 伏笔列表 -->
              <div v-else-if="pendingForeshadowings.length > 0" class="space-y-1.5">
                <div v-for="fs in pendingForeshadowings" :key="fs.id" class="p-2 rounded-lg border text-xs"
                  :class="fs.urgency === 'urgent' ? 'border-[#ef4444] bg-[#fef2f2]' : fs.urgency === 'warning' ? 'border-[#f59e0b] bg-[#fffbeb]' : 'border-[#e8e3dc] bg-white'">
                  <div class="flex items-start gap-2">
                    <span class="text-sm flex-shrink-0">{{ fs.urgency === 'urgent' ? '⚠️' : fs.urgency === 'warning' ? '📍' : '🔜' }}</span>
                    <div class="flex-1 min-w-0">
                      <div class="font-semibold text-[#6b6560]">{{ fs.name }}</div>
                      <p class="text-[#9c9690] mt-1 leading-relaxed">{{ fs.description || '暂无描述' }}</p>
                      <div class="text-[11px] text-[#9c9690] mt-1.5">
                        埋于第 {{ fs.chapterId }} 章 · 已过 {{ fs.passedChapters || 0 }} 章
                      </div>
                    </div>
                  </div>
                  <div class="mt-1.5 flex gap-1.5">
                    <!-- 融入生成下拉菜单 -->
                    <div class="relative" ref="foreshadowDropRef">
                      <button
                        class="px-2 py-1 text-[11px] rounded bg-[#fef3c7] text-[#92400e] hover:bg-[#fde68a] transition-colors font-medium"
                        @click="toggleForeshadowDropdown(fs)"
                      >✨ 融入生成 ▾</button>
                      <div
                        v-if="foreshadowDropdownVisible === fs.id"
                        class="absolute left-0 top-full mt-1 bg-white border border-[#e8e3dc] rounded-lg shadow-lg z-50 overflow-hidden min-w-[120px]"
                      >
                        <button
                          class="w-full px-3 py-2 text-[11px] text-left hover:bg-[#fef3c7] transition-colors"
                          @click="handleFullGenerate(fs)"
                        >📝 全量生成</button>
                        <button
                          class="w-full px-3 py-2 text-[11px] text-left hover:bg-[#dcfce7] hover:text-[#16a34a] transition-colors"
                          @click="handleAppendForeshadow(fs)"
                        >➕ 追加补写</button>
                      </div>
                    </div>
                    <button
                      class="px-2 py-1 text-[11px] rounded bg-[#e8e3dc] text-[#6b6560] hover:bg-[#d4cec6] transition-colors font-medium"
                      @click="handleMarkResolved(fs)"
                    >✅ 已回收</button>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-else class="text-center py-8">
                <div class="text-3xl mb-2">🎉</div>
                <div class="text-xs text-[#9c9690]">暂无待回收伏笔</div>
                <div class="text-[11px] text-[#d1d5db] mt-1">继续创作，保持情节推进</div>
              </div>
            </div>
          </template>

          <!-- 兜底（协同创作） -->
          <template v-else>
            <AICoWriter
              :project-id="store.currentProjectId"
              :chapter-index="currentChapterIndex + 1"
              :current-content="store.editorContent"
              @generated="onContentGenerated"
            />
          </template>
        </div>

        <!-- AI状态指示 -->
        <div class="px-3 py-2 border-t border-[#e8e3dc] flex items-center gap-1.5 text-xs text-[#9c9690]">
          <span class="w-2 h-2 rounded-full bg-[#0d9488]"></span>
          <span>AI 就绪 · DeepSeek v4</span>
        </div>
      </aside>
    </div>

    <!-- ═══ 删除确认弹窗 ═══ -->
    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/20" @click.self="showDeleteConfirm = false">
        <div class="bg-white rounded-xl p-6 shadow-lg max-w-sm w-full mx-4">
          <div class="text-sm font-semibold text-[#1a1815] mb-2">确认删除章节</div>
          <div class="text-xs text-[#6b6560] mb-4">确定要删除「{{ deleteTarget?.title || '未命名' }}」吗？此操作不可恢复。</div>
          <div class="flex justify-end gap-2">
            <button class="px-4 py-1.5 border border-[#e8e3dc] rounded-lg text-xs text-[#6b6560] hover:border-[#d97706] transition-colors" @click="showDeleteConfirm = false">取消</button>
            <button class="px-4 py-1.5 bg-[#be123c] text-white rounded-lg text-xs font-semibold hover:bg-[#9f1239] transition-colors" @click="confirmDelete">删除</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 哨兵告警删除确认弹窗 ═══ -->
    <Teleport to="body">
      <div v-if="showAlertDeleteConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/20" @click.self="showAlertDeleteConfirm = false">
        <div class="bg-white rounded-xl p-6 shadow-lg max-w-sm w-full mx-4">
          <div class="text-sm font-semibold text-[#1a1815] mb-2">{{ alertDeleteType === 'single' ? '确认删除告警' : '确认清空已处理告警' }}</div>
          <div class="text-xs text-[#6b6560] mb-4">
            {{ alertDeleteType === 'single' ? '确定要删除该告警吗？此操作不可恢复。' : '确定要删除所有已处理的告警吗？此操作不可恢复。' }}
          </div>
          <div class="flex justify-end gap-2">
            <button class="px-4 py-1.5 border border-[#e8e3dc] rounded-lg text-xs text-[#6b6560] hover:border-[#d97706] transition-colors" @click="showAlertDeleteConfirm = false">取消</button>
            <button class="px-4 py-1.5 bg-[#be123c] text-white rounded-lg text-xs font-semibold hover:bg-[#9f1239] transition-colors" @click="confirmAlertDelete">{{ alertDeleteType === 'single' ? '删除' : '清空' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 新建章节弹窗 ═══ -->
    <CreateChapterModal
      v-if="showCreateModal"
      :project-id="store.currentProjectId"
      :chapter-count="store.chapters.length"
      :existing-titles="existingTitles"
      @close="showCreateModal = false"
      @created="onChapterCreated"
    />

    <!-- ═══ 伏笔追加位置选择弹窗 ═══ -->
    <Teleport to="body">
      <div v-if="showAppendPositionModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/20" @click.self="showAppendPositionModal = false">
        <div class="bg-white rounded-xl p-6 shadow-lg max-w-md w-full mx-4">
          <div class="text-base font-semibold text-[#1a1815] mb-4">追加伏笔补写</div>
          <div class="text-sm text-[#6b6560] mb-2 font-medium">伏笔：{{ appendForeshadowTarget?.name }}</div>
          <p class="text-xs text-[#9c9690] mb-5 leading-relaxed bg-[#f9f7f4] p-3 rounded-lg">{{ appendForeshadowTarget?.description }}</p>
          <div class="text-sm text-[#6b6560] mb-3 font-medium">插入位置：</div>
          <div class="space-y-3 mb-5">
            <label class="flex items-center gap-3 cursor-pointer p-2 rounded-lg hover:bg-[#f9f7f4] transition-colors">
              <input type="radio" v-model="appendPosition" value="end" class="accent-[#d97706] w-4 h-4" />
              <span class="text-sm text-[#6b6560]">追加到章节末尾</span>
            </label>
            <label class="flex items-center gap-3 cursor-pointer p-2 rounded-lg hover:bg-[#f9f7f4] transition-colors">
              <input type="radio" v-model="appendPosition" value="cursor" class="accent-[#d97706] w-4 h-4" />
              <span class="text-sm text-[#6b6560]">插入到光标位置</span>
            </label>
            <label class="flex items-center gap-3 cursor-pointer p-2 rounded-lg hover:bg-[#f9f7f4] transition-colors">
              <input type="radio" v-model="appendPosition" value="auto" class="accent-[#d97706] w-4 h-4" />
              <span class="text-sm text-[#6b6560]">AI 智能插入</span>
            </label>
          </div>
          <div class="flex justify-end gap-3">
            <button class="px-5 py-2 border border-[#e8e3dc] rounded-lg text-sm text-[#6b6560] hover:border-[#d97706] transition-colors" @click="showAppendPositionModal = false">取消</button>
            <button class="px-5 py-2 bg-[#d97706] text-white rounded-lg text-sm font-semibold hover:bg-[#b45309] transition-colors" :disabled="appendLoading" @click="confirmAppendForeshadow">
              {{ appendLoading ? '生成中...' : '生成补写' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 伏笔追加结果预览弹窗 ═══ -->
    <Teleport to="body">
      <div v-if="showAppendResultModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/20" @click.self="showAppendResultModal = false">
        <div class="bg-white rounded-xl p-6 shadow-lg max-w-2xl w-full mx-4 max-h-[85vh] flex flex-col">
          <div class="text-base font-semibold text-[#1a1815] mb-3">伏笔补写结果预览</div>
          <div class="text-sm text-[#6b6560] mb-4">请预览新增内容（红色边框标记），确认后生效</div>
          <div class="flex-1 overflow-y-auto p-4 rounded-lg border-2 border-[#fecaca] bg-[#fef2f2] text-sm leading-relaxed whitespace-pre-wrap mb-5">
            {{ appendInsertedContent }}
          </div>
          <div class="flex justify-end gap-3">
            <button class="px-5 py-2 border border-[#e8e3dc] rounded-lg text-sm text-[#6b6560] hover:border-[#d97706] transition-colors" @click="cancelAppendForeshadow">取消</button>
            <button class="px-5 py-2 bg-[#e8e3dc] text-[#6b6560] rounded-lg text-sm font-semibold hover:bg-[#d4cec6] transition-colors" @click="modifyAppendForeshadow">修改</button>
            <button class="px-5 py-2 bg-[#16a34a] text-white rounded-lg text-sm font-semibold hover:bg-[#15803d] transition-colors" @click="acceptAppendForeshadow">接受</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- AI 写作面板-->
    <AiWritePanel
      :visible="showAiPanel"
      :project-id="store.currentProjectId"
      :chapter-id="store.currentChapterId"
      :chapter-title="store.chapterTitle"
      :chapter-index="aiPanelChapterIndex"
      @close="showAiPanel = false"
      @adopt="handleAiAdopt"
    />

    <!-- ═══ Toast 提示 ═══ -->
    <Teleport to="body">
      <Transition name="toast-fade">
        <div v-if="toastMsg" class="fixed top-6 right-6 z-50 px-4 py-2.5 rounded-lg shadow-md text-xs font-medium flex items-center gap-2"
          :class="toastType === 'success' ? 'bg-[#ecfdf5] text-[#065f46] border border-[#a7f3d0]' : 'bg-[#fef2f2] text-[#991b1b] border border-[#fecaca]'"
        >
          <span>{{ toastType === 'success' ? '✓' : '✕' }}</span>
          {{ toastMsg }}
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { useSettingsStore } from '@/stores/settings'
import { aiApi, sentinelApi, plotApi } from '@/api'
import CreateChapterModal from '@/components/common/CreateChapterModal.vue'
import AiWritePanel from '@/components/common/AiWritePanel.vue'
import AICoWriter from '@/components/editor/AICoWriter.vue'
import { sendNotification } from '@/services/notificationService'

const store = useNovelStore()
const settingsStore = useSettingsStore()

// ─── 编辑器引用 ───
const textareaRef = ref(null)
const chatMsgsRef = ref(null)

// ─── 编辑器外观设置（来自 settings store） ───
const editorFontSize = computed(() => settingsStore.fontSize || 16)
const editorLineHeight = computed(() => settingsStore.lineHeight || 1.65)

// ─── 自动保存控制（跟随 autoSave 设置） ───
watch(() => settingsStore.autoSave, (enabled) => {
  if (!enabled && autoSaveTimer) {
    clearTimeout(autoSaveTimer)
    autoSaveTimer = null
  }
})

// ─── 章节索引 ───
const currentChapterIndex = computed(() =>
  store.chapters.findIndex(c => c.id === store.currentChapterId)
)

// ─── 切换章节状态 ───
async function handleToggleStatus() {
  const pid = store.currentProjectId
  if (!pid || !store.currentChapterId) return
  try {
    const oldStatus = store.currentChapter?.status
    await store.toggleChapterStatus(pid, store.currentChapterId)
    // 刷新章节列表确保状态同步
    await store.fetchChapters(pid)
    const newStatus = store.currentChapter?.status
    showToast(newStatus === 'published' ? '章节已发布' : '已转为草稿', 'success')
  } catch (e) {
    showToast('状态切换失败', 'error')
  }
}

// ─── 进度条颜色 ───
const progressBarColor = computed(() => {
  if (store.wordProgress >= 100) return '#0d9488'
  if (store.wordProgress >= 50) return '#d97706'
  return '#9c9690'
})

// ─── 写作目标提醒 ───
let lastGoalNotified = false

watch(() => store.wordProgress, (progress) => {
  if (progress >= 100 && !lastGoalNotified) {
    lastGoalNotified = true
    sendNotification({
      type: 'writing_goal',
      title: '写作目标达成 ✨',
      description: `已完成今日目标 ${store.targetWords} 字，继续保持！`,
      severity: 'info',
      onSend: () => {
        store.addLocalNotification({
          type: 'goal',
          title: '写作目标达成 ✨',
          description: `已完成 ${store.targetWords} 字目标`,
          severity: 'info'
        })
        showToast('🎉 写作目标达成！', 'success')
      }
    })
  } else if (progress < 100) {
    lastGoalNotified = false
  }
})

// ─── AI Tab 状态 ───
const activeAiTab = ref('suggest')
const aiTabs = [
  { key: 'suggest', label: '智能提示' },
  { key: 'chat', label: '对话' },
  { key: 'cowrite', label: '协同创作' },
  { key: 'foreshadow', label: '伏笔回收' },
  { key: 'sentinel', label: '哨兵监测' }
]

watch(() => activeAiTab.value, (tab) => {
  if (tab === 'foreshadow') {
    fetchPendingForeshadowings()
  }
})


const placeholderText = '在此开始创作...\n\n让你的故事流淌在字里行间。\n\n支持 Markdown 语法：\n# 标题、**加粗**、*斜体*、> 引用'

// ─── 选中文本（用于润色） ───
const selectedText = ref('')
function updateSelectedText() {
  const ta = textareaRef.value
  if (ta && ta.selectionStart !== ta.selectionEnd) {
    selectedText.value = store.editorContent.substring(ta.selectionStart, ta.selectionEnd)
  } else {
    selectedText.value = ''
  }
}

// ─── 标题变更 ───
function onTitleChange(e) {
  store.updateChapterTitle(e.target.value)
  triggerAutoSave()
}

// ─── 内容变更 ───
function onContentChange(e) {
  store.updateEditorContent(e.target.value)
  updateSelectedText()
  triggerAutoSave()
}

// ─── 自动保存（防抖 3 秒，跟随 autoSave 设置） ───
let autoSaveTimer = null
function triggerAutoSave() {
  if (!settingsStore.autoSave) return
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => {
    performSave(true)  // 自动保存：不创建历史版本
  }, 3000)
}

async function performSave(silent = false) {
  const pid = store.currentProjectId
  if (!pid || !store.currentChapterId) return
  if (!store.chapters.find(c => c.id === store.currentChapterId)) return
  try {
    await store.saveCurrentChapter(pid, silent)
  } catch (e) {
    showToast('保存失败：' + (e.message || '网络错误'), 'error')
  }
}

// ─── 手动保存 ───
async function handleManualSave() {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  await performSave()
  if (store.lastSavedAt) {
    showToast('保存成功', 'success')
    // 版本保存通知（手动保存创建版本历史）
    sendNotification({
      type: 'version_save',
      title: '版本已保存',
      description: `章节「${store.chapterTitle || '未命名'}」已创建新版本`,
      severity: 'info',
      onSend: () => {
        store.addLocalNotification({
          type: 'version',
          title: '版本已保存',
          description: `章节「${store.chapterTitle || '未命名'}」已创建新版本`,
          severity: 'info'
        })
      }
    })
  }
}

// ─── 键盘快捷键 Ctrl+S / Cmd+S ───
function handleKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleManualSave()
  }
}

// ─── 章节操作 ───
async function handleSelectChapter(ch) {
  if (store.currentChapterId && store.currentChapterId !== ch.id) {
    await performSave()
  }
  store.selectChapter(ch)
  selectedText.value = ''
  nextTick(() => textareaRef.value?.focus())
}

// ─── 新建章节弹窗 ───
const showCreateModal = ref(false)
const existingTitles = computed(() => store.chapters.map(c => c.title).filter(Boolean))

// AI 写作面板
const showAiPanel = ref(false)
const aiPanelChapterIndex = computed(() => store.chapters.findIndex(c => c.id === store.currentChapterId) + 1)

function handleCreateChapter() {
  const pid = store.currentProjectId
  if (!pid) { showToast('请先选择作品', 'error'); return }
  if (store.currentChapterId) performSave()
  showCreateModal.value = true
}

async function onChapterCreated(data) {
  showCreateModal.value = false
  const pid = store.currentProjectId
  if (!pid) return
  try {
    const ch = await store.createChapter(pid, {
      title: data.title || '未命名章节',
      content: data.content || '',
      status: 'draft'
    })
    store.selectChapter(ch)
    selectedText.value = ''
    showToast('新章节已创建', 'success')
    nextTick(() => textareaRef.value?.focus())
  } catch (e) {
    showToast('创建章节失败：' + (e.message || '网络错误'), 'error')
  }
}

// ─── 删除章节 ───
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)

function handleDeleteChapter(ch) {
  deleteTarget.value = ch
  showDeleteConfirm.value = true
}

async function confirmDelete() {
  const pid = store.currentProjectId
  const ch = deleteTarget.value
  if (!pid || !ch) return
  try {
    await store.deleteChapter(pid, ch.id)
    showDeleteConfirm.value = false
    deleteTarget.value = null
    showToast('章节已删除', 'success')
  } catch (e) {
    showToast('删除失败：' + (e.message || '网络错误'), 'error')
  }
}

// ─── AI 扩写 ───
function handleAiExpand() {
  if (!store.currentChapterId) return
  showAiPanel.value = true
}

function handleAiAdopt(gc) {
  store.updateEditorContent(gc)
  triggerAutoSave()
  showAiPanel.value = false
  showToast("AI 内容已采纳", "success")
}

// ─── 协同创作生成回调 ───
function onContentGenerated(result) {
  const content = typeof result === 'string' ? result : (result?.content || result?.text || '')
  if (content) {
    store.updateEditorContent(store.editorContent + '\n\n' + content)
    triggerAutoSave()
    showToast('章节内容已生成并追加', 'success')
  }
}

// ─── AI 润色 ───
async function handleAiPolish() {
  const pid = store.currentProjectId
  if (!pid || !selectedText.value) return
  try {
    const result = await aiApi.chat(pid,
      `请润色以下小说片段，使其更加生动流畅，保持原意和风格：\n\n${selectedText.value}`
    )
    const polishedText = typeof result === 'string' ? result : (result.content || result.text || '')
    if (polishedText) {
      const ta = textareaRef.value
      if (ta) {
        const start = ta.selectionStart
        const end = ta.selectionEnd
        const newContent = store.editorContent.substring(0, start) + polishedText.trim() + store.editorContent.substring(end)
        store.updateEditorContent(newContent)
      } else {
        store.updateEditorContent(store.editorContent.replace(selectedText.value, polishedText.trim()))
      }
      selectedText.value = ''
      triggerAutoSave()
      showToast('润色完成', 'success')
    }
  } catch (e) {
    showToast('润色失败：' + (e.message || '网络错误'), 'error')
  }
}

// ─── AI 对话 ───
const chatInput = ref('')
const chatLoading = ref(false)
const chatMessages = ref([
  { role: 'ai', text: '你好！我是AI写作助手。你可以让我帮你写场景、润色文字、构思情节。生成的内容会自动插入编辑器光标位置。' }
])

// ─── 伏笔回收 ───
const pendingForeshadowings = ref([])
const pendingForeshadowingsLoading = ref(false)
const foreshadowDropdownVisible = ref(null)
const foreshadowDropRef = ref(null)

// 伏笔追加补写相关状态
const showAppendPositionModal = ref(false)
const showAppendResultModal = ref(false)
const appendForeshadowTarget = ref(null)
const appendPosition = ref('end')
const appendLoading = ref(false)
const appendInsertedContent = ref('')
const appendFullContent = ref('')
const appendInsertIndex = ref(0)

async function fetchPendingForeshadowings() {
  const pid = store.currentProjectId
  if (!pid) return
  pendingForeshadowingsLoading.value = true
  try {
    const currentCh = currentChapterIndex.value + 1
    const list = await plotApi.listUrgentForeshadowing(pid, currentCh)
    pendingForeshadowings.value = (list || []).map(f => {
      const buriedIn = f.chapterId || 1
      const passed = currentCh - buriedIn
      let urgency = 'normal'
      if (passed >= 3) urgency = 'urgent'
      else if (passed >= 1) urgency = 'warning'
      return { ...f, passedChapters: passed, urgency }
    })
  } catch (e) {
    console.error('获取伏笔失败：', e)
  } finally {
    pendingForeshadowingsLoading.value = false
  }
}

function toggleForeshadowDropdown(fs) {
  foreshadowDropdownVisible.value = foreshadowDropdownVisible.value === fs.id ? null : fs.id
}

function handleFullGenerate(fs) {
  foreshadowDropdownVisible.value = null
  const instruction = `【伏笔回收任务】请在本章中自然融入并回收伏笔「${fs.name}」：${fs.description || ''}`
  navigator.clipboard.writeText(instruction).then(() => {
    showToast('伏笔回收指令已复制到剪贴板，请在"协同创作"中粘贴使用', 'success')
  }).catch(() => {
    showToast('伏笔回收指令已准备，请在"协同创作"中使用', 'success')
  })
  activeAiTab.value = 'cowrite'
}

function handleAppendForeshadow(fs) {
  foreshadowDropdownVisible.value = null
  appendForeshadowTarget.value = fs
  appendPosition.value = 'end'
  showAppendPositionModal.value = true
}

async function confirmAppendForeshadow() {
  if (!appendForeshadowTarget.value || !store.currentChapterId) return
  appendLoading.value = true
  try {
    const pid = store.currentProjectId
    const chapterId = store.currentChapterId
    const currentContent = store.editorContent || ''
    const cursorPos = textareaRef.value?.selectionStart || currentContent.length
    const token = localStorage.getItem('token')

    const res = await fetch(`/api/projects/${pid}/chapters/${chapterId}/append-foreshadow`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({
        foreshadowingId: appendForeshadowTarget.value.id,
        foreshadowingTitle: appendForeshadowTarget.value.name,
        foreshadowingDescription: appendForeshadowTarget.value.description || '',
        originalContent: currentContent,
        insertPosition: appendPosition.value,
        cursorPosition: appendPosition.value === 'cursor' ? cursorPos : null,
        projectId: pid
      })
    })
    const result = await res.json()
    if (result.code === 200 || result.code === 0) {
      appendInsertedContent.value = result.data.insertedContent || ''
      appendFullContent.value = result.data.fullContent || ''
      appendInsertIndex.value = result.data.insertPosition || currentContent.length
      showAppendPositionModal.value = false
      showAppendResultModal.value = true
    } else {
      showToast('生成失败：' + (result.message || '未知错误'), 'error')
    }
  } catch (e) {
    console.error('伏笔追加失败：', e)
    showToast('生成失败：' + (e.message || '网络错误'), 'error')
  } finally {
    appendLoading.value = false
  }
}

function acceptAppendForeshadow() {
  store.editorContent = appendFullContent.value
  if (textareaRef.value) {
    textareaRef.value.value = appendFullContent.value
    textareaRef.value.setSelectionRange(appendInsertIndex.value, appendInsertIndex.value + appendInsertedContent.value.length)
    textareaRef.value.focus()
  }
  showAppendResultModal.value = false
  showToast('伏笔已成功融入章节', 'success')
  pendingForeshadowings.value = pendingForeshadowings.value.filter(f => f.id !== appendForeshadowTarget.value?.id)
}

function cancelAppendForeshadow() {
  showAppendResultModal.value = false
  appendInsertedContent.value = ''
  appendFullContent.value = ''
}

function modifyAppendForeshadow() {
  showAppendResultModal.value = false
  appendPosition.value = 'auto'
  showAppendPositionModal.value = true
}

async function handleMarkResolved(fs) {
  try {
    await plotApi.updateForeshadowing(store.currentProjectId, fs.id, { ...fs, status: 'resolved' })
    pendingForeshadowings.value = pendingForeshadowings.value.filter(f => f.id !== fs.id)
    showToast('已标记为已回收', 'success')
  } catch (e) {
    showToast('标记失败：' + (e.message || '网络错误'), 'error')
  }
}

async function handleSendChat() {
  const text = chatInput.value.trim()
  if (!text || chatLoading.value) return
  const pid = store.currentProjectId
  if (!pid) return

  chatMessages.value.push({ role: 'user', text })
  chatInput.value = ''
  chatLoading.value = true

  nextTick(() => {
    if (chatMsgsRef.value) chatMsgsRef.value.scrollTop = chatMsgsRef.value.scrollHeight
  })

  try {
    const contextSnippet = store.editorContent.substring(0, 800)
    const result = await aiApi.chat(pid,
      `当前小说内容摘要：${contextSnippet}\n\n用户请求：${text}\n\n请根据请求生成内容。如果是写作请求，请直接输出可用的文本。`
    )
    const aiText = typeof result === 'string' ? result : (result.content || result.text || '抱歉，我无法处理该请求。')
    chatMessages.value.push({ role: 'ai', text: aiText })

    if (text.includes('写') || text.includes('生成') || text.includes('创作') || text.includes('续写')) {
      const ta = textareaRef.value
      const cursorPos = ta ? ta.selectionStart : store.editorContent.length
      const before = store.editorContent.substring(0, cursorPos)
      const after = store.editorContent.substring(cursorPos)
      store.updateEditorContent(before + '\n\n' + aiText.trim() + after)
      triggerAutoSave()
      showToast('AI 生成内容已插入编辑器', 'success')
    }
  } catch (e) {
    chatMessages.value.push({ role: 'ai', text: '抱歉，请求失败：' + (e.message || '网络错误') })
  } finally {
    chatLoading.value = false
    nextTick(() => {
      if (chatMsgsRef.value) chatMsgsRef.value.scrollTop = chatMsgsRef.value.scrollHeight
    })
  }
}

// ─── AI 智能提示 ───
const aiTips = ref([])
let suggestTimer = null

watch(() => store.editorContent, () => {
  if (suggestTimer) clearTimeout(suggestTimer)
  if (store.editorContent.length < 50) {
    aiTips.value = []
    return
  }
  suggestTimer = setTimeout(() => {
    handleRefreshSuggestions()
  }, 4000)
}, { immediate: false })

async function handleRefreshSuggestions() {
  const pid = store.currentProjectId
  if (!pid || store.editorContent.length < 50) return
  store.aiSuggestLoading = true
  try {
    const result = await aiApi.chat(pid,
      `请分析以下小说片段，给出3条写作建议。每条建议包含：类型（伏笔/感官描写/节奏分析/人物一致性）、标题、具体建议内容。以JSON格式返回：[{"icon":"🎯","title":"标题","content":"建议内容","level":"warn|info|success"}]。\n\n小说内容：\n${store.editorContent.substring(0, 1500)}`
    )

    let suggestions = []
    const rawText = typeof result === 'string' ? result : (result.content || result.text || '')
    try {
      const jsonMatch = rawText.match(/\[[\s\S]*\]/)
      if (jsonMatch) {
        suggestions = JSON.parse(jsonMatch[0])
      }
    } catch {
      suggestions = [
        { id: Date.now(), icon: '📝', title: '继续创作', content: '当前内容较少，建议继续完善主体内容后再获取详细建议。', level: 'info' }
      ]
    }

    aiTips.value = suggestions.map((s, i) => ({
      id: Date.now() + i,
      icon: s.icon || '💡',
      title: s.title || '写作建议',
      content: s.content || '',
      level: s.level || 'info',
      applied: false
    }))
  } catch (e) {
    if (aiTips.value.length === 0) {
      aiTips.value = [
        { id: Date.now(), icon: '📝', title: '开始写作', content: '继续创作后AI会自动提供建议。', level: 'info', applied: false }
      ]
    }
  } finally {
    store.aiSuggestLoading = false
  }
}

function handleApplySuggestion(tip) {
  if (tip.applied) return
  const newContent = store.editorContent + '\n\n// AI建议：' + tip.content + '\n'
  store.updateEditorContent(newContent)
  tip.applied = true
  triggerAutoSave()
  showToast('建议已应用', 'success')
}

function handleDismissSuggestion(tip) {
  aiTips.value = aiTips.value.filter(t => t.id !== tip.id)
}

function tipLevelBg(level) {
  if (level === 'warn') return '#fffbeb'
  if (level === 'info') return '#f0f9ff'
  return '#ecfdf5'
}
function tipLevelBorder(level) {
  if (level === 'warn') return '#fcd34d'
  if (level === 'info') return '#bae6fd'
  return '#a7f3d0'
}
function tipLevelColor(level) {
  if (level === 'warn') return '#92400e'
  if (level === 'info') return '#0c4a6e'
  return '#065f46'
}

// ─── 哨兵监测 ───
const chapterAlerts = ref([])
const chapterAlertsLoading = ref(false)
const sentinelExpanded = ref(false)

async function fetchChapterAlerts() {
  const chapter = store.currentChapter
  if (!chapter || !chapter.id) {
    chapterAlerts.value = []
    return
  }
  chapterAlertsLoading.value = true
  try {
    const pid = store.currentProjectId
    const res = await sentinelApi.checkChapterLightweight(pid, chapter.id)
    chapterAlerts.value = res.data || []
  } catch (e) {
    console.error('获取章节告警失败:', e)
    chapterAlerts.value = []
  } finally {
    chapterAlertsLoading.value = false
  }
}

function sentinelTypeLabel(type) {
  const map = {
    foreshadowing: '伏笔',
    logic: '逻辑',
    character: '人物',
    rhythm: '节奏',
    normal: '正常'
  }
  return map[type] || type || '未知'
}

function sentinelTypeClass(type) {
  const map = {
    foreshadowing: 'foreshadowing',
    logic: 'logic',
    character: 'character',
    rhythm: 'rhythm',
    normal: 'normal'
  }
  return map[type] || 'normal'
}

async function handleAIFixAlert(alert) {
  if (!alert.suggestion) {
    showToast('无修复建议', 'warning')
    return
  }
  try {
    const pid = store.currentProjectId
    const direction = `请根据以下哨兵告警修复内容：${alert.suggestion}\n\n原文：${store.editorContent.slice(0, 500)}...`
    const result = await aiApi.chat(pid, direction)
    const fixedText = typeof result === 'string' ? result : (result.content || result.text || '')
    if (fixedText) {
      store.updateEditorContent(fixedText)
      triggerAutoSave()
      showToast('已根据哨兵建议润色', 'success')
    }
  } catch (e) {
    showToast('修复失败：' + (e.message || '网络错误'), 'error')
  }
}

async function handleScanChapter() {
  await fetchChapterAlerts()
  sentinelExpanded.value = true
}

// ─── 哨兵告警删除 ───
const showAlertDeleteConfirm = ref(false)
const alertDeleteType = ref('single')
const alertDeleteTarget = ref(null)

function handleDeleteAlert(alert) {
  alertDeleteType.value = 'single'
  alertDeleteTarget.value = alert
  showAlertDeleteConfirm.value = true
}

function handleClearResolvedAlerts() {
  alertDeleteType.value = 'resolved'
  alertDeleteTarget.value = null
  showAlertDeleteConfirm.value = true
}

async function confirmAlertDelete() {
  const pid = store.currentProjectId
  if (!pid) return
  try {
    if (alertDeleteType.value === 'single') {
      await sentinelApi.deleteAlert(pid, alertDeleteTarget.value.id)
      chapterAlerts.value = chapterAlerts.value.filter(a => a.id !== alertDeleteTarget.value.id)
      showToast('告警已删除', 'success')
    } else {
      await sentinelApi.clearResolvedAlerts(pid)
      chapterAlerts.value = chapterAlerts.value.filter(a => a.status !== 'resolved' && a.status !== '已处理')
      showToast('已处理告警已清空', 'success')
    }
  } catch (e) {
    showToast('删除失败：' + (e.message || '网络错误'), 'error')
  } finally {
    showAlertDeleteConfirm.value = false
    alertDeleteTarget.value = null
  }
}

// ─── Toast ───
const toastMsg = ref('')
const toastType = ref('success')
let toastTimer = null
function showToast(msg, type = 'success') {
  toastMsg.value = msg
  toastType.value = type
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastMsg.value = '' }, 2500)
}

// ─── 格式化 ───
function formatNumber(n) {
  return (n || 0).toLocaleString()
}

// ─── 组件初始化 ───
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)

  // 从路由参数获取 chapterId
  const route = useRoute()
  const routeChapterId = route.query.chapterId || route.params.chapterId

  if (routeChapterId && store.chapters.length > 0) {
    const ch = store.chapters.find(c => c.id === String(routeChapterId))
    if (ch) {
      store.selectChapter(ch)
      nextTick(() => textareaRef.value?.focus())
    }
  }

  // 如果章节已加载但未选中，自动选第一篇
  if (store.chapters.length > 0 && !store.currentChapterId) {
    store.selectChapter(store.chapters[0])
  }
})

// ─── 等待章节加载完成后自动选中 ───
watch(() => store.chapters.length, (count) => {
  if (count === 0 || store.currentChapterId) return

  // 优先使用路由参数指定的章节
  const route = useRoute()
  const routeChapterId = route.query.chapterId || route.params.chapterId
  if (routeChapterId) {
    const ch = store.chapters.find(c => c.id === String(routeChapterId))
    if (ch) {
      store.selectChapter(ch)
      nextTick(() => textareaRef.value?.focus())
      return
    }
  }

  // 无路由参数时自动选中第一篇
  store.selectChapter(store.chapters[0])
})

// ─── 组件销毁前自动保存 ───
onBeforeUnmount(async () => {
  document.removeEventListener('keydown', handleKeydown)
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  if (suggestTimer) clearTimeout(suggestTimer)
  if (toastTimer) clearTimeout(toastTimer)
  if (store.currentChapterId && store.currentProjectId) {
    await performSave()
  }
})
</script>

<style scoped>
.editor-root {
  animation: fadeSlideIn 0.3s ease;
}

/* ═══ 编辑器主题 ═══ */
.editor-theme-warm-ivory { background: #faf7f2; color: #1a1815; }
.editor-theme-warm-ivory textarea { background: transparent; color: #1a1815; }

/* 暗色主题 — 覆盖所有硬编码亮色 */
.editor-theme-dark {
  background: #1a1a2e;
  color: #e8e3dc;
}
.editor-theme-dark textarea { background: transparent; color: #e8e3dc; }

/* 顶部工具栏 */
.editor-theme-dark :deep(.border-b), 
.editor-theme-dark :deep(.border-t),
.editor-theme-dark :deep(.border-l),
.editor-theme-dark :deep(.border-r),
.editor-theme-dark :deep([class*="border-"]) {
  border-color: #3a3a52 !important;
}
.editor-theme-dark [class*="bg-\[#faf8f5\]"],
.editor-theme-dark [style*="background: #faf8f5"],
.editor-theme-dark [class*="bg-\[#faf7f2\]"] {
  background: #252540 !important;
}
.editor-theme-dark [class*="bg-white"] {
  background: #252540 !important;
}
.editor-theme-dark [class*="text-\[#9c9690\]"] {
  color: #a09aae !important;
}
.editor-theme-dark [class*="text-\[#6b6560\]"] {
  color: #c8c2d0 !important;
}
.editor-theme-dark [class*="text-\[#d4cec6\]"] {
  color: #6a6a82 !important;
}
.editor-theme-dark [class*="text-\[#92400e\]"] {
  color: #fbbf24 !important;
}
.editor-theme-dark [class*="hover\:bg-\[#f3efe8\]"]:hover {
  background: #3a3a52 !important;
}
.editor-theme-dark [class*="hover\:bg-white"]:hover {
  background: #3a3a52 !important;
}

/* 暗色主题 — 左侧栏章节列表 */
.editor-theme-dark aside:first-of-type {
  background: #1e1e3a !important;
}
.editor-theme-dark aside:first-of-type [class*="bg-\[#fef3c7\]"] {
  background: #374151 !important;
}
.editor-theme-dark aside:first-of-type [class*="text-\[#92400e\]"] {
  color: #fbbf24 !important;
}

/* 暗色主题 — 右侧 AI 助手栏 */
.editor-theme-dark aside.w-64 {
  background: #1e1e3a !important;
}
.editor-theme-dark aside.w-64 [class*="bg-white"] {
  background: #252540 !important;
}
.editor-theme-dark aside.w-64 [class*="hover\:bg-\[#faf8f5\]"]:hover {
  background: #3a3a52 !important;
}

/* 暗色主题 — AI 建议卡片 */
.editor-theme-dark [class*="rounded-lg"][class*="border"] > div:not([class*="bg-"]) {
  background: #252540;
}

/* 暗色主题 — 底部状态栏 */
.editor-theme-dark [class*="border-t"]:last-of-type {
  background: #1e1e3a !important;
}
.editor-theme-parchment { background: #f5f0e8; color: #1a1815; }
.editor-theme-parchment textarea { background: transparent; color: #1a1815; }
.editor-theme-green { background: #e8f5e9; color: #1a1815; }
.editor-theme-green textarea { background: transparent; color: #1a1815; }

/* ═══ 写作模式布局 ═══ */
.mode-focus aside { display: none; }
.mode-focus main { max-width: 820px; margin: 0 auto; }
.mode-outline main { display: none; }

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.toast-fade-enter-active { transition: all 0.25s ease-out; }
.toast-fade-leave-active { transition: all 0.2s ease-in; }
.toast-fade-enter-from { opacity: 0; transform: translateX(20px); }
.toast-fade-leave-to { opacity: 0; transform: translateX(20px); }

.editor-root ::-webkit-scrollbar { width: 4px; }
.editor-root ::-webkit-scrollbar-track { background: transparent; }
.editor-root ::-webkit-scrollbar-thumb { background: #d4cec6; border-radius: 4px; }
.editor-root ::-webkit-scrollbar-thumb:hover { background: #b8b0a8; }

textarea:focus { outline: none; }

@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 0.8s linear infinite; }
</style>