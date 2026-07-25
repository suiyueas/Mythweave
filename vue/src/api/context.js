import { get, post, put, del } from './request'

export const contextApi = {
  /** 获取索引统计概览 */
  getStats: (projectId) =>
    get(`/api/projects/${projectId}/context/stats`),

  /** 获取检索配置 */
  getConfig: (projectId) =>
    get(`/api/projects/${projectId}/context/config`),

  /** 更新检索配置 */
  updateConfig: (projectId, data) =>
    put(`/api/projects/${projectId}/context/config`, data),

  /** 获取最近索引活动 */
  getActivities: (projectId, limit = 10) =>
    get(`/api/projects/${projectId}/context/activities`, { limit }),

  /** 语义搜索 */
  search: (projectId, query, topK = 10) =>
    get(`/api/projects/${projectId}/context/search`, { query, topK }),

  /** 装配续写上下文 */
  assemble: (projectId, data) =>
    post(`/api/projects/${projectId}/context/assemble`, data),

  /** 索引章节内容 */
  indexChapter: (projectId, chapterId, content) =>
    post(`/api/projects/${projectId}/context/index-chapter`, { chapterId, content }),

  /** 重建全部索引 */
  rebuildIndex: (projectId) =>
    post(`/api/projects/${projectId}/context/rebuild`),

  /** 增量索引（新内容） */
  incrementalIndex: (projectId) =>
    post(`/api/projects/${projectId}/context/incremental`),

  /** 清理无效索引数据 */
  cleanupIndex: (projectId) =>
    post(`/api/projects/${projectId}/context/cleanup`),

  /** 获取索引健康检查详情 */
  getHealthCheck: (projectId) =>
    get(`/api/projects/${projectId}/context/health`),

  /** 导出索引报告 */
  exportReport: (projectId, format = 'json') =>
    get(`/api/projects/${projectId}/context/export`, { format }),

  /** 获取索引大小趋势 */
  getSizeTrend: (projectId, days = 7) =>
    get(`/api/projects/${projectId}/context/size-trend`, { days }),

  /** 取消正在进行的索引操作 */
  cancelIndexOperation: (projectId, operationId) =>
    del(`/api/projects/${projectId}/context/operations/${operationId}`),

  /** 获取索引操作进度 */
  getOperationProgress: (projectId, operationId) =>
    get(`/api/projects/${projectId}/context/operations/${operationId}`)
}