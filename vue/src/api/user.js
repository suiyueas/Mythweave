import { get, put, post, del } from './request'

export const userApi = {
  getProfile: () => get('/api/users/profile'),
  
  updateProfile: (data) => put('/api/users/profile', data),
  
  uploadAvatar: (formData) => post('/api/users/avatar', formData),
  
  deleteAvatar: () => del('/api/users/avatar'),
  
  changePassword: (data) => put('/api/users/password', data),
  
  sendEmailVerification: () => post('/api/users/email/verify'),
  
  getStats: () => get('/api/users/stats'),

  // ─── VIP 会员 ───
  getVipPlans: () => get('/api/users/vip/plans'),

  createVipOrder: (planId, payChannel) => post('/api/users/vip/order/create', { planId, payChannel }),

  getMyOrders: () => get('/api/users/vip/orders'),

  getOrderByNo: (orderNo) => get(`/api/users/vip/order/${orderNo}`),

  mockPayCallback: (orderNo) => post(`/api/users/vip/order/${orderNo}/pay`),

  activateVip: (planId) => post('/api/users/vip/activate', { planId })
}