/**
 * 通知服务 — 统一处理所有通知的发放控制
 * 
 * 根据 settings store 中的通知设置判断是否应该发送通知：
 * - 通知类型开关（sentinelAlert / agentComplete / writingGoal / versionSave）
 * - 提醒方式（站内通知 / 声音提醒）
 * - 严重度阈值过滤
 * - 免打扰时段
 */
import { useSettingsStore } from '@/stores/settings'

// 通知类型到 setting key 的映射
const NOTIF_TYPE_MAP = {
  sentinel: 'sentinelAlert',
  foreshadowing: 'sentinelAlert',
  logic: 'sentinelAlert',
  character: 'sentinelAlert',
  rhythm: 'sentinelAlert',
  agent: 'agentComplete',
  goal: 'writingGoal',
  writing_goal: 'writingGoal',
  version: 'versionSave',
  version_save: 'versionSave'
}

// 严重度级别映射（用于阈值比较）
const SEVERITY_LEVEL = {
  info: 0,
  warning: 1,
  critical: 2
}

const THRESHOLD_MAP = {
  all: 0,
  warning: 1,
  critical: 2
}

/**
 * 判断当前是否在免打扰时段内
 */
function isInQuietHours() {
  const settingsStore = useSettingsStore()
  if (!settingsStore.quietHoursEnabled) return false

  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()

  const [startHour, startMin] = (settingsStore.quietHoursStart || '22:00').split(':').map(Number)
  const [endHour, endMin] = (settingsStore.quietHoursEnd || '08:00').split(':').map(Number)
  const startMinutes = startHour * 60 + startMin
  const endMinutes = endHour * 60 + endMin

  if (startMinutes < endMinutes) {
    return currentMinutes >= startMinutes && currentMinutes < endMinutes
  } else {
    // 跨天，如 22:00 ~ 08:00
    return currentMinutes >= startMinutes || currentMinutes < endMinutes
  }
}

/**
 * 判断通知类型是否被用户开启
 */
function isNotificationTypeEnabled(notifType) {
  const settingsStore = useSettingsStore()
  const settingKey = NOTIF_TYPE_MAP[notifType]
  if (!settingKey) return true // 未知类型默认放行
  return settingsStore[settingKey] === true || settingsStore.settings[settingKey] === true
}

/**
 * 判断通知的严重度是否达到用户的阈值
 */
function meetsSeverityThreshold(severity) {
  const settingsStore = useSettingsStore()
  const threshold = settingsStore.severityThreshold || 'all'
  const minLevel = THRESHOLD_MAP[threshold] || 0
  const notifLevel = SEVERITY_LEVEL[severity] !== undefined ? SEVERITY_LEVEL[severity] : 0
  return notifLevel >= minLevel
}

/**
 * 核心方法：判断一个通知是否应当被显示
 */
export function shouldShowNotification(notif) {
  if (!notif) return false

  // 1. 检查通知类型是否开启
  if (!isNotificationTypeEnabled(notif.type)) return false

  // 2. 检查严重度阈值
  if (!meetsSeverityThreshold(notif.severity)) return false

  // 3. 检查免打扰时段
  if (isInQuietHours()) return false

  return true
}

/**
 * 过滤通知列表，只返回用户希望看到的通知
 */
export function filterNotifications(notifications) {
  if (!notifications || !Array.isArray(notifications)) return []
  return notifications.filter(shouldShowNotification)
}

/**
 * 播放通知提示音
 */
export function playNotificationSound() {
  const settingsStore = useSettingsStore()
  if (!settingsStore.soundAlert && !settingsStore.settings?.soundAlert) return

  try {
    const audio = new Audio('/notification.mp3')
    audio.volume = 0.3
    audio.play().catch(() => {
      // 浏览器可能阻止自动播放，静默忽略
    })
  } catch {
    // 静默失败
  }
}

/**
 * 发送通知（供业务模块调用）
 * @param {Object} options - 通知选项
 * @param {string} options.type - 通知类型 (sentinel/agent/goal/version)
 * @param {string} options.title - 通知标题
 * @param {string} options.description - 通知描述
 * @param {string} options.severity - 严重度 (info/warning/critical)
 * @param {Function} options.onSend - 实际发送通知的回调（如调用 API 或 store 方法）
 */
export function sendNotification({ type, title, description, severity = 'info', onSend }) {
  // 1. 检查通知类型是否开启
  if (!isNotificationTypeEnabled(type)) return

  // 2. 检查严重度阈值
  if (!meetsSeverityThreshold(severity)) return

  // 3. 检查免打扰时段
  if (isInQuietHours()) return

  // 4. 播放声音提醒
  playNotificationSound()

  // 5. 执行实际发送
  if (typeof onSend === 'function') {
    onSend()
  }
}
