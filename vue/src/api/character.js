import { get, post, put, del } from './request'

export const characterApi = {
  list: (projectId) => get(`/api/projects/${projectId}/characters`),
  create: (projectId, data) => post(`/api/projects/${projectId}/characters`, data),
  update: (projectId, id, data) => put(`/api/projects/${projectId}/characters/${id}`, data),
  delete: (projectId, id) => del(`/api/projects/${projectId}/characters/${id}`),
  listArcStates: (projectId, characterId) => get(`/api/projects/${projectId}/characters/arc-states/${characterId}`)
}
