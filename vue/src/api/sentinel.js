import { get, post, put, del } from './request'

export const sentinelApi = {
  // ─── 通知 ───
  /** 获取通知列表 */
  listNotifications: (projectId, status = 'all', limit = 20) =>
    get(`/api/projects/${projectId}/notifications`, { status, limit }),

  /** 标记单个通知已读 */
  markRead: (projectId, id) =>
    put(`/api/projects/${projectId}/notifications/${id}/read`),

  /** 全部标记已读 */
  markAllRead: (projectId) =>
    put(`/api/projects/${projectId}/notifications/read-all`),

  /** 删除通知 */
  deleteNotification: (projectId, id) =>
    del(`/api/projects/${projectId}/notifications/${id}`),

  // ─── 哨兵 ───
  /** 获取告警统计 */
  getStats: (projectId) =>
    get(`/api/projects/${projectId}/sentinel/stats`),

  /** 获取告警列表 */
  listAlerts: (projectId, type = 'all', status = 'all', limit = 20) =>
    get(`/api/projects/${projectId}/sentinel/alerts`, { type, status, limit }),

  /** 处理告警 */
  resolveAlert: (projectId, id) =>
    put(`/api/projects/${projectId}/sentinel/alerts/${id}/resolve`),

  /** 忽略告警 */
  ignoreAlert: (projectId, id) =>
    put(`/api/projects/${projectId}/sentinel/alerts/${id}/ignore`),

  /** 执行巡查 */
  scan: (projectId) =>
    post(`/api/projects/${projectId}/sentinel/scan`),

  /** 获取巡查进度 */
  getScanProgress: (projectId, taskId) =>
    get(`/api/projects/${projectId}/sentinel/scan/${taskId}/progress`),

  /** 获取巡查日志 */
  getLogs: (projectId, limit = 10) =>
    get(`/api/projects/${projectId}/sentinel/logs`, { limit }),

  /** 轻量级单章节检查（编辑器用） */
  checkChapterLightweight: (projectId, chapterId, content) =>
    get(`/api/projects/${projectId}/sentinel/check-chapter/${chapterId}`, { content })
}