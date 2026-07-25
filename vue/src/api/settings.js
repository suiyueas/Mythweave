import { get, put } from './request'

export const settingsApi = {
  // 获取用户设置
  get: () => get('/api/users/settings'),
  
  // 更新用户设置
  update: (data) => put('/api/users/settings', data)
}