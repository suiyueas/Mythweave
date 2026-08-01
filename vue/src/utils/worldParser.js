/**
 * 世界观设定文本解析器
 * 解析粘贴的世界观设定文本，提取结构化信息
 */

/**
 * 解析世界观设定文本
 * @param {string} text - 粘贴的世界观设定文本
 * @returns {Object} - { success: boolean, sections: Array, raw: string }
 */
export function parseWorldText(text) {
  if (!text || !text.trim()) {
    return { success: false, sections: [], raw: '' }
  }

  const trimmed = text.trim()
  const sections = []

  // 检测是否包含主要章节标记
  const hasMainSections = /^[一二三四五六七八九十\d]+[.、：:、]/.test(trimmed) ||
                          /^第[一二三四五六七八九十\d]+/.test(trimmed) ||
                          /#{1,3}\s/.test(trimmed)

  if (hasMainSections) {
    parseStructuredWorldText(trimmed, sections)
  } else {
    parseSimpleWorldText(trimmed, sections)
  }

  return {
    success: sections.length > 0,
    sections,
    raw: text
  }
}

/**
 * 解析结构化文本
 */
function parseStructuredWorldText(text, sections) {
  // 按主要章节分割
  const chapterPatterns = [
    /^(第?[一二三四五六七八九十\d]+[章节部])\s*[:：]?\s*(.+)?$/gm,
    /^(\d+[.、])\s*(.+)$/gm,
  ]

  // 提取章节标题
  const lines = text.split('\n')
  let currentSection = null
  let currentContent = []

  for (const line of lines) {
    const trimmedLine = line.trim()
    if (!trimmedLine) {
      if (currentContent.length > 0) {
        currentContent.push('')
      }
      continue
    }

    // 检测是否为章节标题
    const chapterMatch = trimmedLine.match(/^(第?[一二三四五六七八九十\d]+[章节部])\s*[:：]?\s*(.*)$/)
    const numberMatch = trimmedLine.match(/^(\d+[.、:])\s*(.+)$/)
    const hashMatch = trimmedLine.match(/^(#{1,3})\s*(.+)$/)

    if (chapterMatch || numberMatch || hashMatch) {
      // 保存上一个章节
      if (currentSection && currentContent.length > 0) {
        sections.push({
          title: currentSection,
          content: currentContent.join('\n').trim(),
          level: getSectionLevel(currentSection)
        })
      }

      // 开始新章节
      const title = chapterMatch ? chapterMatch[2] || chapterMatch[1] :
                    numberMatch ? numberMatch[2] || numberMatch[1] :
                    hashMatch[2] || trimmedLine
      currentSection = title
      currentContent = []
    } else if (currentSection) {
      currentContent.push(trimmedLine)
    } else {
      // 没有章节标题的内容作为概述
      currentContent.push(trimmedLine)
    }
  }

  // 保存最后一个章节
  if (currentSection && currentContent.length > 0) {
    sections.push({
      title: currentSection,
      content: currentContent.join('\n').trim(),
      level: getSectionLevel(currentSection)
    })
  }

  // 如果没有提取到章节，尝试按段落分割
  if (sections.length === 0) {
    parseSimpleWorldText(text, sections)
  }
}

/**
 * 解析简单文本
 */
function parseSimpleWorldText(text, sections) {
  // 按两个以上换行分割
  const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim())

  for (const para of paragraphs) {
    const trimmed = para.trim()
    if (!trimmed || trimmed.length < 20) continue

    const firstLine = trimmed.split('\n')[0]
    const title = firstLine.replace(/^[*_#]+|[*_#]+$/g, '').trim().slice(0, 30)
    const content = trimmed.slice(firstLine.length).trim()

    if (title && content) {
      sections.push({
        title: title,
        content: content.slice(0, 500),
        level: 1
      })
    }
  }
}

/**
 * 获取章节级别
 */
function getSectionLevel(title) {
  if (/^[#]/.test(title)) return title.match(/^#+/)[0].length
  if (/^第[一二三四五六七八九十\d]+章/.test(title)) return 1
  if (/^第[一二三四五六七八九十\d]+节/.test(title)) return 2
  if (/^[一二三四五六七八九十]+[、:]/.test(title)) return 1
  return 1
}

/**
 * 提取特定字段
 */
export function extractWorldField(text, fieldName) {
  const patterns = [
    new RegExp(`${fieldName}\\s*[\\t|:|：|\\-]\\s*(.+?)(?=\\n[\\t|:|：\\-]|$)`, 'i'),
    new RegExp(`【${fieldName}】\\s*([^【】\\n]+)`, 'i'),
    new RegExp(`^${fieldName}[:：]\\s*(.+)$`, 'im'),
  ]

  for (const pattern of patterns) {
    const match = text.match(pattern)
    if (match) return match[1].trim()
  }
  return ''
}

/**
 * 提取地点列表
 */
export function extractLocations(text) {
  const locations = []
  const locationPatterns = [
    /^(.+?)[:：]\s*(描述|位于|是|在|.+$)/gm,
    /^[-*]\s*(.+?)(?:\s*[;:]\s*(.+))?$/gm,
  ]

  for (const pattern of locationPatterns) {
    let match
    while ((match = pattern.exec(text)) !== null) {
      const name = match[1].trim()
      if (name && name.length > 1 && name.length < 50 && !isCommonWord(name)) {
        locations.push({
          name,
          description: match[2]?.trim() || ''
        })
      }
    }
  }

  return locations.slice(0, 10)
}

/**
 * 提取势力列表
 */
export function extractFactions(text) {
  const factions = []
  const factionKeywords = ['骨殿', '异骨会', '凡骨盟', '门派', '家族', '组织', '势力']

  const lines = text.split('\n')
  for (const line of lines) {
    for (const keyword of factionKeywords) {
      if (line.includes(keyword)) {
        const name = extractNameFromLine(line, keyword)
        if (name && !factions.some(f => f.name === name)) {
          factions.push({
            name,
            description: line.replace(/^[*_\-\d.、：:#]+/, '').trim().slice(0, 100)
          })
        }
      }
    }
  }

  return factions.slice(0, 10)
}

/**
 * 提取历史纪元
 */
export function extractEras(text) {
  const eras = []
  const eraPattern = /^(第?[一二三四五六七八九十\d]+[纪世代])\s*[:：]?\s*(.+?)(?=\n|$)/gm

  let match
  while ((match = eraPattern.exec(text)) !== null) {
    eras.push({
      name: match[1],
      description: match[2].trim()
    })
  }

  return eras
}

/**
 * 从行中提取名称
 */
function extractNameFromLine(line, keyword) {
  const idx = line.indexOf(keyword)
  if (idx === -1) return null

  // 提取关键词前的名称
  const before = line.slice(0, idx).trim()
  const match = before.match(/([^，,、；;:\n]+)$/)
  return match ? match[1].trim() : before
}

/**
 * 判断是否为常见词
 */
function isCommonWord(text) {
  const commonWords = ['描述', '位于', '位于', '是', '这里', '这里', '以及', '以及', '其中', '其中']
  return commonWords.includes(text)
}

/**
 * 格式化世界观供AI使用
 */
export function formatWorldForAI(sections) {
  if (!sections || sections.length === 0) return ''

  return sections.map((sec, idx) => {
    return `【${sec.title}】\n${sec.content}`
  }).join('\n\n')
}

/**
 * 提取世界观核心要素
 */
export function extractWorldCore(text) {
  const core = {
    worldName: '',
    coreRules: [],
    locations: [],
    factions: [],
    history: []
  }

  // 提取世界名称
  const nameMatch = text.match(/世界名称[：:\s]*(.+?)(?=\n|$)/i)
  if (nameMatch) core.worldName = nameMatch[1].trim()

  // 提取核心法则
  const ruleMatches = text.match(/[第]?\d*[条款]?\s*法则[：:]([^\n]+)/gi)
  if (ruleMatches) {
    core.coreRules = ruleMatches.map(r => r.replace(/[第]?\d*[条款]?\s*法则[：:]/i, '').trim())
  }

  // 提取地点
  core.locations = extractLocations(text)

  // 提取势力
  core.factions = extractFactions(text)

  // 提取历史
  core.history = extractEras(text)

  return core
}