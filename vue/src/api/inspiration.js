import { get, post, put, del } from './request'

export const inspirationApi = {
  list: (projectId) => get(`/api/projects/${projectId}/inspirations`),
  create: (projectId, data) => post(`/api/projects/${projectId}/inspirations`, data),
  update: (projectId, id, data) => put(`/api/projects/${projectId}/inspirations/${id}`, data),
  delete: (projectId, id) => del(`/api/projects/${projectId}/inspirations/${id}`),
  aiGenerate: (projectId, keywords) => post(`/api/projects/${projectId}/inspirations/ai-generate`, { keywords })
}
