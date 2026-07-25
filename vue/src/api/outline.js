import { get, post, put, del, request } from './request'

export const outlineApi = {
  list: (projectId) => get(`/api/projects/${projectId}/outline`),
  create: (projectId, data) => post(`/api/projects/${projectId}/outline`, data),
  update: (projectId, id, data) => put(`/api/projects/${projectId}/outline/${id}`, data),
  delete: (projectId, id) => del(`/api/projects/${projectId}/outline/${id}`),
  /** 批量更新排序（同幕拖拽） */
  batchSort: (projectId, items) => put(`/api/projects/${projectId}/outline/batch-sort`, items),
  /** 批量更新排序和幕归属（跨幕拖拽） */
  batchSortAct: (projectId, items) => put(`/api/projects/${projectId}/outline/batch-sort-act`, items),
  /** 批量更新状态 */
  batchStatus: (projectId, ids, status) => put(`/api/projects/${projectId}/outline/batch-status`, { ids, status }),
  /** 批量删除 */
  batchDelete: (projectId, ids) => request(`/api/projects/${projectId}/outline/batch`, { method: 'DELETE', body: { ids } })
}
