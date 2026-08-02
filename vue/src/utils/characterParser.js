/**
 * 人物设定文本解析器
 * 解析粘贴的人物设定文本，提取结构化信息
 */

/**
 * 解析人物设定文本
 * @param {string} text - 粘贴的人物设定文本
 * @returns {Object} - { success: boolean, characters: Array, raw: string }
 */
export function parseCharacterText(text) {
  if (!text || !text.trim()) {
    return { success: false, characters: [], raw: '' }
  }

  const trimmed = text.trim()
  const characters = []

  // 检测是否为格式化的文本（包含标记的）
  const isFormatted = /^[#一二三四五六七八九十\d]+\s*[.、：:、]/.test(trimmed) ||
                      /^\d+\.\s*【/.test(trimmed) ||
                      /^第[一二三四五六七八九十\d]+/.test(trimmed)

  if (isFormatted) {
    // 使用结构化解析
    parseStructuredText(trimmed, characters)
  } else {
    // 简单文本，按段落分隔处理
    parseSimpleText(trimmed, characters)
  }

  return {
    success: characters.length > 0,
    characters,
    raw: text
  }
}

/**
 * 解析结构化文本（带编号和标题的格式）
 */
function parseStructuredText(text, characters) {
  const lines = text.split('\n')
  let currentChar = null
  let currentSection = ''
  let currentContent = []

  const sectionTitles = [
    '基础信息', '人物画像', '外貌', '衣着', '标志物',
    '性格特征', '核心', '正面', '负面', '反差',
    '弧光轨迹', '经典台词', '隐藏设定', '特殊能力',
    '配角群像', '人物关系网络', '人物一句话速记'
  ]

  function isChapterTitle(line) {
    return /^[一二三四五六七八九十]+[、:：]\s*[\u4e00-\u9fa5]/.test(line.trim())
  }

  function isCharacterTitle(line) {
    const trimmed = line.trim()
    return /^\d+[.、:：]\s*.+/.test(trimmed) && !line.includes('属性')
  }

  function extractNameFromTitle(titleLine) {
    const trimmed = titleLine.trim()
    const match = trimmed.match(/^\d+[.、:：]\s*(.+?)(?:\s*[·•·、]\s*(.+))?$/)
    if (match) {
      const mainName = match[1].trim()
      const subtitle = match[2] ? match[2].trim() : ''
      return subtitle ? `${mainName} · ${subtitle}` : mainName
    }
    return trimmed
  }

  function isSectionTitle(line) {
    const trimmed = line.trim()
    if (!trimmed) return false
    if (/^属性\s+内容$/.test(trimmed)) return false
    if (sectionTitles.some(t => trimmed === t || trimmed === `【${t}】`)) return true
    if (trimmed.length < 10 && trimmed === line.trim()) {
      const lineIndex = lines.indexOf(line)
      if (lineIndex < lines.length - 1) {
        const nextLine = lines[lineIndex + 1].trim()
        if (nextLine.includes('属性') && nextLine.includes('内容')) return true
      }
    }
    return false
  }

  function finalizeCharacter() {
    if (currentChar && currentContent.length > 0) {
      const fullText = currentContent.join('\n')
      currentChar.name = currentChar.name || extractNameFromBlock(fullText) || '未命名人物'
      currentChar.role = extractRole(fullText)
      currentChar.age = extractField(fullText, ['年龄'])
      currentChar.identity = extractField(fullText, ['身份'])
      currentChar.realm = extractField(fullText, ['境界'])
      currentChar.personality = extractPersonality(fullText)
      currentChar.appearance = extractAppearance(fullText)
      currentChar.backstory = extractBackstory(fullText)
      currentChar.abilities = extractAbilities(fullText)
      currentChar.quotes = extractQuotes(fullText)
      currentChar.hiddenSettings = extractHiddenSettings(fullText)
      currentChar.raw = fullText
      characters.push(currentChar)
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const trimmed = line.trim()

    if (!trimmed) continue
    if (isChapterTitle(trimmed)) continue

    if (isCharacterTitle(trimmed)) {
      finalizeCharacter()
      const name = extractNameFromTitle(trimmed)
      currentChar = { name }
      currentContent = [trimmed]
      currentSection = ''
      continue
    }

    if (currentChar) {
      currentContent.push(line)
    }
  }

  finalizeCharacter()

  if (characters.length === 0) {
    parseSimpleText(text, characters)
  }
}

/**
 * 解析简单文本
 */
function parseSimpleText(text, characters) {
  // 按两个以上换行分割段落
  const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim())

  for (const para of paragraphs) {
    const trimmed = para.trim()
    if (!trimmed || trimmed.length < 10) continue

    // 尝试提取人物名称（第一行或加粗/标记的内容）
    const firstLine = trimmed.split('\n')[0]
    const name = extractNameFromBlock(trimmed) || firstLine.replace(/^[*_#]+|[*_#]+$/g, '').trim()

    if (name && name.length < 20 && !isSectionTitle(name)) {
      characters.push({
        name: name.slice(0, 50),
        role: '待定',
        personality: trimmed.slice(0, 200),
        raw: trimmed
      })
    }
  }
}

/**
 * 从块中提取名称
 */
function extractNameFromBlock(block) {
  // 匹配 "林彻 · 凡骨改写者" 或 "林彻" 格式
  const match = block.match(/([^\n【】\[\]()（）\s]{2,20})(?:\s*[·•·、]\s*(.+))?/)
  if (match) {
    return match[2] ? `${match[1].trim()} · ${match[2].trim()}` : match[1].trim()
  }
  return null
}

/**
 * 判断是否为章节/标题
 */
function isSectionTitle(text) {
  const sectionKeywords = ['第一章', '第2章', '基础信息', '人物画像', '外貌', '性格', '弧光', '经典台词', '隐藏设定', '特殊能力', '配角', '人物关系', '关系网络']
  return sectionKeywords.some(kw => text.includes(kw))
}

/**
 * 提取字段值
 */
function extractField(text, fieldNames) {
  for (const fieldName of fieldNames) {
    // 匹配 "属性	内容" 表格格式
    const tableMatch = text.match(new RegExp(`${fieldName}\\s*[\\t|:|：]\\s*(.+)`))
    if (tableMatch) return tableMatch[1].trim()

    // 匹配 "【字段名】内容" 格式
    const bracketMatch = text.match(new RegExp(`【${fieldName}】\\s*([^【】\\n]+)`))
    if (bracketMatch) return bracketMatch[1].trim()

    // 匹配 "- 字段名: 内容" 格式
    const bulletMatch = text.match(new RegExp(`[-*]\\s*${fieldName}[\\t:\\s]+(.+)`))
    if (bulletMatch) return bulletMatch[1].trim()
  }
  return ''
}

/**
 * 提取人物角色
 */
function extractRole(text) {
  const roles = ['主角', '男主', '女主', '反派', '配角', '导师', '盟友', '对手', '引路人', '守护者', '大长老', '祖', '少主', '盟主', '僧', '生']
  for (const role of roles) {
    if (text.includes(role)) {
      if (role === '主角' || role === '男主' || role === '女主') return '主角'
      if (role === '反派' || role === '对手') return '反派'
      if (role === '导师' || role === '引路人' || role === '守护者') return '导师'
      if (role === '盟友' || role === '少主') return '盟友'
      return '配角'
    }
  }
  return '待定'
}

/**
 * 提取性格特征
 */
function extractPersonality(text) {
  const personalityPatterns = [
    /性格特征[：:\n]([\s\S]+?)(?=弧光|经典台词|外貌|$)/,
    /核心[：:\n]([\s\S]+?)(?=正面|负面|反差|$)/,
    /外貌[：:\n]([\s\S]+?)(?=衣着|标志物|性格|$)/,
  ]

  for (const pattern of personalityPatterns) {
    const match = text.match(pattern)
    if (match) {
      return match[1].trim().slice(0, 300)
    }
  }

  // 返回包含性格关键词的段落
  const lines = text.split('\n')
  for (const line of lines) {
    if (line.includes('性格') || line.includes('正面') || line.includes('核心')) {
      return line.replace(/^[\s\d.、：:#*【】]+/, '').slice(0, 200)
    }
  }

  return ''
}

/**
 * 提取外貌描述
 */
function extractAppearance(text) {
  const match = text.match(/外貌[：:\n]([\s\S]+?)(?=衣着|标志物|性格|特征|$)/)
  if (match) return match[1].trim().slice(0, 300)

  const lines = text.split('\n')
  for (const line of lines) {
    if (line.includes('外貌') && line.length > 5) {
      return line.replace(/^[\s\d.、：:#*【】]+/, '').slice(0, 200)
    }
  }
  return ''
}

/**
 * 提取背景故事
 */
function extractBackstory(text) {
  const patterns = [
    /人物画像[：:\n]([\s\S]+?)(?=性格|特征|标志物|$)/,
    /背景[：:\n]([\s\S]+?)(?=性格|特征|$)/,
  ]

  for (const pattern of patterns) {
    const match = text.match(pattern)
    if (match) return match[1].trim().slice(0, 500)
  }
  return ''
}

/**
 * 提取特殊能力
 */
function extractAbilities(text) {
  const abilities = []
  const lines = text.split('\n')
  let inAbilities = false

  for (const line of lines) {
    if (line.includes('特殊能力') || line.includes('经典台词') || line.includes('能力')) {
      inAbilities = line.includes('特殊能力')
      continue
    }
    if (inAbilities && line.trim() && !line.includes('经典台词')) {
      const cleaned = line.replace(/^[\s\d.、：:#*【】-]+/, '').trim()
      if (cleaned) abilities.push(cleaned)
    }
  }

  return abilities.join('； ').slice(0, 300)
}

/**
 * 提取经典台词
 */
function extractQuotes(text) {
  const quotes = []
  const lines = text.split('\n')

  for (const line of lines) {
    const match = line.match(/["""'""『""](.+?)["""'""』""']/)
    if (match) {
      quotes.push(match[1])
    }
  }

  return quotes.slice(0, 3).join('； ')
}

/**
 * 提取隐藏设定
 */
function extractHiddenSettings(text) {
  const match = text.match(/隐藏设定[\s\S]+?$/)
  if (match) {
    return match[0].replace(/隐藏设定/, '').trim().slice(0, 300)
  }
  return ''
}

/**
 * 格式化解析结果为可读文本（用于AI生成）
 */
export function formatCharactersForAI(characters) {
  if (!characters || characters.length === 0) return ''

  return characters.map((ch, idx) => {
    const parts = [`【人物${idx + 1}】${ch.name}`]
    if (ch.role && ch.role !== '待定') parts.push(`角色：${ch.role}`)
    if (ch.age) parts.push(`年龄：${ch.age}`)
    if (ch.identity) parts.push(`身份：${ch.identity}`)
    if (ch.realm) parts.push(`境界：${ch.realm}`)
    if (ch.personality) parts.push(`性格：${ch.personality}`)
    if (ch.appearance) parts.push(`外貌：${ch.appearance}`)
    if (ch.abilities) parts.push(`能力：${ch.abilities}`)
    if (ch.quotes) parts.push(`台词：${ch.quotes}`)
    if (ch.hiddenSettings) parts.push(`隐藏设定：${ch.hiddenSettings}`)
    return parts.join('\n')
  }).join('\n\n')
}