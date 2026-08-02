/**
 * 核心设定与大纲文本解析器
 * 解析粘贴的核心设定和大纲文本
 */

/**
 * 解析核心设定文本
 * @param {string} text - 粘贴的核心设定文本
 * @returns {Object} - { success: boolean, items: Array, raw: string }
 */
export function parseCoreSettingText(text) {
  if (!text || !text.trim()) {
    return { success: false, items: [], raw: '' }
  }

  const trimmed = text.trim()
  const items = []

  // 检测是否为结构化格式
  const isStructured = /^[#\-*\d]+[\s.、：:]/.test(trimmed) ||
                        /^[【\[].+[】\]]/.test(trimmed) ||
                        /^第?[一二三四五六七八九十\d]+[章节部]/.test(trimmed)

  if (isStructured) {
    parseStructuredCore(trimmed, items)
  } else {
    parseSimpleCore(trimmed, items)
  }

  return {
    success: items.length > 0,
    items,
    raw: text
  }
}

/**
 * 解析结构化核心设定
 */
function parseStructuredCore(text, items) {
  const lines = text.split('\n')
  let currentItem = null
  let currentContent = []

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) {
      if (currentContent.length > 0) currentContent.push('')
      continue
    }

    // 检测标题行
    const titleMatch = trimmed.match(/^([#\-*\d\s.、：:、]+)?(【?[\u4e00-\u9fa5a-zA-Z0-9\s·\-_【】\[\]（）\(\)]+}?)[：:：]?\s*(.*)$/)
    const isTitle = /^#{1,3}\s/.test(trimmed) ||
                    /^【.+】$/.test(trimmed) ||
                    /^[第][一二三四五六七八九十\d]+[章节部]/.test(trimmed) ||
                    (/^\d+[.、：:]/.test(trimmed) && trimmed.length < 50)

    if (isTitle) {
      if (currentItem && currentContent.length > 0) {
        items.push({
          title: currentItem,
          content: currentContent.join('\n').trim()
        })
      }
      const title = trimmed.replace(/^#+\s*/, '').replace(/^【/, '').replace(/】$/, '').trim()
      currentItem = title
      const afterTitle = trimmed.match(/[：:：]\s*(.+)$/)
      if (afterTitle) currentContent = [afterTitle[1]]
      else currentContent = []
    } else if (currentItem) {
      currentContent.push(trimmed)
    }
  }

  if (currentItem && currentContent.length > 0) {
    items.push({
      title: currentItem,
      content: currentContent.join('\n').trim()
    })
  }

  if (items.length === 0) {
    parseSimpleCore(text, items)
  }
}

/**
 * 解析简单核心设定
 */
function parseSimpleCore(text, items) {
  // 按段落分割
  const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim())

  for (const para of paragraphs) {
    const trimmed = para.trim()
    if (!trimmed || trimmed.length < 15) continue

    const firstLine = trimmed.split('\n')[0]
    const title = firstLine.replace(/^[*_#\s\d.、：:【】\[\]]+/, '').trim().slice(0, 30)

    if (title) {
      items.push({
        title: title,
        content: trimmed.slice(Math.min(firstLine.length, 50)).trim().slice(0, 300)
      })
    }
  }
}

/**
 * 解析大纲文本
 * @param {string} text - 粘贴的大纲文本
 * @returns {Object} - { success: boolean, acts: Array, chapters: Array, raw: string }
 */
export function parseOutlineText(text) {
  if (!text || !text.trim()) {
    return { success: false, acts: [], chapters: [], raw: '' }
  }

  const trimmed = text.trim()
  const result = {
    success: false,
    acts: [],
    chapters: [],
    raw: text
  }

  // 检测是否为幕/章节结构
  const isActStructure = /^第[一二三四五六七八九十\d]+幕/.test(trimmed) ||
                         /^[上下中]*(第[一二三四五六七八九十\d]+部)/.test(trimmed) ||
                         /^第一幕|第二幕|第三幕/.test(trimmed)

  // 检测是否为章节表格结构
  const isChapterTable = /章节\s*[^\n]*\s*标题\s*[^\n]*\s*关键事件/.test(trimmed) ||
                          /^\s*第?\d+章/.test(trimmed)

  if (isActStructure) {
    parseActStructure(trimmed, result)
  } else if (isChapterTable) {
    parseChapterTable(trimmed, result)
  } else {
    parseSimpleOutline(trimmed, result)
  }

  result.success = result.acts.length > 0 || result.chapters.length > 0
  return result
}

/**
 * 解析幕结构大纲
 */
function parseActStructure(text, result) {
  // 按幕分割
  const actPattern = /((?:第[一二三四五六七八九十\d]+幕|第一幕|第二幕|第三幕|(?:上|中|下)?第?[一二三四五六七八九十\d]+部)[^]*?)(?=(?:第[一二三四五六七八九十\d]+幕|$))/gi
  const matches = text.match(actPattern) || [text]

  for (const actBlock of matches) {
    if (!actBlock.trim()) continue

    const actLines = actBlock.split('\n')
    let actName = ''
    let actDesc = ''
    const chapters = []

    for (const line of actLines) {
      const trimmed = line.trim()
      if (!trimmed) continue

      // 检测幕标题
      if (/第[一二三四五六七八九十\d]+幕/.test(trimmed) || /(?:上|中|下)第?[一二三四五六七八九十\d]+部/.test(trimmed)) {
        actName = trimmed.replace(/[：:：].*$/, '').trim()
        actDesc = trimmed.match(/[：:：]\s*(.+)$/)?.[1] || ''
      }
      // 检测章节
      else if (/^第?\d+章/.test(trimmed)) {
        const chapterInfo = parseChapterLine(trimmed)
        if (chapterInfo) chapters.push(chapterInfo)
      }
      // 表格格式章节
      else if (/^\s*\d+\s+/.test(trimmed)) {
        const chapterInfo = parseTableChapter(trimmed)
        if (chapterInfo) chapters.push(chapterInfo)
      }
    }

    if (actName || chapters.length > 0) {
      result.acts.push({
        title: actName || '未命名',
        description: actDesc,
        chapters
      })
    }
  }
}

/**
 * 解析章节表格
 */
function parseChapterTable(text, result) {
  const lines = text.split('\n')
  let currentAct = null

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue

    // 检测章节行
    if (/^第?\d+章/.test(trimmed)) {
      const info = parseChapterLine(trimmed)
      if (info) {
        if (!currentAct) {
          currentAct = { title: '第一幕', chapters: [] }
          result.acts.push(currentAct)
        }
        currentAct.chapters.push(info)
        result.chapters.push(info)
      }
    }
    // 检测幕标题
    else if (/第[一二三四五六七八九十\d]+幕/.test(trimmed)) {
      currentAct = {
        title: trimmed.match(/第[一二三四五六七八九十\d]+幕/)?.[0] || trimmed,
        chapters: []
      }
      result.acts.push(currentAct)
    }
  }
}

/**
 * 解析简单大纲
 */
function parseSimpleOutline(text, result) {
  const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim())

  for (const para of paragraphs) {
    const trimmed = para.trim()
    if (!trimmed || trimmed.length < 20) continue

    const firstLine = trimmed.split('\n')[0]
    const title = extractTitle(trimmed)

    if (title) {
      result.chapters.push({
        number: result.chapters.length + 1,
        title: title,
        summary: trimmed.slice(Math.min(firstLine.length, 50)).trim().slice(0, 200)
      })
    }
  }

  // 如果识别到多个章节，合并为第一幕
  if (result.chapters.length > 0) {
    result.acts.push({
      title: '第一幕',
      description: '',
      chapters: result.chapters
    })
  }
}

/**
 * 解析章节行
 */
function parseChapterLine(line) {
  const trimmed = line.trim()
  if (!trimmed) return null

  // 匹配 "第1章	凡骨镇	林彻在..." 或 "第1章  凡骨镇  描述"
  // 支持制表符或空格分隔
  const match = trimmed.match(/^第?(\d+)章[：:、]?\s*([^\t\n]+)(?=\t|\s{2,})/)
  if (match) {
    const title = match[2].trim()
    // 提取摘要：找到标题结束位置后的所有内容
    const titleEndIndex = trimmed.indexOf(title) + title.length
    let summary = trimmed.slice(titleEndIndex).trim()
    // 去掉可能的前导分隔符
    if (summary.startsWith('\t') || summary.startsWith('  ')) {
      summary = summary.replace(/^[\t ]+/, '').trim()
    }
    return {
      number: parseInt(match[1]),
      title: title,
      summary: summary.slice(0, 150)
    }
  }
  return null
}

/**
 * 解析表格格式章节
 */
function parseTableChapter(line) {
  // 匹配 "1  凡骨镇  描述" 或 "1	凡骨镇	描述"（制表符或空格分隔）
  const trimmed = line.trim()
  const parts = trimmed.split(/\t|\s{2,}/)
  if (parts.length >= 2) {
    const num = parseInt(parts[0])
    if (!isNaN(num)) {
      return {
        number: num,
        title: parts[1].trim(),
        summary: parts.slice(2).join(' ').trim().slice(0, 150)
      }
    }
  }
  return null
}

/**
 * 提取标题
 */
function extractTitle(text) {
  const patterns = [
    /^第?[一二三四五六七八九十\d]+[章节部][：:]\s*(.+)/,
    /^【(.+?)】/,
    /^#\s*(.+)/
  ]

  for (const pattern of patterns) {
    const match = text.match(pattern)
    if (match) return match[1].trim().slice(0, 30)
  }

  return text.split('\n')[0].replace(/^[*_\s\d.、：:#]+/, '').trim().slice(0, 30)
}

/**
 * 格式化大纲供AI使用
 */
export function formatOutlineForAI(result) {
  if (!result || (!result.acts.length && !result.chapters.length)) return ''

  const parts = []

  if (result.acts.length > 0) {
    for (const act of result.acts) {
      parts.push(`【${act.title}】${act.description || ''}`)
      for (const ch of act.chapters) {
        parts.push(`第${ch.number}章 ${ch.title}：${ch.summary}`)
      }
      parts.push('')
    }
  } else {
    for (const ch of result.chapters) {
      parts.push(`第${ch.number}章 ${ch.title}：${ch.summary}`)
    }
  }

  return parts.join('\n')
}