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
  batchDelete: (projectId, ids) => request(`/api/projects/${projectId}/outline/batch`, { method: 'DELETE', body: { ids } }),
  /** 批量保存幕与节点（AI 生成的大纲整体保存，支持任意数量的幕） */
  saveActs: (projectId, acts) => post(`/api/projects/${projectId}/outline/acts`, acts),
  /** 修复幕区分布（旧数据重建幕节点与归属） */
  fixActDistribution: (projectId) => post(`/api/projects/${projectId}/outline/fix-act-distribution`)
}
