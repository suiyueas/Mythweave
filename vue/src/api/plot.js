import { get, post, put, del } from './request'

export const plotApi = {
  listThreads: (projectId) => get(`/api/projects/${projectId}/plot/threads`),
  createThread: (projectId, data) => post(`/api/projects/${projectId}/plot/threads`, data),
  updateThread: (projectId, id, data) => put(`/api/projects/${projectId}/plot/threads/${id}`, data),
  deleteThread: (projectId, id) => del(`/api/projects/${projectId}/plot/threads/${id}`),
  listForeshadowing: (projectId) => get(`/api/projects/${projectId}/plot/foreshadowing`),
  createForeshadowing: (projectId, data) => post(`/api/projects/${projectId}/plot/foreshadowing`, data),
  updateForeshadowing: (projectId, id, data) => put(`/api/projects/${projectId}/plot/foreshadowing/${id}`, data),
  deleteForeshadowing: (projectId, id) => del(`/api/projects/${projectId}/plot/foreshadowing/${id}`),
  listKG: (projectId) => get(`/api/projects/${projectId}/plot/kg`),
  createKG: (projectId, data) => post(`/api/projects/${projectId}/plot/kg`, data)
}
