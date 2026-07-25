import { get, post, put, del } from './request'

export const projectApi = {
  list: (pageNum = 1, pageSize = 10) => get('/api/projects', { pageNum, pageSize }),
  getById: (id) => get(`/api/projects/${id}`),
  create: (data) => post('/api/projects', data),
  update: (id, data) => put(`/api/projects/${id}`, data),
  delete: (id) => del(`/api/projects/${id}`),
  syncStats: () => post('/api/projects/_sync-stats')
}
