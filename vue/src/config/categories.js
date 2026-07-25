// src/config/categories.js
// 统一分类配置 — 存储值用英文，展示值用中文

// ─── 世界观分类 ───
export const WORLD_CATEGORIES = [
  { value: 'geography', label: '🌍 地理版图', icon: '🌍' },
  { value: 'history', label: '📜 历史年表', icon: '📜' },
  { value: 'culture', label: '🏛️ 文化社会', icon: '🏛️' },
  { value: 'magic', label: '🔮 力量体系', icon: '🔮' },
  { value: 'technology', label: '⚙️ 科技文明', icon: '⚙️' },
  { value: 'races', label: '👥 种族设定', icon: '👥' },
  { value: 'religion', label: '🕊️ 信仰神明', icon: '🕊️' },
  { value: 'politics', label: '⚖️ 政治势力', icon: '⚖️' }
]

// ─── 人物分类 ───
export const CHARACTER_CATEGORIES = [
  { value: 'protagonist', label: '⭐ 主角' },
  { value: 'supporting', label: '👤 配角' },
  { value: 'antagonist', label: '🔥 反派' },
  { value: 'minor', label: '📖 次要角色' }
]

// ─── 情节分类 ───
export const PLOT_CATEGORIES = [
  { value: 'main', label: '🎯 主线' },
  { value: 'sub', label: '🌿 支线' },
  { value: 'hidden', label: '🔒 暗线' }
]

// ─── 灵感分类 ───
export const INSPIRATION_CATEGORIES = [
  { value: 'dialogue', label: '💬 对白灵感' },
  { value: 'scene', label: '🎬 场景描写' },
  { value: 'detail', label: '🔍 细节设定' },
  { value: 'reference', label: '📚 典故参考' },
  { value: 'character_insp', label: '👤 人物设定' },
  { value: 'plot_insp', label: '📖 情节构思' }
]

// ─── 通用工具函数 ───
export function getCategoryLabel(categories, value) {
  const found = categories.find(c => c.value === value)
  return found ? found.label : value
}

export function getCategoryOptions(categories) {
  return categories.map(c => ({ value: c.value, label: c.label }))
}

// ─── 标签颜色映射 ───
export const CATEGORY_COLOR_MAP = {
  // 世界观
  geography: { bg: '#dbeafe', text: '#1e40af' },
  history: { bg: '#fef3c7', text: '#92400e' },
  culture: { bg: '#fce7f3', text: '#9d174d' },
  magic: { bg: '#e0e7ff', text: '#3730a3' },
  technology: { bg: '#d1fae5', text: '#065f46' },
  races: { bg: '#f3e8ff', text: '#6b21a8' },
  religion: { bg: '#fff7ed', text: '#9a3412' },
  politics: { bg: '#ecfdf5', text: '#047857' },
  // 人物
  protagonist: { bg: '#fef9c3', text: '#854d0e' },
  supporting: { bg: '#d1fae5', text: '#065f46' },
  antagonist: { bg: '#fee2e2', text: '#991b1b' },
  minor: { bg: '#f3f4f6', text: '#4b5563' },
  // 情节
  main: { bg: '#fce7f3', text: '#9d174d' },
  sub: { bg: '#dbeafe', text: '#1e40af' },
  hidden: { bg: '#f3e8ff', text: '#6b21a8' },
  // 灵感
  dialogue: { bg: '#fef3c7', text: '#92400e' },
  scene: { bg: '#d1fae5', text: '#065f46' },
  detail: { bg: '#e0e7ff', text: '#3730a3' },
  reference: { bg: '#fce4ec', text: '#880e4f' },
  character_insp: { bg: '#fef9c3', text: '#854d0e' },
  plot_insp: { bg: '#dbeafe', text: '#1e40af' }
}

// ─── 灵感类型中文映射（兼容旧数据 inspiration/idea/material/reference） ───
export const INSP_TYPE_LABELS = {
  inspiration: '💡 灵感速记',
  idea: '💡 灵感速记',
  material: '📎 素材收集',
  reference: '📚 参考资料',
  character: '🎭 角色灵感',
  dialogue: '💬 对白灵感',
  scene: '🎬 场景描写',
  detail: '🔍 细节设定',
  character_insp: '👤 人物设定',
  plot_insp: '📖 情节构思'
}
