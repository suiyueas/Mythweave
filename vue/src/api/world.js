import { get, post, put, del } from './request'

export const worldApi = {
  listSettings: (projectId) => get(`/api/projects/${projectId}/world/settings`),
  createSetting: (projectId, data) => post(`/api/projects/${projectId}/world/settings`, data),
  updateSetting: (projectId, id, data) => put(`/api/projects/${projectId}/world/settings/${id}`, data),
  deleteSetting: (projectId, id) => del(`/api/projects/${projectId}/world/settings/${id}`),
  listGlossary: (projectId) => get(`/api/projects/${projectId}/world/glossary`),
  createGlossary: (projectId, data) => post(`/api/projects/${projectId}/world/glossary`, data),
  deleteGlossary: (projectId, id) => del(`/api/projects/${projectId}/world/glossary/${id}`),
  listTimeline: (projectId) => get(`/api/projects/${projectId}/world/timeline`),
  createTimeline: (projectId, data) => post(`/api/projects/${projectId}/world/timeline`, data)
}
