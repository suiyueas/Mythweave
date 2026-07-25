import { get, post } from './request'

export const exportApi = {
  /** 获取支持的导出格式列表 */
  formats: () => get('/api/projects/1/export/formats'),

  /** 发起导出请求 */
  exportProject: (projectId, params) =>
    post(`/api/projects/${projectId}/export`, params),

  /** 获取导出历史 */
  getHistory: (projectId) =>
    get(`/api/projects/${projectId}/export/history`),

  /** 导出为 TXT（兼容旧接口） */
  exportTxt: (projectId) =>
    get(`/api/projects/${projectId}/export/txt`)
}
