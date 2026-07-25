/**
 * 公共格式化工具函数
 * 统一项目中的格式化逻辑，避免重复代码
 */

/**
 * 格式化数字（添加千位分隔符）
 * @param {number} n - 要格式化的数字
 * @returns {string} 格式化后的字符串
 */
export function formatNumber(n) {
  return (n || 0).toLocaleString()
}

/**
 * 格式化字数显示
 * - < 10,000: 直接显示 + "字" (如 "9,527字")
 * - ≥ 10,000: 保留一位小数 + "万字" (如 "1.2万字")
 * @param {number} count - 字数
 * @returns {string} 格式化后的字符串
 */
export function formatWordCount(count) {
  if (!count || count === 0) return '0字'
  if (count >= 10000) {
    return (count / 10000).toFixed(1).replace('.0', '') + '万字'
  }
  return count.toLocaleString() + '字'
}

/**
 * 格式化万字单位（用于目标字数显示）
 * - < 10,000: 直接显示数字 (如 "5,000")
 * - ≥ 10,000: 保留一位小数 + "万" (如 "20万")
 * @param {number} n - 数字
 * @returns {string} 格式化后的字符串
 */
export function formatWan(n) {
  if (!n) return '0万'
  if (n >= 10000) {
    return (n / 10000).toFixed(1).replace('.0', '') + '万'
  }
  return n.toLocaleString()
}

/**
 * 格式化时间显示
 * @param {string} timeStr - 时间字符串
 * @returns {string} 格式化后的字符串
 */
export function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

/**
 * 格式化相对时间（如"5分钟前"）
 * @param {string} timeStr - 时间字符串
 * @returns {string} 相对时间字符串
 */
export function formatRelativeTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = (now - date) / 1000

  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  return date.toLocaleDateString('zh-CN')
}

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化后的字符串
 */
export function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let index = 0
  let size = bytes

  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index++
  }

  return size.toFixed(index === 0 ? 0 : 1) + ' ' + units[index]
}

/**
 * 格式化活动时间
 * @param {string} timeStr - 时间字符串
 * @returns {string} 格式化后的字符串
 */
export function formatActivityTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = (now - date) / 1000

  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return date.toLocaleDateString('zh-CN')
}