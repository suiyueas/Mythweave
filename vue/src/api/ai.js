import { get, post, streamGet } from './request'

// ─── 工具函数：分析已有标题风格（纯前端计算，仅用于生成 Prompt 上下文） ───
function analyzeTitleStyle(titles) {
  if (!titles || titles.length === 0) {
    return { pattern: '无参考，自由创作', avgLength: 6, keywords: [], analysis: '暂无已有标题，请自由发挥' }
  }

  const recent = titles.slice(-5)
  const avgLen = Math.round(recent.reduce((s, t) => s + t.length, 0) / recent.length)

  // 提取高频关键词
  const allChars = recent.join('')
  const freq = {}
  const stopChars = '，,、。.！!？?「」""()（）的之与了是在不也吧吗呢'
  for (const ch of allChars) {
    if (stopChars.includes(ch) || ch.length === 0) continue
    freq[ch] = (freq[ch] || 0) + 1
  }
  const keywords = Object.entries(freq)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([ch]) => ch)

  // 分析风格模式
  let pattern = '意象化表达'
  if (recent.some(t => t.includes('的'))) pattern = '名词+的+名词，意象关联'
  if (recent.some(t => /^.{2,3}，.{2,3}$/.test(t))) pattern = '对仗式，意象对比'
  if (recent.every(t => t.length <= 5)) pattern = '精炼凝练，意象浓缩'
  if (recent.some(t => t.includes('?'))) pattern = '设问式，制造悬念'

  const analysis = [
    `近${recent.length}章标题平均 ${avgLen} 字`,
    keywords.length ? `核心意象词：${keywords.join('、')}` : '标题使用意象化表达',
    `风格模式：${pattern}`,
    `请与上一章「${recent[recent.length - 1]}」形成叙事递进，并延续此风格`
  ].join('；')

  return { pattern, avgLength: avgLen, keywords, analysis }
}

// ─── 清理 AI 返回的标题 ───
function cleanTitle(raw) {
  if (!raw || typeof raw !== 'string') return ''
  let cleaned = raw
    .replace(/^["「《]|["」》]$/g, '')
    .replace(/\n.*$/s, '')
    .trim()
    .replaceAll(/^[0-9]+\.\s*/g, '')
    .replaceAll(/^第.+章[：:]?\s*/g, '')
    .trim()
    .slice(0, 50)
  return cleaned
}

export const aiApi = {
  // ─── AI 生成章节标题 ───
  async generateTitle(projectId, { chapterIndex, direction, existingTitles, genre, tone, sentinelHints, context }) {
    const titles = existingTitles || []
    const style = analyzeTitleStyle(titles)

    // 前端只负责发送请求参数，Prompt 构建由后端完成
    const response = await post(`/api/projects/${projectId}/ai/generate-title`, {
      chapterIndex,
      direction: direction || '延续主线剧情，推动情节发展',
      genre: genre || '玄幻',
      tone: tone || '诗意意象',
      existingTitles: titles,
      styleAnalysis: style,
      sentinelHints: sentinelHints || []
    })

    // 兼容不同的返回格式
    const raw = typeof response === 'string' ? response : (response?.title || response?.content || response?.text || '')
    return cleanTitle(raw)
  },

  // ─── AI 流式生成章节内容 ───
  generateContentStream(projectId, params, onToken, onDone, onError) {
    const { chapterIndex, title, direction, existingContent, style } = params

    // 前端只负责发送请求参数，Prompt 构建和流式处理由后端完成
    return streamGet(
      `/api/projects/${projectId}/ai/stream/content`,
      {
        chapterIndex,
        title: title || '未命名章节',
        direction: direction || '延续故事主线，推动情节发展',
        existingContent: existingContent || '',
        style: style || '自然流畅'
      },
      (token) => {
        // 前端只负责传递 token，不做额外处理（由后端保证分段）
        onToken(token)
      },
      onDone,
      onError
    )
  },

  // ─── AI 对话（通用） ───
  chat(projectId, message) {
    return post(`/api/projects/${projectId}/ai/chat`, { message })
  },

  // ─── AI 流式对话 ───
  streamChat(projectId, params, onToken, onDone, onError) {
    // 兼容 message 和 userMessage 两种参数名
    const userMessage = params.userMessage || params.message || ''
    const novelTitle = params.novelTitle || ''
    const genre = params.genre || ''
    const currentChapter = params.currentChapter || ''
    const context = params.context || ''
    return streamGet(
      `/api/projects/${projectId}/ai/stream/chat`,
      { userMessage, novelTitle, genre, currentChapter, context },
      onToken,
      onDone,
      onError
    )
  },

  // ─── AI 润色（新增） ───
  async polish(projectId, { text, style, targetLength }) {
    return post(`/api/projects/${projectId}/ai/polish`, {
      text,
      style: style || '自然流畅',
      targetLength: targetLength || '保持原长度'
    })
  },

  // ─── AI 扩写（非流式，一次性生成完整内容） ───
  async expand(projectId, { chapterIndex, currentContent, direction, style }) {
    return post(`/api/projects/${projectId}/ai/expand`, {
      chapterIndex,
      currentContent: currentContent || '',
      direction: direction || '延续故事主线，丰富细节',
      style: style || '自然流畅'
    })
  },

  // ─── AI 协同创作（非流式，带完整上下文生成章节） ───
  async generateChapter(projectId, { chapterIndex, currentContent, context, direction, targetWords }) {
    return post(`/api/projects/${projectId}/ai/generate-chapter`, {
      chapterIndex,
      currentContent: currentContent || '',
      context,
      direction: direction || '',
      targetWords: targetWords || 2000
    })
  },

  // ─── AI 流式协同创作（带章节衔接上下文，逐字输出） ───
  generateChapterStream(projectId, { chapterIndex, title, existingContent, direction, style, targetWords }, onToken, onDone, onError) {
    return streamGet(
      `/api/projects/${projectId}/ai/stream/content`,
      {
        chapterIndex,
        title: title || '未命名章节',
        direction: direction || '延续故事主线，推动情节发展',
        existingContent: existingContent || '',
        style: style || '自然流畅',
        targetWords: targetWords || 2000
      },
      (token) => {
        onToken(token)
      },
      onDone,
      onError
    )
  }
}

export default aiApi