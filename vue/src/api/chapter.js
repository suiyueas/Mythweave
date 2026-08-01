import { get, post, put, del } from './request'

export const chapterApi = {
  listChapters: (projectId) => get(`/api/projects/${projectId}/chapters`),
  getChapter: (projectId, chapterId) => get(`/api/projects/${projectId}/chapters/${chapterId}`),
  createChapter: (projectId, data) => post(`/api/projects/${projectId}/chapters`, data),
  updateChapter: (projectId, data, silent) => {
    const url = `/api/projects/${projectId}/chapters/${data.id}` + (silent ? '?silent=true' : '')
    return put(url, data)
  },
  deleteChapter: (projectId, chapterId) => del(`/api/projects/${projectId}/chapters/${chapterId}`),
  reorderChapters: (projectId, orderedIds) => put(`/api/projects/${projectId}/chapters/reorder`, { orderedIds }),
  listVersions: (projectId, chapterId) => get(`/api/projects/${projectId}/chapters/${chapterId}/versions`),
  /** 获取单个版本详情（含正文，用于预览/恢复） */
  getVersionDetail: (projectId, chapterId, versionId) => get(`/api/projects/${projectId}/chapters/${chapterId}/versions/${versionId}`)
}
