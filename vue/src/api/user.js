import { get, put, post, del } from './request'

export const userApi = {
  getProfile: () => get('/api/users/profile'),
  
  updateProfile: (data) => put('/api/users/profile', data),
  
  uploadAvatar: (formData) => post('/api/users/avatar', formData),
  
  deleteAvatar: () => del('/api/users/avatar'),
  
  changePassword: (data) => put('/api/users/password', data),
  
  sendEmailVerification: () => post('/api/users/email/verify'),
  
  getStats: () => get('/api/users/stats')
}