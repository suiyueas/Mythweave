import { get, post, put, del } from './request'

export const authApi = {
  login: (data) => post('/api/auth/login', data),

  register: (data) => post('/api/auth/register', data)
}