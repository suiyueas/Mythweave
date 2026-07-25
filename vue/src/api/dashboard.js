import { get } from './request'

export const dashboardApi = {
  /** 获取仪表盘统计概览 */
  getStats: (projectId) => get(`/api/projects/${projectId}/dashboard/stats`),

  /** 获取写作热力图数据 */
  getHeatmap: (projectId) => get(`/api/projects/${projectId}/dashboard/heatmap`),

  /** 获取最近活动 */
  getRecentActivities: (projectId, limit = 10) =>
    get(`/api/projects/${projectId}/dashboard/activities`, { limit }),

  /** 获取本周写作趋势 */
  getWeeklyTrend: (projectId) => get(`/api/projects/${projectId}/dashboard/weekly-trend`)
}
